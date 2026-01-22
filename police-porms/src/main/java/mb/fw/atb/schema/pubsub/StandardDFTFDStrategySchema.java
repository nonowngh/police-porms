package mb.fw.atb.schema.pubsub;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import mb.fw.atb.config.FieldsSchema;
import mb.fw.atb.config.Specifications;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class StandardDFTFDStrategySchema {

    public static Specifications specifications() {
        String name = "Large DB TO DB";
        String nameKor = "대용량 DB TO DB";
        String description = "대용량 DB 데이터를 연계하기위한 방법으로 파일TO파일 방식으로 연계한후 DB로 전송하는 방법입니다. , \n Master-Detail 테이블 구조를 지원합니다.";
        String version = "1.0";

        String sendContextJson = " {\n" +
                "  \"interfaceId\": \"IF_DTD_004\",\n" +
                "  \"actionType\": \"SENDER\",\n" +
                "  \"strategy\": \"StandardDFTFDStrategy\",\n" +
                "  \"cronExpression\": [\n" +
                "    \"0/20 * * * * ?\"\n" +
                "  ],\n" +
                "  \"actFirst\": false,\n" +
                "  \"onSignalLoop\": false,\n" +
                "  \"fileSendPath\": \"/atb/send/IF_DTD_004\",\n" +
                "  \"fileTempPath\": \"/atb/send/IF_DTD_004/temp\",\n" +
                "  \"fileCompletePath\": \"/atb/send/IF_DTD_004/complete\",\n" +
                "  \"fileErrorPath\": \"/atb/send/IF_DTD_004/error\",\n" +
                "  \"fileSuccessPath\": \"/atb/send/IF_DTD_004/success\",\n" +
                "  \"fileRetryCount\": 3,\n" +
                "  \"fileDelete\": false,\n" +
                "  \"fileType\": \"DELIMITER\",\n" +
                "  \"fileDelimiter\": \",\",\n" +
                "  \"fileDelimiterQualifier\": \"\\\"\",\n" +
                "  \"detailData\": false,\n" +
                "  \"detailNames\": [\"CHILD\"],\n" +
                "  \"sendSystemCode\": \"A01\",\n" +
                "  \"receiveSystemCode\": \"B01\",\n" +
                "  \"receiverIds\": [\"DFTFD_RECEIVER_01\"]\n" +
                "}";


        String receiverContextJson = " {\n" +
                "  \"interfaceId\": \"IF_DTD_004\",\n" +
                "  \"actionType\": \"RECEIVER\",\n" +
                "  \"strategy\": \"StandardDFTFDStrategy\",\n" +
                "  \"procedureCall\": true,\n" +
                "  \"fileRecvPath\": \"/atb/recv/IF_DTD_004\",\n" +
                "  \"fileTempPath\": \"/atb/recv/IF_DTD_004/temp\",\n" +
                "  \"fileSuccessPath\": \"/atb/recv/IF_DTD_004/success\",\n" +
                "  \"fileErrorPath\": \"/atb/recv/IF_DTD_004/error\",\n" +
                "  \"fileRetryCount\": 3,\n" +
                "  \"fileType\": \"DELIMITER\",\n" +
                "  \"fileDelimiter\": \",\",\n" +
                "  \"fileDelimiterQualifier\": \"\\\"\",\n" +
                "  \"fileFirstHeader\": true,\n" +
                "  \"fileErrorSkip\": false\n" +
                "}";

        //스펙 생성
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
        FieldsSchema receiverSchema = new FieldsSchema();
        Set<String> requiredFieldsReceiver = Sets.newHashSet("interfaceId", "actionType", "strategy", "fileRecvPath", "fileTempPath", "fileSuccessPath", "fileErrorPath");
        receiverSchema.setRequiredFields(requiredFieldsReceiver);

        Set<String> fixedValueFieldsReceiver = Sets.newHashSet("actionType", "strategy");
        receiverSchema.setFixedValueFields(fixedValueFieldsReceiver);

                /*
                  - interface-id: IF_DTD_004
                    action-type: RECEIVER
                    strategy: StandardDFTFDStrategy
                    procedure-call: true
                    file-recv-path: "/atb/recv/IF_DTD_004"
                    file-temp-path: "/atb/recv/IF_DTD_004/temp"
                    file-success-path: "/atb/recv/IF_DTD_004/success"
                    file-error-path: "/atb/recv/IF_DTD_004/error"
                    file-retry-count: 3
                    file-type: DELIMITER
                    file-delimiter: ','
                    file-delimiter-qualifier: '"'
                    file-first-header: true
                    file-error-skip: false
                 */

        Map<String, String> fieldComment = Maps.newHashMap();
        fieldComment.put("interfaceId", "인터페이스 아이디");
        fieldComment.put("actionType", "액션 타입(SENDER, RECEIVER)");
        fieldComment.put("strategy", "전략(패턴)");
        fieldComment.put("procedureCall", "프로시저 호출 여부(true, false)");
        fieldComment.put("fileRecvPath", "파일 수신 경로");
        fieldComment.put("fileTempPath", "파일 임시 경로");
        fieldComment.put("fileSuccessPath", "파일 수신 성공 경로");
        fieldComment.put("fileErrorPath", "파일 수신 에러 경로");
        fieldComment.put("fileRetryCount", "파일 재전송 횟수");
        fieldComment.put("fileType", "파일 타입 (DELIMITER, FIXED)");
        fieldComment.put("fileDelimiter", "파일 구분자");
        fieldComment.put("fileDelimiterQualifier", "파일 구분자 한정자");
        fieldComment.put("fileFirstHeader", "파일 헤더 포함 여부(true, false)");
        fieldComment.put("fileErrorSkip", "파일 에러시 건너뛰기 여부(true, false)");

        receiverSchema.setFieldComment(fieldComment);

        Map<String, String> defaultValue = Maps.newHashMap();
        defaultValue.put("interfaceId", "empty");
        defaultValue.put("actionType", "empty");
        defaultValue.put("strategy", "empty");
        defaultValue.put("procedureCall", "false");
        defaultValue.put("fileRecvPath", "empty");
        defaultValue.put("fileTempPath", "empty");
        defaultValue.put("fileSuccessPath", "empty");
        defaultValue.put("fileErrorPath", "empty");
        defaultValue.put("fileRetryCount", "3");
        defaultValue.put("fileType", "DELIMITER");
        defaultValue.put("fileDelimiter", ",");
        defaultValue.put("fileDelimiterQualifier", "\"");
        defaultValue.put("fileFirstHeader", "true");
        defaultValue.put("fileErrorSkip", "false");

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


        FieldsSchema senderSchema = new FieldsSchema();
        Set<String> requiredFields = Sets.newHashSet("interfaceId", "actionType", "strategy", "cronExpression", "file-send-path", "file-temp-path", "file-complete-path", "file-error-path", "file-success-path", "sendSystemCode", "receiveSystemCode", "receiverIds");
        senderSchema.setRequiredFields(requiredFields);

        Set<String> fixedValueFields = Sets.newHashSet("actionType", "strategy");
        senderSchema.setFixedValueFields(fixedValueFields);

          /*
          - interface-id: IF_DTD_004
            action-type: SENDER
            strategy: StandardDFTFDStrategy
            cron-expression: 0/20 * * * * ?
            act-first: false
            on-signal-loop: false
            file-send-path: "/atb/send/IF_DTD_004"
            file-temp-path: "/atb/send/IF_DTD_004/temp"
            file-complete-path: "/atb/send/IF_DTD_004/complete"
            file-error-path: "/atb/send/IF_DTD_004/error"
            file-success-path: "/atb/send/IF_DTD_004/success"
            file-retry-count: 3
            file-delete: false
            file-type: DELIMITER
            file-delimiter: ','
            file-delimiter-qualifier: '"'
            detail-data: true
            detail-names: CHILD
            send-system-code: A01
            receive-system-code: B01
            receiver-ids: DFTFD_RECEIVER_01
      */

        Map<String, String> fieldComment = Maps.newHashMap();
        fieldComment.put("interfaceId", "인터페이스 아이디");
        fieldComment.put("actionType", "액션 타입 (SENDER, RECEIVER)");
        fieldComment.put("strategy", "전략(패턴)");
        fieldComment.put("cronExpression", "스케줄링을 위한 크론 표현식");
        fieldComment.put("actFirst", "최초에 한번만 실행, 이때 크론 표현식은 무시(true, false)");
        fieldComment.put("onSignalLoop", "스케줄 실행후 데이터가 발생하면 한번더 수행 여부(true, false)");
        fieldComment.put("fileSendPath", "파일 전송 경로");
        fieldComment.put("fileTempPath", "파일 임시 경로");
        fieldComment.put("fileCompletePath", "파일 전송 완료 경로");
        fieldComment.put("fileErrorPath", "파일 전송 에러");
        fieldComment.put("fileSuccessPath", "파일 전송 성공(최종) ");
        fieldComment.put("fileRetryCount", "파일 재전송 횟수");
        fieldComment.put("fileDelete", "파일 전송 완료시 삭제 여부(true, false)");
        fieldComment.put("fileType", "파일 타입 (DELIMITER, FIXED)");
        fieldComment.put("fileDelimiter", "파일 구분자");
        fieldComment.put("fileDelimiterQualifier", "데이터를 감싸는 파일 구분자");
        fieldComment.put("detailData", "디테일 테이블 사용 여부 (true, false)");
        fieldComment.put("detailNames", "디테일 식별자 (예: CHILD,CHILD2)");
        fieldComment.put("sendSystemCode", "송신 시스템 코드(이력용)");
        fieldComment.put("receiveSystemCode", "수신 시스템 코드(이력용)");
        fieldComment.put("receiverIds", "수신 어댑터에 부여된 id");

        senderSchema.setFieldComment(fieldComment);

        Map<String, String> defaultValue = Maps.newHashMap();
        defaultValue.put("interfaceId", "empty");
        defaultValue.put("actionType", "empty");
        defaultValue.put("strategy", "empty");
        defaultValue.put("cronExpression", "empty");
        defaultValue.put("actFirst", "false");
        defaultValue.put("onSignalLoop", "false");
        defaultValue.put("fileSendPath", "empty");
        defaultValue.put("fileTempPath", "empty");
        defaultValue.put("fileCompletePath", "empty");
        defaultValue.put("fileErrorPath", "empty");
        defaultValue.put("fileSuccessPath", "empty");
        defaultValue.put("fileRetryCount", "3");
        defaultValue.put("fileDelete", "false");
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
