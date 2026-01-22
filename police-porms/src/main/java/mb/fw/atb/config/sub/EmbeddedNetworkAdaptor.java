package mb.fw.atb.config.sub;

import lombok.Data;
import mb.fw.net.common.et.TCPMode;

@Data
public class EmbeddedNetworkAdaptor {

    String type;
    TCPMode tcpMode;
    String[] remoteHosts;
    int[] remotePorts;
    int[] bindPorts;
    String pollingImplClass;
    int connectTimeoutSec = 30;
    int reconnectSec;
    int recvTaskCount;
    boolean hexMode;
    String callType = "HTTP";
    int[] messageId;
    int[] messageIdLen;
    int[] workCode;
    int[] workCodeLen;

    //LengthField
    int maxFrameLength;
    int lengthFieldOffset;
    int lengthFieldLength;
    int lengthAdjustment;
    int initialBytesToStrip;
    boolean stringValueUse;
    boolean sendClose;

    //Delimiter
    String hexDelimiters;
    String addLastHexDelimiter;

    //Fixed Length
    int frameLength;

    String groupId;
    String srcOrg;
    String trgOrg;
    String loggingType;
}
