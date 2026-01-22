package mb.fw.atb.schema.pubsub;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import mb.fw.atb.config.FieldsSchema;
import mb.fw.atb.config.Specifications;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class StandardFTFStrategySchema {

    public static Specifications specifications() {

        String name = "File TO File";
        String nameKor = "파일 TO 파일";
        String description = "파일을 송신지에서 수신지로 전송하는 전략입니다.";
        String version = "1.0";

    /*
      - interface-id: IF_FTF_001
        action-type: SENDER
        strategy: StandardFTFStrategy
        cron-expression:
            - 0/20 * * * * ?
        on-signal-loop: true
        file-send-path: "/atb/send/IF_FTF_001"
        file-temp-path: "/atb/send/IF_FTF_001/temp"
        file-complete-path: "/atb/send/IF_FTF_001/complete"
        file-error-path: "/atb/send/IF_FTF_001/error"
        file-success-path: "/atb/send/IF_FTF_001/success"
        file-send-count: 1
        file-retry-count: 3
        file-delete: false
        file-glob-pattern: "*.{csv,doc,xml,xlsx,zip,hwp,exe,tar}"
        send-system-code: A01
        receive-system-code: B01
        receiver-ids: FTF_RECEIVER_01
        다음 스펙대로 sendContextJson을 생성
     */

        String sendContextJson = " {\n" +
                "  \"interfaceId\": \"IF_FTF_001\",\n" +
                "  \"actionType\": \"SENDER\",\n" +
                "  \"strategy\": \"StandardFTFStrategy\",\n" +
                "  \"cronExpression\": [\n" +
                "    \"0/20 * * * * ?\"\n" +
                "  ],\n" +
                "  \"onSignalLoop\": true,\n" +
                "  \"fileSendPath\": \"/atb/send/IF_FTF_001\",\n" +
                "  \"fileTempPath\": \"/atb/send/IF_FTF_001/temp\",\n" +
                "  \"fileCompletePath\": \"/atb/send/IF_FTF_001/complete\",\n" +
                "  \"fileErrorPath\": \"/atb/send/IF_FTF_001/error\",\n" +
                "  \"fileSuccessPath\": \"/atb/send/IF_FTF_001/success\",\n" +
                "  \"fileSendCount\": 1,\n" +
                "  \"fileRetryCount\": 3,\n" +
                "  \"fileDelete\": false,\n" +
                "  \"fileGlobPattern\": \"*.{csv,doc,xml,xlsx,zip,hwp,exe,tar}\",\n" +
                "  \"sendSystemCode\": \"A01\",\n" +
                "  \"receiveSystemCode\": \"B01\",\n" +
                "  \"receiverIds\": [\n" +
                "    \"FTF_RECEIVER_01\"\n" +
                "  ]\n" +
                "}";


        /*
          - interface-id: IF_FTF_001
            action-type: RECEIVER
            strategy: StandardFTFStrategy
            file-recv-path: "/atb/recv/IF_FTF_001"
            file-temp-path: "/atb/recv/IF_FTF_001/temp"
            file-retry-count: 3
            다음 스펙대로 receiverContextJson을 생성
             */
        String receiverContextJson = " {\n" +
                "  \"interfaceId\": \"IF_FTF_001\",\n" +
                "  \"actionType\": \"RECEIVER\",\n" +
                "  \"strategy\": \"StandardFTFStrategy\",\n" +
                "  \"fileRecvPath\": \"/atb/recv/IF_FTF_001\",\n" +
                "  \"fileTempPath\": \"/atb/recv/IF_FTF_001/temp\",\n" +
                "  \"fileRetryCount\": 3\n" +
                "}";

        Specifications specifications = new Specifications();
        specifications.setName(name);
        specifications.setNameKor(nameKor);
        specifications.setVersion(version);
        specifications.setDescription(description);
        specifications.setSender(sendContextJson);
        specifications.setReceiver(receiverContextJson);

        //송신자 스키마
        FieldsSchema senderSchema = generateSenderSchema();
        specifications.setSenderSchema(senderSchema);

        //수신자 스키마
        FieldsSchema receiverSchema = generateReceiverSchema();
        specifications.setReceiverSchema(receiverSchema);

        return specifications;
    }

    /**
     * 수신자 스키마 생성
     *
     * @return FieldsSchema
     */
    @NotNull
    private static FieldsSchema generateReceiverSchema() {
             /*
                - interface-id: IF_FTF_001
                  action-type: RECEIVER
                  strategy: StandardFTFStrategy
                  file-recv-path: "/atb/recv/IF_FTF_001"
                  file-temp-path: "/atb/recv/IF_FTF_001/temp"
                  file-retry-count: 3
         */
        FieldsSchema receiverSchema = new FieldsSchema();
        Set<String> requiredFieldsReceiver = Sets.newHashSet("interfaceId", "actionType", "strategy", "fileRecvPath", "fileTempPath");
        receiverSchema.setRequiredFields(requiredFieldsReceiver);

        Set<String> fixedValueFieldsReceiver = Sets.newHashSet("actionType", "strategy");
        receiverSchema.setFixedValueFields(fixedValueFieldsReceiver);

        Map<String, String> fieldComment = Maps.newHashMap();
        fieldComment.put("interfaceId", "인터페이스 ID");
        fieldComment.put("actionType", "동작 유형 (SENDER, RECEIVER)");
        fieldComment.put("strategy", "전략(패턴)");
        fieldComment.put("fileRecvPath", "파일 수신 경로");
        fieldComment.put("fileTempPath", "파일 임시 경로");

        receiverSchema.setFieldComment(fieldComment);


        Map<String, String> defaultValue = Maps.newHashMap();
        defaultValue.put("interfaceId", "empty");
        defaultValue.put("actionType", "empty");
        defaultValue.put("strategy", "empty");
        defaultValue.put("fileRecvPath", "empty");
        defaultValue.put("fileTempPath", "empty");
        defaultValue.put("fileRetryCount", "3");


        receiverSchema.setDefaultValue(defaultValue);
        return receiverSchema;
    }

    /**
     * 송신자 스키마 생성
     *
     * @return FieldsSchema
     */
    @NotNull
    private static FieldsSchema generateSenderSchema() {
            /*
                  - interface-id: IF_FTF_001
                    action-type: SENDER
                    strategy: StandardFTFStrategy
                    cron-expression: 0/20 * * * * ?
                    act-first: false
                    on-signal-loop: true
                    file-send-path: "/atb/send/IF_FTF_001"
                    file-temp-path: "/atb/send/IF_FTF_001/temp"
                    file-complete-path: "/atb/send/IF_FTF_001/complete"
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

        FieldsSchema senderSchema = new FieldsSchema();
        Set<String> requiredFields = Sets.newHashSet("interfaceId", "actionType", "strategy", "cronExpression", "fileSendPath", "fileTempPath", "fileCompletePath", "fileErrorPath", "fileSuccessPath", "sendSystemCode", "receiveSystemCode", "receiverIds");
        senderSchema.setRequiredFields(requiredFields);

        Set<String> fixedValueFields = Sets.newHashSet("actionType", "strategy");
        senderSchema.setFixedValueFields(fixedValueFields);

        Map<String, String> fieldComment = Maps.newHashMap();
        fieldComment.put("interfaceId", "인터페이스 ID");
        fieldComment.put("actionType", "동작 유형 (SENDER, RECEIVER)");
        fieldComment.put("strategy", "전략(패턴)");
        fieldComment.put("cronExpression", "스케줄링을 위한 크론 표현식");
        fieldComment.put("actFirst", "최초에 한번만 실행, 이때 크론 표현식은 무시(true, false)");
        fieldComment.put("onSignalLoop", "스케줄 실행후 데이터가 발생하면 한번더 수행 여부(true, false)");
        fieldComment.put("fileSendPath", "파일 송신 경로");
        fieldComment.put("fileTempPath", "파일 임시 경로");
        fieldComment.put("fileCompletePath", "파일 완료 경로");
        fieldComment.put("fileErrorPath", "파일 에러 경로");
        fieldComment.put("fileSuccessPath", "파일 성공 경로");
        fieldComment.put("fileRetryCount", "파일 재시도 횟수");
        fieldComment.put("fileDelete", "파일 삭제");
        fieldComment.put("fileGlobPattern", "파일 Glob 패턴");
        fieldComment.put("fileType", "파일 유형");
        fieldComment.put("fileDelimiter", "파일 구분자");
        fieldComment.put("fileDelimiterQualifier", "파일 구분자 한정자");
        fieldComment.put("detailData", "상세 데이터");
        fieldComment.put("detailNames", "상세 이름");
        fieldComment.put("sendSystemCode", "송신 시스템 코드");
        fieldComment.put("receiveSystemCode", "수신 시스템 코드");
        fieldComment.put("receiverIds", "수신자 ID");

        senderSchema.setFieldComment(fieldComment);

        Map<String, String> defaultValue = Maps.newHashMap();
        defaultValue.put("interfaceId", "empty");
        defaultValue.put("actionType", "empty");
        defaultValue.put("strategy", "empty");
        defaultValue.put("cronExpression", "empty");
        defaultValue.put("onSignalLoop", "false");
        defaultValue.put("actFirst", "false");
        defaultValue.put("fileSendPath", "empty");
        defaultValue.put("fileTempPath", "empty");
        defaultValue.put("fileCompletePath", "empty");
        defaultValue.put("fileErrorPath", "empty");
        defaultValue.put("fileSuccessPath", "empty");
        defaultValue.put("fileRetryCount", "3");
        defaultValue.put("fileDelete", "false");
        defaultValue.put("fileGlobPattern", "*");
        defaultValue.put("fileType", "DELIMITER");
        defaultValue.put("fileDelimiter", ",");
        defaultValue.put("fileDelimiterQualifier", "\"");
        defaultValue.put("detailData", "false");
        defaultValue.put("detailNames", "empty");
        defaultValue.put("sendSystemCode", "empty");
        defaultValue.put("receiveSystemCode", "empty");
        defaultValue.put("receiverIds", "empty");

        senderSchema.setDefaultValue(defaultValue);
        return senderSchema;
    }
}
