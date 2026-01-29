package mb.fw.policeporms.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "interface.file", ignoreUnknownFields = true)
public class FileTransferConfig {

	private String tempDirectory;

}
