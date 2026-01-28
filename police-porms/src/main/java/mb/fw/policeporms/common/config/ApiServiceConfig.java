package mb.fw.policeporms.common.config;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import mb.fw.policeporms.common.constant.ApiType;
import mb.fw.policeporms.domain.sender.service.base.ApiService;

@Profile("sender")
@Configuration
public class ApiServiceConfig {

	@Bean
	Map<ApiType, ApiService> apiServiceMap(List<ApiService> services) {
		return services.stream().collect(Collectors.toMap(ApiService::getApiType, service -> service));
	}
}
