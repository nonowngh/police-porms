package mb.fw.atb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.apache.tomcat.util.buf.HexUtils;

public class AdaptorCmcsTest {

    public static void main(String[] args) throws Exception {

       // System.out.println(HexUtils.toHexString("^#".getBytes()));
        // 900 : status
        // 102 : stop 
        // 110 : pid set
        // 1000 : currentStatus
        String[] statusArr = new String[]{"900", "102" , "110" , "1000"};
        for (String status : statusArr) {
            switch (status) {
                case "900":
                    System.out.println("[ status ]");
                    break;
                case "102":
                    System.out.println("[ stop ]");
                    break;
                case "110":
                    System.out.println("[ pid set ]");
                    break;
                case "1000":
                    System.out.println("[ currentStatus ]");
                    break;
            }

            ByteBuf byteBuf = null;
            if(status.equals("110")){
                 byteBuf = Unpooled.wrappedBuffer(Unpooled.directBuffer().writeInt(status.getBytes().length + 6).writeBytes(status.getBytes()).writeBytes("^#1111".getBytes()));
            }else{
                 byteBuf = Unpooled.wrappedBuffer(Unpooled.directBuffer().writeInt(status.getBytes().length + 2).writeBytes(status.getBytes()));

            }
            System.out.println(ByteBufUtil.prettyHexDump(byteBuf));
            System.out.println(ByteBufUtil.hexDump(byteBuf));
        }
    }

}
