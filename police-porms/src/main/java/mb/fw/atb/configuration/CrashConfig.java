package mb.fw.atb.configuration;

import com.mb.indigo2.springsupport.AdaptorConfigBean;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.util.ESBProductEncryption;
import mb.fw.net.common.encryption.ProductEncryption;
import org.crsh.CrashLogin;
import org.crsh.shell.impl.command.CRaSHShellFactory;
import org.crsh.spring.SpringBootstrap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.devtools.restart.classloader.RestartClassLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Properties;

@Slf4j
@Configuration("crashConfig")
public class CrashConfig {

    @Autowired
    AdaptorConfigBean bean;

    @Autowired
    Environment environment;

    @Getter
    String password;

    @Getter
    String username;

    @Bean(name = "crashSpringBootstrap")
    public SpringBootstrap SpringBootstrap() throws Exception {

        Properties properties = PropertiesLoaderUtils.loadAllProperties(bean.getAdaptorName() + ".properties");

        String port = properties.getProperty("crash.console.port");

        if (port == null) {
            log.info("crashSpringBootstrap disabled");
            return null;
        } else {
            log.info("crashSpringBootstrap enabled");
        }

        properties.setProperty("crash.telnet.port", port);

        //env에 값이 없어서 groovy 에서 가져오질 못해서 CrashConfig통해 로그인 정보를 가져오도록 변경
        String username = properties.getProperty("crash.auth.simple.username");
        String passwordEnc = properties.getProperty("crash.auth.simple.password");
        String password = null;
        try {
            if (passwordEnc.startsWith("ENC(") && passwordEnc.endsWith(")")) {
                password = ESBProductEncryption.decryptString(passwordEnc);
                properties.setProperty("crash.auth.simple.password", password);
            } else {
                password = passwordEnc;
            }
        } catch (Exception e) {
            log.warn("crashSpringBootstrap start fail..", e);
            return null;
        }

        this.username = username;
        this.password = password;

        //properties에 있는 내용을 log로 출력해줘
        properties.forEach((k, v) -> log.info("SpringBootstrap prop key : " + k + ", value : " + v));
        SpringBootstrap springBootstrap = new SpringBootstrap();
        springBootstrap.setConfig(properties);
        springBootstrap.setCmdMountPointConfig("file:commands/");
        springBootstrap.setConfMountPointConfig("file:.");

        return springBootstrap;

    }
}
