package mb.fw.policeporms.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.Data;

@Data
@Profile("receiver")
@Configuration
@ConfigurationProperties(prefix = "mybatis", ignoreUnknownFields = true)
public class MyBatisConfig {

	private int chunkSize = 1000;
//	@Bean(name = "batchSqlSessionTemplate")
//    SqlSessionTemplate batchSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
//        return new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
//    }
//	
//	@Bean(name = "simpleSqlSessionTemplate")
//    SqlSessionTemplate simpleSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
//        return new SqlSessionTemplate(sqlSessionFactory, ExecutorType.SIMPLE);
//    }
}
