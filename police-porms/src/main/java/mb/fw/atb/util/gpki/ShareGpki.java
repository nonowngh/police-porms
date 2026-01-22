package mb.fw.atb.util.gpki;

public class ShareGpki {

    public ShareGpki() {

    }

    public static NewGpkiUtil generateNewGpkiUtil(String myServerId, String certFilePath, String envCertFilePathName, String envPrivateKeyFilePathName, String envPrivateKeyPasswd, String sigCertFilePathName ,
                                                  String sigPrivateKeyFilePathName, String sigPrivateKeyPasswd , String gpkiLicPath,String targetServerId, String ldapAddress) throws Exception {
        NewGpkiUtil g = new NewGpkiUtil();
        //  GPKI 라이선스 설치 경로 ex) C:/gpki2/gpkisecureweb/conf
        g.setGpkiLicPath(gpkiLicPath);
        //  GPKI 인증서 설치 경로 C:/gpki2/gpkisecureweb/certs
        g.setCertFilePath(certFilePath);
        // 이용기관 인증서 파일 경로 ex) C:/gpki2/gpkisecureweb/certs/SVR1311000030_env.cer
        g.setEnvCertFilePathName(envCertFilePathName);
        // 이용기관 프라이빗 키 경로 ex) C:/gpki2/gpkisecureweb/certs/SVR1311000030_env.key
        g.setEnvPrivateKeyFilePathName(envPrivateKeyFilePathName);
        // 프라이빗 키 패스워드
        g.setEnvPrivateKeyPasswd(envPrivateKeyPasswd);
        // LDAP 의 사용유무
        // 미사용일 경우 암호화할 타겟의 인증서를 파일로 저장해놓고 사용하여야함.
        g.setIsLDAP(true);
        // "SVR1311000030"
        if(ldapAddress != null) {
            g.setLdapAddress(ldapAddress);
        }
        g.setMyServerId(myServerId);
        // 전자서명 파일 경로 ex) C:/gpki2/gpkisecureweb/certs/SVR1311000030_sig.cer
        g.setSigCertFilePathName(sigCertFilePathName);
        // 전자서명 프라이빗 키 경로 ex) C:/gpki2/gpkisecureweb/certs/SVR1311000030_sig.key
        g.setSigPrivateKeyFilePathName(sigPrivateKeyFilePathName);
        // 전자서명 프라이빗 키 경로 ex) C:/gpki2/gpkisecureweb/certs/SVR1311000030_sig.key
        g.setSigPrivateKeyPasswd(sigPrivateKeyPasswd);
        // 대상기관 정보 ,콤마로 여러개 등록 가능
        g.setTargetServerIdList(targetServerId);

        g.init();
        return g;
    }
}
