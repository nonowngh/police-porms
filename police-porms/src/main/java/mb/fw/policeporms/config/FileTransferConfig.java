package mb.fw.policeporms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "file.transfer", ignoreUnknownFields = true)
public class FileTransferConfig {

	private String tempDirectory;

}
