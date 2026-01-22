package mb.fw.atb;

import com.indigo.esb.nifs.IFTPClient;
import com.indigo.esb.nifs.exception.IFTPChannelCloseException;
import com.indigo.esb.nifs.exception.IFTPConnectionFailException;
import com.indigo.esb.nifs.exception.IFTPFileNotFoundException;
import com.indigo.esb.nifs.exception.IFTPRequestTimeOutException;

import java.io.IOException;

public class IFTPServer {

    public static void main(String[] args) throws IOException, IFTPConnectionFailException, IFTPRequestTimeOutException, IFTPChannelCloseException, IFTPFileNotFoundException {
        //SERVER_START();
       // CLIENT_START();
    }

    private static void CLIENT_START() throws IFTPConnectionFailException, IFTPRequestTimeOutException, IFTPChannelCloseException, IFTPFileNotFoundException {
        IFTPClient iftpClient = new IFTPClient();
        //iftpClient.setRemoteHost(new String[]{"192.168.1.127:24052"});
        iftpClient.setRemoteHost(new String[]{"127.0.0.1:24052"});
        iftpClient.setViewCount(500);
        iftpClient.setRetryDataCnt(100);
        iftpClient.setTransferSleep(0);
        iftpClient.put("SPRING_BOOT", "C:\\works\\atb\\target\\", "atb-1.0.0.jar");
    }

    private static void SERVER_START() throws IOException {
        com.indigo.esb.nifs.IFTPServer iftpServer = new com.indigo.esb.nifs.IFTPServer();
        iftpServer.setBindPort(24052);
        iftpServer.setStorePath("C:\\works\\atb\\target\\");
        iftpServer.setViewCount(500);
        iftpServer.bind();
    }
}
