package mb.fw.atb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.config.sub.IFContext;

@Slf4j
public class ContextJsonTest {
    public static void main(String[] args) {
        /*
          - interfaceId: IF_DTD_001
            actionType: SENDER
            strategy: StandardDTDStrategy
            cronExpression:
               - 0/20 * * * * ?
            on-signal-loop: false
            send-system-code: A01
            receive-system-code: B01
            receiver-ids: DTD_RECEIVER_01
     */
        String contextJson = "{\n" +
                "  \"interfaceId\": \"IF_DTD_001\",\n" +
                "  \"actionType\": \"SENDER\",\n" +
                "  \"strategy\": \"StandardDTDStrategy\",\n" +
                "  \"cronExpression\": [\n" +
                "    \"0/20 * * * * ?\"\n" +
                "  ],\n" +
                "  \"onSignalLoop\": false,\n" +
                "  \"sendSystemCode\": \"A01\",\n" +
                "  \"receiveSystemCode\": \"B01\",\n" +
                "  \"receiverIds\": \"DTD_RECEIVER_01\"\n" +
                "}";
        log.info("contextJson : {}", contextJson);
        //json to object
        ObjectMapper mapper = new ObjectMapper();

        IFContext context;
        {
            try {
                context = mapper.readValue(contextJson, IFContext.class);
                log.info("context : {}", context);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        /*
        - interfaceId: IF_FTD_003
        actionType: SENDER
        strategy: StandardFTDBatch
        cronExpression:
               - 0/20 * * * * ?
        on-signal-loop: false
        procedureCall: true
        file-send-path: "/atb/send/IF_FTD_003"
        file-temp-path: "/atb/send/IF_FTD_003/temp"
        file-complete-path: "/atb/send/IF_FTD_003/complete"
        file-success-path: "/atb/send/IF_FTD_003/success"
        file-error-path: "/atb/send/IF_FTD_003/error"
        file-send-count: 1
        file-delete: true
        file-glob-pattern: "*.{csv,doc,xml,xlsx,zip,hwp,exe}"
        file-type: DELIMITER
        file-delimiter: ','
        file-delimiter-qualifier: '"'
        fileErrorSkip: false
        file-first-header: true
        send-system-code: A01
        receive-system-code: B01
        */
        //동일하게 contextJson을 작성해줘
        String contextJson2 = "{\n" +
                "  \"interfaceId\": \"IF_FTD_003\",\n" +
                "  \"actionType\": \"SENDER\",\n" +
                "  \"strategy\": \"StandardFTDBatch\",\n" +
                "  \"cronExpression\": [\n" +
                "    \"0/20 * * * * ?\"\n" +
                "  ],\n" +
                "  \"onSignalLoop\": false,\n" +
                "  \"procedureCall\": true,\n" +
                "  \"fileSendPath\": \"/atb/send/IF_FTD_003\",\n" +
                "  \"fileTempPath\": \"/atb/send/IF_FTD_003/temp\",\n" +
                "  \"fileCompletePath\": \"/atb/send/IF_FTD_003/complete\",\n" +
                "  \"fileSuccessPath\": \"/atb/send/IF_FTD_003/success\",\n" +
                "  \"fileErrorPath\": \"/atb/send/IF_FTD_003/error\",\n" +
                "  \"fileSendCount\": 1,\n" +
                "  \"fileDelete\": true,\n" +
                "  \"fileGlobPattern\": \"*.{csv,doc,xml,xlsx,zip,hwp,exe}\",\n" +
                "  \"fileType\": \"DELIMITER\",\n" +
                "  \"fileDelimiter\": \",\",\n" +
                "  \"fileDelimiterQualifier\": \"\\\"\",\n" +
                "  \"fileErrorSkip\": false,\n" +
                "  \"fileFirstHeader\": true,\n" +
                "  \"sendSystemCode\": \"A01\",\n" +
                "  \"receiveSystemCode\": \"B01\"\n" +
                "}";
        log.info("contextJson2 : {}", contextJson2);

        /*
              - interfaceId: IF_FTF_001
                actionType: SENDER
                strategy: StandardFTFStrategy
                cronExpression:
                    - 0/20 * * * * ?
                on-signal-loop: true
                file-send-path: "/atb/send/IF_FTF_001"
                file-temp-path: "/atb/send/IF_FTF_001/temp"
                file-complete-path: "/atb//send/IF_FTF_001/complete"
                file-error-path: "/atb/send/IF_FTF_001/error"
                file-success-path: "/atb/send/IF_FTF_001/success"
                file-send-count: 1
                file-retry-count: 3
                file-delete: false
                file-glob-pattern: "*.{csv,doc,xml,xlsx,zip,hwp,exe,tar}"
                send-system-code: A01
                receive-system-code: B01
                receiver-ids: FTF_RECEIVER_01
         */

        //동일하게 contextJson을 작성해줘
        String contextJson3 = "{\n" +
                "  \"interfaceId\": \"IF_FTF_001\",\n" +
                "  \"actionType\": \"SENDER\",\n" +
                "  \"strategy\": \"StandardFTFStrategy\",\n" +
                "  \"cronExpression\": [\n" +
                "    \"0/20 * * * * ?\"\n" +
                "  ],\n" +
                "  \"onSignalLoop\": true,\n" +
                "  \"fileSendPath\": \"/atb/send/IF_FTF_001\",\n" +
                "  \"fileTempPath\": \"/atb/send/IF_FTF_001/temp\",\n" +
                "  \"fileCompletePath\": \"/atb//send/IF_FTF_001/complete\",\n" +
                "  \"fileErrorPath\": \"/atb/send/IF_FTF_001/error\",\n" +
                "  \"fileSuccessPath\": \"/atb/send/IF_FTF_001/success\",\n" +
                "  \"fileSendCount\": 1,\n" +
                "  \"fileRetryCount\": 3,\n" +
                "  \"fileDelete\": false,\n" +
                "  \"fileGlobPattern\": \"*.{csv,doc,xml,xlsx,zip,hwp,exe,tar}\",\n" +
                "  \"sendSystemCode\": \"A01\",\n" +
                "  \"receiveSystemCode\": \"B01\",\n" +
                "  \"receiverIds\": \"FTF_RECEIVER_01\"\n" +
                "}";
        log.info("contextJson3 : {}", contextJson3);

        /*
              - interfaceId: IF_FTF_001
                actionType: RECEIVER
                strategy: StandardFTFStrategy
                file-recv-path: "/atb/recv/IF_FTF_001"
                file-temp-path: "/atb/recv/IF_FTF_001/temp"
                file-retry-count: 3
         */
        //동일하게 contextJson을 작성해줘
        String contextJson4 = "{\n" +
                "  \"interfaceId\": \"IF_FTF_001\",\n" +
                "  \"actionType\": \"RECEIVER\",\n" +
                "  \"strategy\": \"StandardFTFStrategy\",\n" +
                "  \"fileRecvPath\": \"/atb/recv/IF_FTF_001\",\n" +
                "  \"fileTempPath\": \"/atb/recv/IF_FTF_001/temp\",\n" +
                "  \"fileRetryCount\": 3\n" +
                "}";
        log.info("contextJson4 : {}", contextJson4);
    }


}
