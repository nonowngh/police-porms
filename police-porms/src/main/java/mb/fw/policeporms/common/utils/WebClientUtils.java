package mb.fw.policeporms.common.utils;

import org.springframework.web.util.UriComponentsBuilder;

import mb.fw.policeporms.common.spec.InterfaceSpec;

public class WebClientUtils {

	public static UriComponentsBuilder appendQueryParams(InterfaceSpec spec, String apiPath) {
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(spec.getApiUrl()).path(apiPath);
		if (spec.getAdditionalParams() != null) {
			spec.getAdditionalParams().forEach((key, value) -> {
				if (value != null) {
					uriBuilder.queryParam(key, value);
				}
			});
		}
		return uriBuilder;
	}
}
