package mb.fw.atb.schema.batch;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import mb.fw.atb.config.FieldsSchema;
import mb.fw.atb.config.Specifications;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class StandardDTFBatchSchema {

    public static Specifications specifications() {
        String name = "DB TO FILE BATCH";
        String nameKor = "DB TO 파일 배치";
        String description = "DB 데이터를 FILE 로 생성합니다. DELIMITER, XML , JSON을 지원" +
                " , \n Master-Detail 테이블 구조를 지원합니다.";
        String version = "1.0";

        String sendContextJson = " {\n" +
                "  \"interfaceId\": \"IF_DTF_001\",\n" +
                "  \"actionType\": \"CUSTOM\",\n" +
                "  \"strategy\": \"StandardDTFBatch\",\n" +
                "  \"cronExpression\": [\n" +
                "    \"0/20 * * * * ?\"\n" +
                "  ],\n" +
                "  \"actFirst\": false,\n" +
                "  \"onSignalLoop\": false,\n" +
                "  \"fileTempPath\": \"/atb/recv/IF_DTF_001/temp\",\n" +
                "  \"fileErrorPath\": \"/atb/recv/IF_DTF_001/error\",\n" +
                "  \"fileRecvPath\": \"/atb/recv/IF_DTF_001/success\",\n" +
                "  \"fileType\": \"DELIMITER\",\n" +
                "  \"fileDelimiter\": \",\",\n" +
                "  \"detailData\": false,\n" +
                "  \"detailNames\": [\"CHILD\"],\n" +
                "  \"sendSystemCode\": \"A01\",\n" +
                "  \"receiveSystemCode\": \"B01\",\n" +
                "}";

        //스펙 생성
        Specifications specifications = new Specifications();
        specifications.setName(name);
        specifications.setNameKor(nameKor);
        specifications.setVersion(version);
        specifications.setDescription(description);
        specifications.setSender(sendContextJson);

        //송신자 스키마
        FieldsSchema senderSchema = generateSenderSchema();
        specifications.setSenderSchema(senderSchema);

        return specifications;
    }

    /**
     * 송신자 스키마 생성
     *
     * @return FieldsSchema
     */
    @NotNull
    private static FieldsSchema generateSenderSchema() {


        FieldsSchema senderSchema = new FieldsSchema();
        Set<String> requiredFields = Sets.newHashSet("interfaceId", "actionType", "strategy", "cronExpression", "fileRecvPath", "fileErrorPath", "sendSystemCode", "receiveSystemCode");
        senderSchema.setRequiredFields(requiredFields);

        Set<String> fixedValueFields = Sets.newHashSet("actionType", "strategy");
        senderSchema.setFixedValueFields(fixedValueFields);

        Map<String, String> fieldComment = Maps.newHashMap();
        fieldComment.put("interfaceId", "인터페이스 아이디");
        fieldComment.put("actionType", "액션 타입 (SENDER, RECEIVER , CUSTOM)");
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
