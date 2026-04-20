package mb.fw.policeporms.domain.sender.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.common.annotation.SenderService;
import mb.fw.policeporms.common.config.FileTransferConfig;
import mb.fw.policeporms.common.constant.ApiType;
import mb.fw.policeporms.common.constant.InterfaceApiPathConstants;
import mb.fw.policeporms.common.constant.InterfaceStatus;
import mb.fw.policeporms.common.dto.RequestMessage;
import mb.fw.policeporms.common.dto.ResponseMessage;
import mb.fw.policeporms.common.spec.InterfaceSpec;
import mb.fw.policeporms.domain.sender.service.base.ApiService;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Slf4j
@SenderService
public class InterfaceCallService {

	private final WebClient interfaceWebClient;
	private final Map<ApiType, ApiService> apiServiceMap;
	private final FileTransferConfig fileTransferConfig;

	public InterfaceCallService(@Qualifier("interfaceWebClient") WebClient interfaceWebClient,
			Map<ApiType, ApiService> apiServiceMap, FileTransferConfig fileTransferConfig) {
		this.interfaceWebClient = interfaceWebClient;
		this.apiServiceMap = apiServiceMap;
		this.fileTransferConfig = fileTransferConfig;
	}

	public ResponseMessage executeApiDataSend(InterfaceSpec spec, String transactionId,
			Consumer<Integer> countCallback) {
		String interfaceId = spec.getInterfaceId();
		ResponseMessage response = new ResponseMessage();
		response.setInterfaceId(interfaceId);
		response.setTransactionId(transactionId);

		java.util.concurrent.atomic.AtomicBoolean isCallbackExecuted = new java.util.concurrent.atomic.AtomicBoolean(
				false);
		Consumer<Integer> safeCallback = (count) -> {
			if (countCallback != null && isCallbackExecuted.compareAndSet(false, true)) {
				countCallback.accept(count);
			}
		};

		// JSON 설정(additionalParams)에서 커스텀 타임아웃 값 추출
		int customTimeoutMinutes = 0;
		if (spec.getAdditionalParams() != null && spec.getAdditionalParams().containsKey("customTimeoutMinutes")) {
			try {
				customTimeoutMinutes = Integer.parseInt(String.valueOf(spec.getAdditionalParams().get("customTimeoutMinutes")));
			} catch (NumberFormatException e) {
				log.warn("[{}] customTimeoutMinutes 파라미터 파싱 오류. 기본 타임아웃을 사용합니다.", interfaceId);
			}
		}

		// 파일 경로 설정
		String fileName = "temp_" + transactionId + ".jsonl.gz";
		if (spec.isDataEncrypt())
			fileName += ".enc";
		Path sendFile = Paths.get(fileTransferConfig.getTempDirectory(), fileName);
		try {
			int totalCount = callApi(spec, sendFile, transactionId);

			safeCallback.accept(totalCount);

			if (totalCount == 0) {
				// 파라미터 중 List(배열)가 있는지 확인하여 다중 호출 여부 판별
				boolean isMultiCall = false;
				if (spec.getAdditionalParams() != null) {
					for (Object value : spec.getAdditionalParams().values()) {
						if (value instanceof java.util.List) {
							isMultiCall = true;
							break;
						}
					}
				}

				if (isMultiCall) {
					// 다중 호출(배열)일 경우 모든 배열이 0건 이어도 스킵 및 성공 처리
					response.setProcessCd(InterfaceStatus.SUCCESS);
					response.setResultCount(0);
					response.setProcessMsg("배열 데이터 0건 리턴으로 파일 전송을 생략합니다.");
				} else {
					// 단일 호출일 경우 0건 '에러' 처리
					response.setProcessCd(InterfaceStatus.ERROR);
					response.setProcessMsg("전송할 데이터(API 응답건수 0)가 없습니다.");
				}
				return response;
			}

			response.setResultCount(totalCount);
			
			String processType = "DB";
			if (spec.getReceiverProcessType() != null) {
			    processType = spec.getReceiverProcessType();
			} else if (spec.getAdditionalParams() != null && spec.getAdditionalParams().containsKey("receiverProcessType")) {
			    processType = String.valueOf(spec.getAdditionalParams().get("receiverProcessType"));
			}
			
			String originalFileName = null;
			if (spec.getAdditionalParams() != null && spec.getAdditionalParams().containsKey("ORIGINAL_FILE_NAME")) {
			    originalFileName = String.valueOf(spec.getAdditionalParams().get("ORIGINAL_FILE_NAME"));
			}

			// RequestMessage 생성 시 추출한 processType 주입
			RequestMessage request = RequestMessage.builder().interfaceId(interfaceId).transactionId(transactionId)
			        .sendDataCount(totalCount).sendFileName(originalFileName != null ? originalFileName : fileName)
			        .sendFileSize(Files.size(sendFile))
			        .receiverProcessType(processType).build();

			// sendFile 메서드에 customTimeoutMinutes 파라미터 전달
			ResponseMessage serverResponse = sendFile(request, sendFile.toFile(), customTimeoutMinutes).block();
			if (serverResponse != null) {
				response.setProcessCd(serverResponse.getProcessCd());
				response.setProcessMsg(serverResponse.getProcessMsg());
				response.setResultCount(serverResponse.getResultCount());
			}

		} catch (Exception e) {
			log.error("[{}] executeApiDataSend 처리 중 오류 발생 : {}", transactionId, e.getMessage());
			// 콜백이 아직 실행되지 않았다면 1으로 호출하여 로그 누락 방지
			safeCallback.accept(1);
			response.setProcessCd(InterfaceStatus.ERROR);
			response.setProcessMsg(e.getMessage());

		} finally {
			try {
				if (Files.deleteIfExists(sendFile)) {
					log.debug("[{}] 임시 파일 삭제 완료: {}", transactionId, sendFile.getFileName());
				}
			} catch (IOException e) {
				log.error("[{}] 임시 파일 삭제 실패: {}", transactionId, e.getMessage());
			}
		}
		return response;
	}

