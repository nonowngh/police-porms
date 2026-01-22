package mb.fw.atb;

import com.mb.mci.common.util.HexUtil;
import mb.fw.transformation.util.HexDumper;
import mb.fw.transformation.util.HexViewer;

public class SOSI {

    public static void main(String[] args) throws Exception {
        //SO , SI를 String str 변수 앞뒤에 넣어라
        byte so = 0x0E;
        byte si = 0x0F;
        String str = "한글";
        byte[] bytes = str.getBytes();
        byte[] newBytes = new byte[bytes.length + 2];
        newBytes[0] = so;
        newBytes[newBytes.length - 1] = si;
        System.arraycopy(bytes, 0, newBytes, 1, bytes.length);
        System.out.println(new String(newBytes));
        System.out.println(HexViewer.view(newBytes));
    }
}
