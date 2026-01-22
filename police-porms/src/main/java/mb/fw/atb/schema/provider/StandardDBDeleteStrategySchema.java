package mb.fw.atb.schema.provider;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import mb.fw.atb.config.FieldsSchema;
import mb.fw.atb.config.Specifications;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class StandardDBDeleteStrategySchema {

    /**
     * 설정 완료 2024-11-07
     *
     * @return
     */
    public static Specifications specifications() {
        String name = "DB Delete";
        String nameKor = "DB 삭제";
        String description = "DB에 데이터를 삭제하는 전략입니다.";
        String version = "1.0";

      /*
      - interfaceId: IF_REST_DB_04
        actionType: CUSTOMIZE
        strategy: StandardDBDeleteStrategy
        send-system-code: A01
        receive-system-code: B01
        receiver-ids: REST_PROVIDER_DB_01

        camelCase로 변경
      */

        String sendContextJson = " {\n" +
                "  \"interfaceId\": \"IF_REST_DB_04\",\n" +
                "  \"actionType\": \"CUSTOMIZE\",\n" +
                "  \"strategy\": \"StandardDBDeleteStrategy\",\n" +
                "  \"sendSystemCode\": \"A01\",\n" +
                "  \"receiveSystemCode\": \"B01\",\n" +
                "  \"receiverIds\": [\n" +
                "    \"DTD_RECEIVER_01\"\n" +
                "  ]\n" +
                "}";

       /*
      - interfaceId: IF_REST_DB_04
        actionType: RECEIVER
        strategy: StandardDBDeleteStrategy
        send-system-code: A01
        receive-system-code: B01

                camelCase로 변경
        */
        String receiverContextJson = " {\n" +
                "  \"interfaceId\": \"IF_REST_DB_04\",\n" +
                "  \"actionType\": \"RECEIVER\",\n" +
                "  \"strategy\": \"StandardDBDeleteStrategy\",\n" +
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
        specifications.setReceiver(receiverContextJson);

        //송신자 스키마
        FieldsSchema senderSchema = generateSchema();
        specifications.setSenderSchema(senderSchema);

        //수신자 스키마
        FieldsSchema receiverSchema = generateSchema();
        specifications.setReceiverSchema(receiverSchema);

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
        - interfaceId: IF_REST_DB_01
        actionType: CUSTOMIZE
        strategy: StandardDBInsertStrategy
        send-system-code: A01
        receive-system-code: B01
      */

        FieldsSchema senderSchema = new FieldsSchema();
        Set<String> requiredFields = Sets.newHashSet("interfaceId", "actionType", "strategy", "sendSystemCode", "receiveSystemCode");
        senderSchema.setRequiredFields(requiredFields);

        Set<String> fixedValueFields = Sets.newHashSet("actionType");
        senderSchema.setFixedValueFields(fixedValueFields);

        Map<String, String> fieldComment = Maps.newHashMap();
        fieldComment.put("interfaceId", "인터페이스 아이디");
        fieldComment.put("actionType", "액션 타입 (CUSTOMIZE)");
        fieldComment.put("strategy", "전략(패턴)");
        fieldComment.put("sendSystemCode", "송신 시스템 코드");
        fieldComment.put("receiveSystemCode", "수신 시스템 코드");

        senderSchema.setFieldComment(fieldComment);

        Map<String, String> defaultValue = Maps.newHashMap();
        defaultValue.put("interfaceId", "empty");
        defaultValue.put("actionType", "empty");
        defaultValue.put("strategy", "empty");
        defaultValue.put("sendSystemCode", "empty");
        defaultValue.put("receiveSystemCode", "empty");

        senderSchema.setDefaultValue(defaultValue);
        return senderSchema;
    }
}
