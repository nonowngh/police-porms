package mb.fw.policeporms.domain.sender.service.base;

import java.nio.file.Path;

import mb.fw.policeporms.common.constant.ApiType;
import mb.fw.policeporms.common.spec.InterfaceSpec;

public interface ApiService {
	
	ApiType getApiType();

	int fetchAndSave(InterfaceSpec spec, Path tempFile);
	
}
