package mb.fw.policeporms.service;

import java.util.HashMap;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mb.fw.policeporms.constants.MybatisConstants;
import mb.fw.policeporms.dto.RequestMessage;
import mb.fw.policeporms.dto.ResponseMessage;

@Service
public class InterfaceProcessService {

	private final SqlSessionTemplate sqlSessionTemplate;

	public InterfaceProcessService(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

	@Transactional
	public ResponseMessage process(RequestMessage request) {
		String sqlId = request.getInterfaceId() + MybatisConstants.SQL_ID_INSERT;
		try {
			Map<String, Object> param = new HashMap<>();
			param.put(MybatisConstants.PARAM_LIST, request.getDataList());
			sqlSessionTemplate.insert(sqlId, param);
			return ResponseMessage.builder().interfaceId(request.getInterfaceId())
					.transactionId(request.getTransactionId()).processCd("S").processMsg("SUCCESS").build();
		} catch (Exception e) {
			return ResponseMessage.builder().interfaceId(request.getInterfaceId())
					.transactionId(request.getTransactionId()).processCd("E").processMsg(e.getMessage()).build();
		}
	}
}
