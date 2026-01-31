package mb.fw.policeporms.domain.receiver.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
		String insertSqlId = interfaceId + "." + MybatisConstants.SqlId.INSERT;
		String deleteSqlId = interfaceId + "." + MybatisConstants.SqlId.DELETE;
		
		String transactionId = request.getTransactionId();
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
		int totalCount = request.getSendDataCount();
		try {
			sqlSessionTemplate.delete(deleteSqlId);
			log.info("[{}] 기존 데이터 삭제 완료", transactionId);

			try (InputStream is = file.getInputStream();
					GZIPInputStream gzis = new GZIPInputStream(is);
					BufferedReader reader = new BufferedReader(new InputStreamReader(gzis, StandardCharsets.UTF_8))) {

				String line;
				int currentCount = 0;
				List<Map<String, Object>> chunkList = new ArrayList<>();
				int chunkSize = mybatisConfig.getChunkSize(); // 예: 1000
				Map<String, Object> params = new HashMap<>();

				while ((line = reader.readLine()) != null) {
					Map<String, Object> row = objectMapper.readValue(line, new TypeReference<Map<String, Object>>() {
					});
					chunkList.add(row);
					currentCount++;
					if (chunkList.size() >= chunkSize) {
						params.put(MybatisConstants.Param.LIST, chunkList);
						sqlSessionTemplate.insert(insertSqlId, params);
						chunkList.clear();
						LoggingUtils.printInsertProgress(transactionId, totalCount, currentCount);
					}
				}
				// 마지막 잔여 데이터 처리
				if (!chunkList.isEmpty()) {
					params.put(MybatisConstants.Param.LIST, chunkList);
					sqlSessionTemplate.insert(insertSqlId, params);
				}
				log.info("[{}] 최종 적재 완료: 총 {}건", transactionId, String.format("%,d", currentCount));
				response.setProcessCd(InterfaceStatus.SUCCESS);
				response.setProcessMsg("처리완료");
				response.setResultCount(currentCount);
			}
		} catch (Exception e) {
			log.error("[{}] 수신 처리 중 치명적 오류 발생", transactionId, e);
			// 트랜잭션 롤백 강제
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			response.setProcessCd(InterfaceStatus.ERROR);
			response.setProcessMsg("DB 적재 실패 : " + e.getMessage());
//			response.setResultCount(0);
		}

		return response;
	}

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
