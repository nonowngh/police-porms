package mb.fw.atb.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.config.sub.*;
import mb.fw.atb.enums.ActionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "atb.if-config")
@Slf4j
public class IFConfig {

    @Autowired
    private JmsTemplate jmsTemplate;
    @PostConstruct
    public void init() {
    }
    boolean autoConfig = false;
    String id;
    String description;
    String address;
    String serverIp;
    String prefix;

    String seedOpMode = "ECB";
    String encryptionKey = null;
    String initVector = null;

    //데이터에서 receiverId를 찾는다.
    String dataReceiverId;

    boolean resultLogging = true;
    boolean sendLogging = true;

    IFResult IfResult = new IFResult();
    List<IFContext> context;

    String mapperRemoteQueue;
    long mapperReceiveTimeout = 10000;
    EmbeddedNetworkAdaptor embeddedNetworkAdaptor;
    RemoteNetworkAdaptor remoteNetworkAdaptor;
    Iftp iftp;
    Gpki gpki;
    FTIInfo ftiInfo;

    //jms consumer 추가
    String senderConcurrency = "2-10";
    String receiverConcurrency = "10-10";

    //NONE , JMS
    String routeType = "NONE";

    //OAuth2 설정을 위해 추가
    Oauth2 oauth2;

    public IFContext findContext(String interfaceId, ActionType actionType) {
        try {
            IFContext selectionContext = context.stream().filter(ifContext -> {
                if (ifContext.getInterfaceId().equals(interfaceId) && ifContext.getActionType() == actionType) {
                    return true;
                } else {
                    return false;
                }
            }).findFirst().get();
            return selectionContext;
        } catch (Exception e) {
            return null;
        }
    }

    public IFContext findContext(String interfaceId) {
        try {
            IFContext selectionContext = context.stream().filter(ifContext -> {
                if (ifContext.getInterfaceId().equals(interfaceId)) {
                    return true;
                } else {
                    return false;
                }
            }).findFirst().get();
            return selectionContext;
        } catch (Exception e) {
            return null;
        }
    }
}