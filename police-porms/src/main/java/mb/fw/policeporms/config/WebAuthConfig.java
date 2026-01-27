package mb.fw.policeporms.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.Data;
import mb.fw.policeporms.constants.InterfaceApiPathConstants;
import mb.fw.policeporms.constants.InterfaceAuthConstants;
import mb.fw.policeporms.receiver.interceptor.InternalAuthInterceptor;

@Data
@Configuration
@ConfigurationProperties(prefix = "web.auth", ignoreUnknownFields = true)
public class WebAuthConfig implements WebMvcConfigurer{
	
	@Autowired
	private InternalAuthInterceptor authInterceptor;
	
	private String interfaceApiKey = "porms-2026-secret-key";

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(authInterceptor).addPathPatterns(InterfaceApiPathConstants.INTERFACE_PATH + "/**") // 수신 API 경로에만 적용
				.excludePathPatterns("/public/**"); // 예외 경로가 있다면 추가
	}
	
	@PostConstruct
	public void init() {
		InterfaceAuthConstants.AUTH_KEY = interfaceApiKey;
	}
}
