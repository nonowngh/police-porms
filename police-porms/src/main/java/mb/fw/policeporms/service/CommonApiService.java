package mb.fw.policeporms.service;

import org.springframework.web.reactive.function.client.WebClient;

import mb.fw.policeporms.spec.InterfaceSpec;
import reactor.core.publisher.Mono;

public interface CommonApiService {
	String getApiName();

	Mono<?> fetch(InterfaceSpec spec, WebClient openApiWebClient);
	
}
