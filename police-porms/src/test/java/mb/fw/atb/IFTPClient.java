package mb.fw.atb;

import com.indigo.esb.nifs.IFTPServer;
import com.indigo.esb.nifs.exception.IFTPChannelCloseException;
import com.indigo.esb.nifs.exception.IFTPConnectionFailException;
import com.indigo.esb.nifs.exception.IFTPFileNotFoundException;
import com.indigo.esb.nifs.exception.IFTPRequestTimeOutException;
import java.io.IOException;

public class IFTPClient {

    public static void main(String[] args) throws IOException, IFTPConnectionFailException, IFTPRequestTimeOutException, IFTPChannelCloseException, IFTPFileNotFoundException {
        //SERVER_START();
       CLIENT_START();
    }

    private static void CLIENT_START() throws IFTPConnectionFailException, IFTPRequestTimeOutException, IFTPChannelCloseException, IFTPFileNotFoundException {
        com.indigo.esb.nifs.IFTPClient iftpClient = new com.indigo.esb.nifs.IFTPClient();
        iftpClient.setRemoteHost(new String[]{"192.168.20.127:24052"});
        //iftpClient.setRemoteHost(new String[]{"127.0.0.1:24052"});
        iftpClient.setViewCount(500);
        iftpClient.setRetryDataCnt(100);
        iftpClient.setTransferSleep(0);
        iftpClient.remoteFileDelete("SPRING_BOOT" , "atb-1.0.0.jar");
        iftpClient.put("SPRING_BOOT", "C:\\works\\atb\\target\\", "atb-1.0.0.jar");
    }

    private static void SERVER_START() throws IOException {
        IFTPServer iftpServer = new IFTPServer();
        iftpServer.setBindPort(24052);
        iftpServer.setStorePath("C:\\works\\atb\\target\\");
        iftpServer.setViewCount(500);
        iftpServer.bind();
    }
}
