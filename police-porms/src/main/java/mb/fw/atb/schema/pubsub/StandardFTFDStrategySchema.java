package mb.fw.atb.schema.pubsub;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import mb.fw.atb.config.FieldsSchema;
import mb.fw.atb.config.Specifications;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class StandardFTFDStrategySchema {

    public static Specifications specifications() {

        String name = "File TO DB";
        String nameKor = "파일 TO DB";
        String description = "파일을 송신지에서 수신지로 전송후 DB로 전송하는 전략입니다. \nMaster-Detail 테이블 구조를 지원하지만 파일 규격을 준수해야합니다.";
        String version = "1.0";

        String sendContextJson = " {\n" +
                "  \"interfaceId\": \"IF_FTD_002\",\n" +
                "  \"actionType\": \"SENDER\",\n" +
                "  \"strategy\": \"StandardFTFDStrategy\",\n" +
                "  \"cronExpression\": [\n" +
                "    \"0/20 * * * * ?\"\n" +
                "  ],\n" +
                "  \"onSignalLoop\": false,\n" +
                "  \"fileSendPath\": \"/atb/send/IF_FTD_002\",\n" +
                "  \"fileTempPath\": \"/atb/send/IF_FTD_002/temp\",\n" +
                "  \"fileCompletePath\": \"/atb/send/IF_FTD_002/complete\",\n" +
                "  \"fileErrorPath\": \"/atb/send/IF_FTD_002/error\",\n" +
                "  \"fileSuccessPath\": \"/atb/send/IF_FTD_002/success\",\n" +
                "  \"fileSendCount\": 10,\n" +
                "  \"fileRetryCount\": 3,\n" +
                "  \"fileDelete\": true,\n" +
                "  \"fileGlobPattern\": \"*.{csv,doc,xml,xlsx,zip,hwp,exe}\",\n" +
                "  \"sendSystemCode\": \"A01\",\n" +
                "  \"receiveSystemCode\": \"B01\",\n" +
                "  \"receiverIds\": [\"FTFD_RECEIVER_01\"]\n" +
                "}";

        String receiverContextJson = " {\n" +
                "  \"interfaceId\": \"IF_FTD_002\",\n" +
                "  \"actionType\": \"RECEIVER\",\n" +
                "  \"strategy\": \"StandardFTFDStrategy\",\n" +
                "  \"procedureCall\": true,\n" +
                "  \"fileRecvPath\": \"/atb/recv/IF_FTD_002/\",\n" +
                "  \"fileTempPath\": \"/atb/recv/IF_FTD_002/temp\",\n" +
                "  \"fileSuccessPath\": \"/atb/recv/IF_FTD_002/success\",\n" +
                "  \"fileErrorPath\": \"/atb/recv/IF_FTD_002/error\",\n" +
                "  \"fileRetryCount\": 3,\n" +
                "  \"fileType\": \"DELIMITER\",\n" +
                "  \"fileDelimiter\": \",\",\n" +
                "  \"fileDelimiterQualifier\": \"\\\"\",\n" +
                "  \"fileFirstHeader\": false,\n" +
                "  \"fileErrorSkip\": true\n" +
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

        /**
         - interface-id: IF_FTD_002
         action-type: RECEIVER
         strategy: StandardFTFDStrategy
         procedure-call: true
         file-recv-path: "/atb/recv/IF_FTD_002/"
         file-temp-path: "/atb/recv/IF_FTD_002/temp"
         file-success-path: "/atb/recv/IF_FTD_002/success"
         file-error-path: "/atb/recv/IF_FTD_002/error"
         file-retry-count: 3
         file-type: DELIMITER
         file-delimiter: ','
         file-delimiter-qualifier: '"'
         file-first-header: false
         file-error-skip: true
         */

        FieldsSchema receiverSchema = new FieldsSchema();
        Set<String> requiredFieldsReceiver = Sets.newHashSet("interfaceId", "actionType", "strategy", "fileRecvPath", "fileTempPath", "fileSuccessPath", "fileErrorPath");
        receiverSchema.setRequiredFields(requiredFieldsReceiver);

        Set<String> fixedValueFieldsReceiver = Sets.newHashSet("actionType", "strategy");
        receiverSchema.setFixedValueFields(fixedValueFieldsReceiver);

        Map<String, String> fieldCommentReceiver = Maps.newHashMap();
        fieldCommentReceiver.put("interfaceId", "인터페이스 ID");
        fieldCommentReceiver.put("actionType", "동작 유형");
        fieldCommentReceiver.put("strategy", "전략");
        fieldCommentReceiver.put("procedureCall", "프로시저 호출 여부(true, false)");
        fieldCommentReceiver.put("fileRecvPath", "파일 수신 경로");
        fieldCommentReceiver.put("fileTempPath", "파일 임시 경로");
        fieldCommentReceiver.put("fileSuccessPath", "파일 성공 경로");
        fieldCommentReceiver.put("fileErrorPath", "파일 에러 경로");
        fieldCommentReceiver.put("fileRetryCount", "파일 재시도 횟수");
        fieldCommentReceiver.put("fileType", "파일 유형");
        fieldCommentReceiver.put("fileDelimiter", "파일 구분자");
        fieldCommentReceiver.put("fileDelimiterQualifier", "파일 구분자 한정자");
        fieldCommentReceiver.put("fileFirstHeader", "파일 헤더 존재 여부");
        fieldCommentReceiver.put("fileErrorSkip", "파일 에러 스킵 여부");

        receiverSchema.setFieldComment(fieldCommentReceiver);


        Map<String, String> defaultValueReceiver = Maps.newHashMap();
        defaultValueReceiver.put("interfaceId", "empty");
        defaultValueReceiver.put("actionType", "empty");
        defaultValueReceiver.put("strategy", "empty");
        defaultValueReceiver.put("procedureCall", "false");
        defaultValueReceiver.put("fileRecvPath", "empty");
        defaultValueReceiver.put("fileTempPath", "empty");
        defaultValueReceiver.put("fileSuccessPath", "empty");
        defaultValueReceiver.put("fileErrorPath", "empty");
        defaultValueReceiver.put("fileRetryCount", "3");
        defaultValueReceiver.put("fileType", "DELIMITER");
        defaultValueReceiver.put("fileDelimiter", ",");
        defaultValueReceiver.put("fileDelimiterQualifier", "\"");
        defaultValueReceiver.put("fileFirstHeader", "true");
        defaultValueReceiver.put("fileErrorSkip", "false");

        receiverSchema.setDefaultValue(defaultValueReceiver);
        return receiverSchema;
    }

    /**
     * 송신자 스키마 생성
     *
     * @return FieldsSchema
     */
    @NotNull
    private static FieldsSchema generateSenderSchema() {
        /**
         - interface-id: IF_FTD_002
         action-type: SENDER
         strategy: StandardFTFDStrategy
         cron-expression: 0/20 * * * * ?
         on-signal-loop: false
         file-send-path: "/atb/send/IF_FTD_002"
         file-temp-path: "/atb/send/IF_FTD_002/temp"
         file-complete-path: "/atb/send/IF_FTD_002/complete"
         file-error-path: "/atb/send/IF_FTD_002/error"
         file-success-path: "/atb/send/IF_FTD_002/success"
         file-send-count: 10
         file-retry-count: 3
         file-delete: true
         file-glob-pattern: "*.{csv,doc,xml,xlsx,zip,hwp,exe}"
         send-system-code: A01
         receive-system-code: B01
         receiver-ids: FTFD_RECEIVER_01
         */

        FieldsSchema senderSchema = new FieldsSchema();
        Set<String> requiredFields = Sets.newHashSet("interfaceId", "actionType", "strategy", "cronExpression", "fileSendPath", "fileTempPath", "fileCompletePath", "fileErrorPath", "fileSuccessPath", "sendSystemCode", "receiveSystemCode", "receiverIds");
        senderSchema.setRequiredFields(requiredFields);

        Set<String> fixedValueFields = Sets.newHashSet("actionType", "strategy");
        senderSchema.setFixedValueFields(fixedValueFields);

        Map<String, String> fieldComment = Maps.newHashMap();
        fieldComment.put("interfaceId", "인터페이스 ID");
        fieldComment.put("actionType", "동작 유형");
        fieldComment.put("strategy", "전략");
        fieldComment.put("cronExpression", "크론 표현식");
        fieldComment.put("onSignalLoop", "OnSignal 루프");
        fieldComment.put("actFirst", "ActFirst");
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
        fieldComment.put("detailData", "디테일 테이블 사용 여부 (true, false)");
        fieldComment.put("detailNames", "디테일 식별자 (예: CHILD,CHILD2)");
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
