package mb.fw.atb.schema.pubsub;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import mb.fw.atb.config.FieldsSchema;
import mb.fw.atb.config.Specifications;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class StandardDTDStrategySchema {

    /**
     * 설정 완료 2024-11-07
     * @return 
     */
    public static Specifications specifications() {
        String name = "Normal DB TO DB";
        String nameKor = "일반 DB TO DB";
        String description = "송신 DB에서 수신 DB로 JMS(Java Message Service)를 이용한 데이터를 전송하는 전략입니다.\n Master-Detail 테이블 구조를 지원합니다.";
        String version = "1.0";

      /*
          - interface-id: IF_DTD_003
            action-type: SENDER
            strategy: StandardDTDStrategy
            cron-expression:
               - 0/20 * * * * ?
            act-first: false
            on-signal-loop: false
            detail-data: true
            detail-names: CHILD
            send-system-code: A01
            receive-system-code: B01
            receiver-ids:
               - DTD_RECEIVER_01
      */

        String sendContextJson = " {\n" +
                "  \"interfaceId\": \"IF_DTD_003\",\n" +
                "  \"actionType\": \"SENDER\",\n" +
                "  \"strategy\": \"StandardDTDStrategy\",\n" +
                "  \"cronExpression\": [\n" +
                "    \"0/20 * * * * ?\"\n" +
                "  ],\n" +
                "  \"actFirst\": false,\n" +
                "  \"onSignalLoop\": false,\n" +
                "  \"detailData\": false,\n" +
                "  \"detailNames\": [\n" +
                "    \"CHILD\"\n" +
                "  ],\n" +
                "  \"sendSystemCode\": \"A01\",\n" +
                "  \"receiveSystemCode\": \"B01\",\n" +
                "  \"receiverIds\": [\n" +
                "    \"DTD_RECEIVER_01\"\n" +
                "  ]\n" +
                "}";

        /*
              - interface-id: IF_DTD_001
                action-type: RECEIVER
                strategy: StandardDTDStrategy
                procedure-call: true
         */

        String receiverContextJson = " {\n" +
                "  \"interfaceId\": \"IF_DTD_001\",\n" +
                "  \"actionType\": \"RECEIVER\",\n" +
                "  \"strategy\": \"StandardDTDStrategy\",\n" +
                "  \"procedureCall\": true\n" +
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
     * @return FieldsSchema
     */
    @NotNull
    private static FieldsSchema generateReceiverSchema() {
        FieldsSchema receiverSchema = new FieldsSchema();
        Set<String> requiredFieldsReceiver = Sets.newHashSet("interfaceId", "actionType", "strategy");
        receiverSchema.setRequiredFields(requiredFieldsReceiver);

        Set<String> fixedValueFieldsReceiver = Sets.newHashSet("actionType", "strategy");
        receiverSchema.setFixedValueFields(fixedValueFieldsReceiver);

        Map<String, String> fieldComment = Maps.newHashMap();
        fieldComment.put("interfaceId", "인터페이스 아이디");
        fieldComment.put("actionType", "액션 타입 (SENDER, RECEIVER)");
        fieldComment.put("strategy", "전략(패턴)");
        fieldComment.put("procedureCall", "프로시저 호출 여부(true, false)");
        receiverSchema.setFieldComment(fieldComment);

        Map<String, String> defaultValue = Maps.newHashMap();
        defaultValue.put("interfaceId", "empty");
        defaultValue.put("actionType", "empty");
        defaultValue.put("strategy", "empty");
        defaultValue.put("procedureCall", "false");
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
        Set<String> requiredFields = Sets.newHashSet("interfaceId", "actionType", "strategy", "cronExpression", "sendSystemCode", "receiveSystemCode", "receiverIds");
        senderSchema.setRequiredFields(requiredFields);

        Set<String> fixedValueFields = Sets.newHashSet("actionType", "strategy");
        senderSchema.setFixedValueFields(fixedValueFields);

        Map<String, String> fieldComment = Maps.newHashMap();
        fieldComment.put("interfaceId", "인터페이스 아이디");
        fieldComment.put("actionType", "액션 타입 (SENDER, RECEIVER)");
        fieldComment.put("strategy", "전략(패턴)");
        fieldComment.put("cronExpression", "스케줄링을 위한 크론 표현식");
        fieldComment.put("actFirst", "최초에 한번만 실행, 이때 크론 표현식은 무시(true, false)");
        fieldComment.put("onSignalLoop", "스케줄 실행후 데이터가 발생하면 한번더 수행 여부(true, false)");
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
        defaultValue.put("detailData", "false");
        defaultValue.put("detailNames", "empty");
        defaultValue.put("sendSystemCode", "empty");
        defaultValue.put("receiveSystemCode", "empty");
        defaultValue.put("receiverIds", "empty");

        senderSchema.setDefaultValue(defaultValue);
        return senderSchema;
    }
}
