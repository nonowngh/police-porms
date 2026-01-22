package mb.fw.atb.config.sub;

import lombok.Data;

@Data
public class Iftp {

    /**
     * IFTP Configuration
     */
    boolean enabled;
    String[] remoteHosts;
    int dataPacketSize = 4096;
    int retryDataCnt = 20;
    int viewCount = 1000;
    int transferSleep = 0;
    boolean encrypt = false;
    String password=null;
}
