package mb.fw.atb.config.sub;

import lombok.Data;

import java.util.Map;

@Data
public class RemoteNetworkAdaptor {
    /**
     * NetworkAdaptor Configuration
     */
    Map<String, String> outboundQueueMap;
    String inboundQueue;
    long receiveTimeout = 10000;
}
