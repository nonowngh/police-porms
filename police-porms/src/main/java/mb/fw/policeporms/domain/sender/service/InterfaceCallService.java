package mb.fw.policeporms.domain.sender.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

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
		
	    java.util.concurrent.atomic.AtomicBoolean isCallbackExecuted = new java.util.concurrent.atomic.AtomicBoolean(false);
	    Consumer<Integer> safeCallback = (count) -> {
	        if (countCallback != null && isCallbackExecuted.compareAndSet(false, true)) {
	            countCallback.accept(count);
	        }
	    };
	    
		// 파일 경로 설정
		String fileName = "temp_" + transactionId + ".jsonl.gz";
		Path sendFile = Paths.get(fileTransferConfig.getTempDirectory(), fileName);
		try {
			int totalCount = callApi(spec, sendFile, transactionId);

			safeCallback.accept(totalCount);

			if (totalCount == 0) {
				response.setProcessCd(InterfaceStatus.ERROR);
				response.setProcessMsg("전송할 데이터(API 응답건수 0)가 없습니다.");
				return response;
			}
			response.setResultCount(totalCount);

			RequestMessage request = RequestMessage.builder().interfaceId(interfaceId).transactionId(transactionId)
					.sendDataCount(totalCount).sendFileName(fileName).sendFileSize(Files.size(sendFile)).build();

			ResponseMessage serverResponse = sendFile(request, sendFile.toFile()).block();
			if (serverResponse != null) {
				response.setProcessCd(serverResponse.getProcessCd());
				response.setProcessMsg(serverResponse.getProcessMsg());
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
		try {
			sendTotalCount = service.fetchAndSave(spec, tempFile, transactionId);
			if (sendTotalCount != 0)
				log.info("[{}] '{}' 파일 생성 완료. 총 건수: {}, 파일크기: {}bytes 수신 서버로 전송 시작...", transactionId,
						tempFile.toAbsolutePath(), sendTotalCount, Files.size(tempFile));
		} catch (Exception e) {
			throw e;
		}
		return sendTotalCount;
	}

	// 생성한 파일 수신 ESB 프로세스로 전송
	private Mono<ResponseMessage> sendFile(RequestMessage request, File file) {
		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		// 1. RequestMessage 객체 자체를 "message"라는 이름의 JSON 파트로 추가
		builder.part("message", request, MediaType.APPLICATION_JSON);
		// 2. 실제 파일 추가
		builder.part("file", new FileSystemResource(file));
		return this.interfaceWebClient.post().uri(InterfaceApiPathConstants.RECEIVE_FILE_PATH)
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
