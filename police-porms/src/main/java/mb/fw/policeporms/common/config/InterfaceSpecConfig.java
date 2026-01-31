package mb.fw.policeporms.common.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.common.spec.InterfaceSpec;

@Profile("sender")
@Slf4j
@Configuration
public class InterfaceSpecConfig {

	private List<InterfaceSpec> specs;

	@Value("classpath:interface-specs.json")
	Resource jsonFile;

	@Bean
	List<InterfaceSpec> interfaceSpecs() throws Exception {
		if(!jsonFile.exists()) throw new Exception("not found json-file 'interface-specs.json'");
		ObjectMapper mapper = new ObjectMapper();
		specs = mapper.readValue(jsonFile.getInputStream(), new TypeReference<List<InterfaceSpec>>() {
		});
		logSpecs();
		return specs;
	}

	public void logSpecs() {
		if (specs != null && !specs.isEmpty()) {
			log.info("====== InterfaceSpec Config Loaded ======");
			specs.forEach(spec -> log.info("⚙ " + spec.toString()));
			log.info("========================================");
		} else {
			log.warn("InterfaceSpec Config is empty!");
		}
	}
}
