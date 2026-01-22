package mb.fw.atb.configuration;

import com.indigo.esb.nifs.IFTPClient;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.config.sub.Iftp;
import mb.fw.atb.constant.ATBPrefix;
import mb.fw.atb.config.IFConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class IFTPConfiguration {

    @Autowired(required = false)
    IFConfig ifconfig;

    @ConditionalOnProperty(name = ATBPrefix.DEFAULT_PREFIX + ".iftp.enabled", matchIfMissing = false)
    @Bean(name = "iftpClient")
    public IFTPClient iftpClient() {
        IFTPClient client = new IFTPClient();
        Iftp iftpCfg = ifconfig.getIftp();

        client.setRemoteHost(iftpCfg.getRemoteHosts());
        client.setDataPacketSize((short) iftpCfg.getDataPacketSize());
        client.setRetryDataCnt((short) iftpCfg.getRetryDataCnt());
        client.setViewCount(iftpCfg.getViewCount());
        client.setTransferSleep(iftpCfg.getTransferSleep());
        if (iftpCfg.isEncrypt()) {
            client.setEncrypt(true);
            if (iftpCfg.getPassword() != null) {
                client.setPassword(iftpCfg.getPassword());
            }
            //암호화 여부 적용 예정
        }
        return client;
    }
}
