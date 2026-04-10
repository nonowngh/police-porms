package mb.fw.policeporms.domain.receiver.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.common.annotation.ReceiverService;
import mb.fw.policeporms.common.config.MyBatisConfig;
import mb.fw.policeporms.common.constant.InterfaceStatus;
import mb.fw.policeporms.common.constant.MybatisConstants;
import mb.fw.policeporms.common.dto.RequestMessage;
import mb.fw.policeporms.common.dto.ResponseMessage;
import mb.fw.policeporms.common.utils.GzipUtils;
import mb.fw.policeporms.common.utils.InterfaceEncryptUtils;
import mb.fw.policeporms.common.utils.LoggingUtils;

@Slf4j
@ReceiverService
@RequiredArgsConstructor
public class InterfaceProcessService {

	private final MyBatisConfig mybatisConfig;
	private final SqlSessionTemplate sqlSessionTemplate;
//    private final @Qualifier("batchSqlSessionTemplate") SqlSessionTemplate batchSqlSessionTemplate;

	private final ObjectMapper objectMapper;

	@Transactional(rollbackFor = Exception.class)
	public ResponseMessage fileProcess(RequestMessage request, MultipartFile file) {
		String interfaceId = request.getInterfaceId();
		String transactionId = request.getTransactionId();
		String insertSqlId = interfaceId + "." + MybatisConstants.SqlId.INSERT;
		String deleteSqlId = interfaceId + "." + MybatisConstants.SqlId.DELETE;

		ResponseMessage response = new ResponseMessage();
		response.setInterfaceId(interfaceId);
		response.setTransactionId(transactionId);
		response.setResultCount(request.getSendDataCount());

		if (!GzipUtils.isGzipFileValid(file)) {
			response.setProcessCd(InterfaceStatus.ERROR);
			response.setProcessMsg("전송된 파일이 손상되었거나 유효하지 않습니다.");
//			response.setResultCount(0);
			return response;
		}

		String fileName = file.getOriginalFilename();
		boolean isEncrypt = fileName != null && fileName.toLowerCase().endsWith(".enc");

		try {
			sqlSessionTemplate.delete(deleteSqlId);
			log.info("[{}] 기존 데이터 삭제 완료", transactionId);

			try (InputStream is = file.getInputStream()) {
				// 디스크에서 읽어오는 속도를 대폭 향상 (64KB 버퍼)
				InputStream finalIn = new BufferedInputStream(is, 65536);

				if (isEncrypt) {
					finalIn = InterfaceEncryptUtils.createFileDecryptInputStream(finalIn);
					log.debug("[{}] 암호화 파일 복호화 스트림 연결 완료", transactionId);
					// 복호화된 데이터를 GZIP에 넘기기 전 병목 제거 (64KB 버퍼)
					finalIn = new BufferedInputStream(finalIn, 65536);
				}

				// GZIP 및 문자열 Reader 버퍼 사이즈도 64KB로 확장, GC 스래싱 및 메모리 부하 방지
				try (GZIPInputStream gzis = new GZIPInputStream(finalIn, 65536);
					 BufferedReader reader = new BufferedReader(new InputStreamReader(gzis, StandardCharsets.UTF_8), 65536)) {
					int currentCount = processInsert(reader, insertSqlId, transactionId, request.getSendDataCount());

					log.info("[{}] 최종 적재 완료: 총 {}건", transactionId, String.format("%,d", currentCount));
					response.setProcessCd(InterfaceStatus.SUCCESS);
					response.setProcessMsg("처리완료");
					response.setResultCount(currentCount);
				}
			}
		} catch (Exception e) {
			log.error("[{}] 수신 처리 중 치명적 오류 발생", transactionId, e);
			// 트랜잭션 롤백 강제
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			response.setProcessCd(InterfaceStatus.ERROR);
			response.setProcessMsg("DB 적재 실패 : " + e.getMessage());
//				response.setResultCount(0);
		}

		return response;
	}

//			try (InputStream is = file.getInputStream();
//					GZIPInputStream gzis = new GZIPInputStream(is);
//					BufferedReader reader = new BufferedReader(new InputStreamReader(gzis, StandardCharsets.UTF_8))) {
//
//				String line;
//				int currentCount = 0;
//				List<Map<String, Object>> chunkList = new ArrayList<>();
//				int chunkSize = mybatisConfig.getChunkSize(); // 예: 1000
//				Map<String, Object> params = new HashMap<>();
//
//				while ((line = reader.readLine()) != null) {
//					Map<String, Object> row = objectMapper.readValue(line, new TypeReference<Map<String, Object>>() {
//					});
//					chunkList.add(row);
//					currentCount++;
//					if (chunkList.size() >= chunkSize) {
//						params.put(MybatisConstants.Param.LIST, chunkList);
//						sqlSessionTemplate.insert(insertSqlId, params);
//						chunkList.clear();
//						LoggingUtils.printInsertProgress(transactionId, totalCount, currentCount);
//					}
//				}
//				// 마지막 잔여 데이터 처리
//				if (!chunkList.isEmpty()) {
//					params.put(MybatisConstants.Param.LIST, chunkList);
//					sqlSessionTemplate.insert(insertSqlId, params);
//				}
//				log.info("[{}] 최종 적재 완료: 총 {}건", transactionId, String.format("%,d", currentCount));
//				response.setProcessCd(InterfaceStatus.SUCCESS);
//				response.setProcessMsg("처리완료");
//				response.setResultCount(currentCount);
//			}
//		} catch (Exception e) {
//			log.error("[{}] 수신 처리 중 치명적 오류 발생", transactionId, e);
//			// 트랜잭션 롤백 강제
//			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//			response.setProcessCd(InterfaceStatus.ERROR);
//			response.setProcessMsg("DB 적재 실패 : " + e.getMessage());
////			response.setResultCount(0);
//		}
	
