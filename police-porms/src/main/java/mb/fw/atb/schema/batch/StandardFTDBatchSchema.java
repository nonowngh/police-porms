package mb.fw.atb.schema.batch;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import mb.fw.atb.config.FieldsSchema;
import mb.fw.atb.config.Specifications;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class StandardFTDBatchSchema {

    /**
     * 설정 완료 2024-11-07
     *
     * @return
     */
    public static Specifications specifications() {
        String name = "File TO DB Batch";
        String nameKor = "파일 TO DB 배치";
        String description = "파일을 읽어 DB에 데이터를 삽입하는 배치입니다.";
        String version = "1.0";

        String sendContextJson = " {\n" +
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

        //스펙 생성
        Specifications specifications = new Specifications();
        specifications.setName(name);
        specifications.setNameKor(nameKor);
        specifications.setVersion(version);
        specifications.setDescription(description);
        specifications.setSender(sendContextJson);

        //송신자 스키마
        FieldsSchema senderSchema = generateSchema();
        specifications.setSenderSchema(senderSchema);

        return specifications;
    }


    /**
     * 송신자 스키마 생성
     *
     * @return FieldsSchema
     */
    @NotNull
    private static FieldsSchema generateSchema() {

  /*
          - interface-id: IF_FTD_003
            action-type: SENDER
            strategy: StandardFTDBatch
            cron-expression: 0/20 * * * * ?
            on-signal-loop: false
            procedure-call: true
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
            file-error-skip: false
            file-first-header: true
            send-system-code: A01
            receive-system-code: B01
      */

        FieldsSchema senderSchema = new FieldsSchema();
        Set<String> requiredFields = Sets.newHashSet("interfaceId", "actionType", "strategy", "cronExpression", "file-send-path", "file-temp-path", "file-complete-path", "file-success-path", "file-error-path", "file-send-count", "send-system-code", "receive-system-code");
        senderSchema.setRequiredFields(requiredFields);

        Set<String> fixedValueFields = Sets.newHashSet("actionType");
        senderSchema.setFixedValueFields(fixedValueFields);

        Map<String, String> fieldComment = Maps.newHashMap();
        fieldComment.put("interfaceId", "인터페이스 ID");
        fieldComment.put("actionType", "동작 유형 (SENDER, RECEIVER)");
        fieldComment.put("strategy", "전략(패턴)");
        fieldComment.put("cronExpression", "배치 실행 주기");
        fieldComment.put("onSignalLoop", "OnSignal 반복 여부");
        fieldComment.put("procedureCall", "프로시저 호출 여부");
        fieldComment.put("fileSendPath", "파일 송신 경로");
        fieldComment.put("fileTempPath", "파일 임시 경로");
        fieldComment.put("fileCompletePath", "파일 완료 경로");
        fieldComment.put("fileSuccessPath", "파일 성공 경로");
        fieldComment.put("fileErrorPath", "파일 에러 경로");
        fieldComment.put("fileSendCount", "파일 송신 횟수");
        fieldComment.put("fileDelete", "파일 삭제 여부");
        fieldComment.put("fileGlobPattern", "파일 패턴");
        fieldComment.put("fileType", "파일 타입");
        fieldComment.put("fileDelimiter", "파일 구분자");
        fieldComment.put("fileDelimiterQualifier", "파일 구분자 한정자");
        fieldComment.put("fileErrorSkip", "파일 에러 스킵 여부");
        fieldComment.put("fileFirstHeader", "파일 헤더 여부");
        fieldComment.put("sendSystemCode", "송신 시스템 코드");
        fieldComment.put("receiveSystemCode", "수신 시스템 코드");
        senderSchema.setFieldComment(fieldComment);

        Map<String, String> defaultValue = Maps.newHashMap();
        defaultValue.put("interfaceId", "empty");
        defaultValue.put("actionType", "empty");
        defaultValue.put("strategy", "empty");
        defaultValue.put("cronExpression", "empty");
        defaultValue.put("onSignalLoop", "false");
        defaultValue.put("procedureCall", "false");
        defaultValue.put("fileSendPath", "empty");
        defaultValue.put("fileTempPath", "empty");
        defaultValue.put("fileCompletePath", "empty");
        defaultValue.put("fileSuccessPath", "empty");
        defaultValue.put("fileErrorPath", "empty");
        defaultValue.put("fileSendCount", "10");
        defaultValue.put("fileDelete", "false");
        defaultValue.put("fileGlobPattern", "*");
        defaultValue.put("fileType", "DELIMITER");
        defaultValue.put("fileDelimiter", ",");
        defaultValue.put("fileDelimiterQualifier", "\"");
        defaultValue.put("fileErrorSkip", "false");
        defaultValue.put("fileFirstHeader", "true");
        defaultValue.put("sendSystemCode", "empty");
        defaultValue.put("receiveSystemCode", "empty");

        senderSchema.setDefaultValue(defaultValue);
        return senderSchema;
    }
}
