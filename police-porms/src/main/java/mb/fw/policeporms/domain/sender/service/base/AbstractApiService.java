package mb.fw.policeporms.domain.sender.service.base;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public abstract class AbstractApiService implements ApiService {

	protected final ObjectMapper objectMapper;
	protected final WebClient openApiWebClient;
	protected final XmlMapper xmlMapper;

	protected AbstractApiService(ObjectMapper objectMapper, @Qualifier("openApiWebClient") WebClient openApiWebClient) {
		this.objectMapper = objectMapper;
		this.openApiWebClient = openApiWebClient;
		this.xmlMapper = (XmlMapper) new XmlMapper()
				.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * 이미 열려있는 BufferedWriter를 통해 데이터를 JSONL로 기록합니다.
	 */
	protected void writeRowsToWriter(BufferedWriter writer, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty())
			return;
		try {
			for (Map<String, Object> row : rows) {
				writer.write(this.objectMapper.writeValueAsString(row));
				writer.newLine();
			}
			// 배치 단위로 flush를 호출하면 메모리 관리와 성능 사이의 균형을 맞출 수 있습니다.
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException("데이터 기록 중 입출력 오류 발생", e);
		}
	}
}