	/**
	 * 데이터를 읽어 Chunk 단위로 DB에 Insert 합니다. (트랜잭션 안전 보장형 병렬 파이프라인)
	 * 📌 다른 인터페이스에 영향이 없도록 트랜잭션은 메인 쓰레드가 유지하고 파싱만 분리합니다.
	 */
	private int processInsert(BufferedReader reader, String insertSqlId, String transactionId, int totalCount)
			throws IOException {
		
		int chunkSize = mybatisConfig.getChunkSize(); 
		BlockingQueue<List<Map<String, Object>>> queue = new ArrayBlockingQueue<>(10);
		AtomicInteger totalInserted = new AtomicInteger(0);
		AtomicBoolean hasError = new AtomicBoolean(false);
		AtomicReference<Exception> producerException = new AtomicReference<>();

		// -------------------------------------------------------------------
		// 🧑‍🏭 1. 생산자(Producer) 백그라운드 쓰레드 : 파일 읽기 및 JSON 파싱 전담
		// -------------------------------------------------------------------
		Thread jsonParserThread = new Thread(() -> {
			try {
				String line;
				List<Map<String, Object>> currentChunk = new ArrayList<>(chunkSize);
				
				while ((line = reader.readLine()) != null) {
					// 메인 쓰레드(DB) 쪽에서 에러가 났다면 파싱 즉시 중단
					if (hasError.get()) break; 

					Map<String, Object> row = objectMapper.readValue(line, new TypeReference<Map<String, Object>>() {});
					currentChunk.add(row);

					if (currentChunk.size() >= chunkSize) {
						queue.put(currentChunk);
						currentChunk = new ArrayList<>(chunkSize);
					}
				}
				// 잔여 데이터 큐에 밀어넣기
				if (!currentChunk.isEmpty() && !hasError.get()) {
					queue.put(currentChunk);
				}
			} catch (Exception e) {
				log.error("[{}] JSON 파싱 중 백그라운드 오류 발생", transactionId, e);
				producerException.set(e);
			} finally {
				try {
					// 파싱이 끝났거나 에러가 났으면 소비자(메인 쓰레드)에게 작업 종료 신호(빈 리스트) 전송
					queue.put(Collections.emptyList()); 
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});
		jsonParserThread.start();

		// -------------------------------------------------------------------
		// 🧑‍🏭 2. 소비자(Consumer) 메인 쓰레드 : @Transactional 유지하며 DB Insert 전담
		// -------------------------------------------------------------------
		try {
			while (true) {
				// 백그라운드 쓰레드가 파싱해둔 데이터를 꺼냄 (없으면 대기)
				List<Map<String, Object>> chunkList = queue.take();
				
				// 빈 리스트(Poison Pill)를 받으면 정상 종료 또는 에러 확인
				if (chunkList.isEmpty()) {
					if (producerException.get() != null) {
						throw new IOException("파일 파싱 중 치명적 오류 발생", producerException.get());
					}
					break; 
				}

				// 📌 기존과 동일한 Map<String, Object> 구조 조립 (다른 XML에 100% 호환)
				Map<String, Object> params = new HashMap<>();
				params.put(MybatisConstants.Param.LIST, chunkList);
				
				// 📌 메인 쓰레드이므로 Spring @Transactional이 완벽하게 작동함!
				sqlSessionTemplate.insert(insertSqlId, params);

				int inserted = totalInserted.addAndGet(chunkList.size());
				LoggingUtils.printInsertProgress(transactionId, totalCount, inserted);
			}
		} catch (Exception e) {
			hasError.set(true); // 파싱 쓰레드에게 중단하라고 플래그 전송
			jsonParserThread.interrupt(); // 혹시 큐에 블로킹되어 있다면 인터럽트로 깨움
			throw new IOException("DB 적재 과정에서 치명적 오류가 발생했습니다.", e);
		}

		return totalInserted.get();
	}

//	/**
//	 * 데이터를 읽어 Chunk 단위로 DB에 Insert 합니다.
//	 */
//	private int processInsert(BufferedReader reader, String insertSqlId, String transactionId, int totalCount)
//			throws IOException {
//		String line;
//		int currentCount = 0;
//		int chunkSize = mybatisConfig.getChunkSize();
//		List<Map<String, Object>> chunkList = new ArrayList<>();
//		Map<String, Object> params = new HashMap<>();
//
//		while ((line = reader.readLine()) != null) {
//			Map<String, Object> row = objectMapper.readValue(line, new TypeReference<Map<String, Object>>() {
//			});
//			chunkList.add(row);
//			currentCount++;
//
//			if (chunkList.size() >= chunkSize) {
//				params.put(MybatisConstants.Param.LIST, chunkList);
//				sqlSessionTemplate.insert(insertSqlId, params);
//				chunkList.clear();
//				LoggingUtils.printInsertProgress(transactionId, totalCount, currentCount);
//			}
//		}
//
//		// 마지막 잔여 데이터 처리
//		if (!chunkList.isEmpty()) {
//			params.put(MybatisConstants.Param.LIST, chunkList);
//			sqlSessionTemplate.insert(insertSqlId, params);
//		}
//		return currentCount;
//	}

	

//	private void insertRow(MultipartFile file, String insertSqlId, String deleteSqlId, String transactionId,
//			ResponseMessage response, int totalCount)
//			throws IOException, JsonProcessingException, JsonMappingException {
//		try (SqlSession batchSession = sqlSessionTemplate.getSqlSessionFactory().openSession(ExecutorType.BATCH,
//				false)) {
//			sqlSessionTemplate.delete(deleteSqlId);
//			log.info("[{}] 기존 데이터 삭제 완료", transactionId);
//			try (InputStream is = file.getInputStream();
//					GZIPInputStream gzis = new GZIPInputStream(is);
//					BufferedReader reader = new BufferedReader(new InputStreamReader(gzis, StandardCharsets.UTF_8))) {
//				String line;
//				int currentCount = 0;
//				while ((line = reader.readLine()) != null) {
//					Map<String, Object> row = objectMapper.readValue(line, new TypeReference<Map<String, Object>>() {
//					});
//
//					batchSession.insert(insertSqlId, row);
//					currentCount++;
//					if (currentCount % mybatisConfig.getBatchSize() == 0) {
//						batchSession.flushStatements();
//						printProgress(transactionId, totalCount, currentCount);
//					}
//				}
//				// 마지막 잔여 데이터 전송
//				batchSession.flushStatements();
//				
//				log.info("[{}] 최종 적재 완료: 총 {}건", transactionId, String.format("%,d", currentCount));
//				response.setProcessCd(InterfaceStatus.SUCCESS);
//				response.setProcessMsg("처리완료");
//				response.setResultCount(currentCount);
//			}
//		}
//	}

//	private void printProgress(String txId, int total, int current) {
//		double progress = (total > 0) ? ((double) current / total) * 100 : 0;
//		log.info("[{}] ⏳ 진행 상황: {}/{}건 적재 중 ({})", txId, String.format("%,d", current), String.format("%,d", total),
//				String.format("%.1f%%", progress));
//	}

}