	// open-api 호출 후 응답 데이터 파일로 생성
	private int callApi(InterfaceSpec spec, Path tempFile, String transactionId) throws IOException {
		int sendTotalCount = 0;
		ApiService service = apiServiceMap.get(spec.getApiType());
		if (service == null) {
			throw new RuntimeException("No service found for " + spec.getApiType());
		}
		long startTime = System.currentTimeMillis();
		try {
			sendTotalCount = service.fetchAndSave(spec, tempFile, transactionId);
			String durationSeconds = String.format("%.1f", (System.currentTimeMillis() - startTime) / 1000.0);

			if (sendTotalCount != 0)
				log.info("[{}] '{}' 파일 생성 완료.(소요시간 : {}s) 총 건수: {}, 파일크기: {}bytes 수신 서버로 전송 시작...", transactionId,
						tempFile.toAbsolutePath(), durationSeconds, sendTotalCount, Files.size(tempFile));
		} catch (Exception e) {
			throw e;
		}
		return sendTotalCount;
	}

	// 📌 생성한 파일 수신 ESB 프로세스로 전송 (타임아웃 동적 제어 추가)
	private Mono<ResponseMessage> sendFile(RequestMessage request, File file, int customTimeoutMinutes) {
		
		WebClient currentWebClient = this.interfaceWebClient;

		// 📌 커스텀 타임아웃이 설정되어 있다면 (예: 30분), 일회용 WebClient로 복제(mutate)
		if (customTimeoutMinutes > 0) {
			log.info("[{}] ⏳ 커스텀 타임아웃 감지: 해당 요청의 통신 대기 시간을 {}분으로 연장합니다.", request.getTransactionId(), customTimeoutMinutes);
			
			HttpClient httpClient = HttpClient.create()
				.responseTimeout(Duration.ofMinutes(customTimeoutMinutes)) // 전체 응답 대기 시간
				.doOnConnected(conn -> conn
					.addHandlerLast(new ReadTimeoutHandler(customTimeoutMinutes, TimeUnit.MINUTES))
					.addHandlerLast(new WriteTimeoutHandler(customTimeoutMinutes, TimeUnit.MINUTES))
				);

			// 기존 설정(토큰, 헤더 등)은 100% 유지한 채 커넥터만 교체
			currentWebClient = this.interfaceWebClient.mutate()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
		}

		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		// RequestMessage 객체 자체를 "message"라는 이름의 JSON 파트로 추가
		builder.part("message", request, MediaType.APPLICATION_JSON);
		// 실제 파일 추가
		builder.part("file", new FileSystemResource(file));
		
		return currentWebClient.post().uri(InterfaceApiPathConstants.RECEIVE_FILE_PATH) // 복제된 WebClient 사용
				.contentType(MediaType.MULTIPART_FORM_DATA).body(BodyInserters.fromMultipartData(builder.build()))
				.retrieve().onStatus(status -> status.isError(), clientResponse -> {
					return clientResponse.bodyToMono(ResponseMessage.class) // 에러 바디를 ResponseMessage로 파싱
							.flatMap(errorRes -> {
								String msg = (errorRes != null && errorRes.getProcessMsg() != null)
										? errorRes.getProcessMsg()
										: "알 수 없는 서버 오류";
//								log.error("[{}] 수신 서버 에러 응답 수신: {}", request.getTransactionId(), msg);
								return Mono.error(new RuntimeException("[" + msg + "]"));
							});
				}).bodyToMono(ResponseMessage.class)
				.doOnSuccess(res -> log.debug("[{}] 파일 전송 성공 : {}", request.getTransactionId(), res))
				.onErrorResume(e -> {
//					log.error("[{}] 전송 오류: {}", request.getTransactionId(), e.getMessage());
					return Mono.error(new RuntimeException("파일 전송 오류 : " + e.getMessage()));
				});
	}

}