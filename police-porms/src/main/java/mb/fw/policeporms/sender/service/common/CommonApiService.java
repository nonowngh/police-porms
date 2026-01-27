package mb.fw.policeporms.sender.service.common;

import java.nio.file.Path;

import mb.fw.policeporms.spec.InterfaceSpec;

public interface CommonApiService {
	
	String getApiType();

	int fetchAndSave(InterfaceSpec spec, Path tempFile);
	
}
