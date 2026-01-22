package mb.fw.atb.config.sub;

import lombok.Data;

@Data
public class Gpki {
    /**
     * GPKI Configuration
     */
    String myServerId = null;
    String certFilePath = null;
    String envCertFilePathName = null;
    String envPrivateKeyFilePathName = null;
    String envPrivateKeyPasswd = null;
    String sigCertFilePathName = null;
    String sigPrivateKeyFilePathName = null;
    String sigPrivateKeyPasswd = null;
    String licensePath = null;
    String cryptoKey;
    String gpkiTargetServerIds = null;
    String ldapAddress = null;
}
