package mb.fw.atb;

import java.nio.charset.Charset;

public class Base64Test {
    public static void main(String[] args) {
        String utf8Str = "            VAS2BKCA253   BKCA011   106132019960300014831102004000011462024041613201900002282100011CB0000131770         20240416000019               12120000000440000 0000000000000124821김민규 (SH)               00000000000000000000000000000000000000079004113757083  13201920240416             000089Y0110120124821                                    ";
        String encoded = java.util.Base64.getEncoder().encodeToString(utf8Str.getBytes(Charset.forName("EUC-KR")));
        System.out.println("Encoded: " + encoded);
        String decoded = new String(java.util.Base64.getDecoder().decode(encoded) , Charset.forName("EUC-KR"));
        System.out.println("Decoded: " + decoded);
    }
}
