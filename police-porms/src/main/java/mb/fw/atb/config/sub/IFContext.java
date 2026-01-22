package mb.fw.atb.config.sub;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import mb.fw.atb.enums.ActionType;

import java.util.Map;


/**
 * 설정파일 Config
 */
@Data
public class IFContext {
    String strategy;
    String dataClass;
    ActionType actionType;
    String contextPath;
    String singleAddress;
    String shareBodyRootName;
    String interfaceId;
    String[] cronExpression;
    String fileSendPath;
    String fileTempPath;
    String fileErrorPath;
    String fileCompletePath;
    String fileSuccessPath;
    String fileRecvPath;
    String fileGlobPattern = "*";
    String fileType = "DELIMITER";
    String gpkiTargetServerId;
    boolean actFirst = false;
    int actFirstIntervalSec = 10;

    //NetworkAdaptor config
    String mapperGroupId;
    String parserId;
    String inMapperType = "BYPASS";
    String outMapperType = "BYPASS";
    String outBoundMapperMessageId;
    String inBoundMapperMessageId;
    String inboundCallUrl;

    boolean procedureCall = false;

    boolean lengthFieldInject = false;
    int lengthFieldPosition = 0;
    int lengthFieldLength = 4;
    int lengthFieldAddition = -4;

    String processMode = "ASYNC";

    boolean fileErrorSkip = false;
    boolean onSignalLoop = false;
    boolean fileFirstHeader = true;
    char fileDelimiter = ',';
    char fileDelimiterQualifier = '\"';
    String sendSystemCode = "";
    String receiveSystemCode = "";
    String[] receiverIds = {"empty"};
    int fileRetryCount = 3;
    int fileSendCount = 10;
    boolean fileDelete;
    boolean hubFileDelete;


    String clientId;
    String clientClass;
    String targetUrl;

    boolean detailData = false;
    String[] detailNames;


    public Map createContextMap() {
        ObjectMapper om = new ObjectMapper();
        return om.convertValue(this, Map.class);
    }


}
