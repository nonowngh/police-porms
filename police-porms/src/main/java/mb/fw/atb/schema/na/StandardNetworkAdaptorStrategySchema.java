package mb.fw.atb.schema.na;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import mb.fw.atb.config.FieldsSchema;
import mb.fw.atb.config.Specifications;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class StandardNetworkAdaptorStrategySchema {

    /**
     * 설정 완료 2024-11-07
     *
     * @return
     */
    public static Specifications specifications() {
        String name = "TCP/IP Pattern (Provider to TCP/IP)";
        String nameKor = "TCP/IP 패턴 (Provider to TCP/IP)";
        String description = "Rest Provider 통하여 Json 데이터를 TCP/IP로 송신하는 전략입니다. 수신은 Http Rest API로 호출합니다.";
        String version = "1.0";

//        String contextJson = " {\n" +
//                "  \"interfaceId\": \"IF_TCP_S_001\",\n" +
//                "  \"actionType\": \"CUSTOMIZE\",\n" +
//                "  \"strategy\": \"StandardNetworkAdaptorStrategy\",\n" +
//                "  \"process-mode\": \"SYNC\",\n" +
//                "  \"out-mapper-type\": \"PARSER\",\n" +
//                "  \"in-mapper-type\": \"PARSER\",\n" +
//                "  \"parser-id\": \"SHineSCIParser\",\n" +
//                "  \"send-system-code\": \"A01\",\n" +
//                "  \"receive-system-code\": \"SCI\",\n" +
//                "  \"receiver-ids\": [\"SGI_TCP\"]\n" +
//                "}";

        //CamelCase로 변경
        String contextJson = " {\n" +
                "  \"interfaceId\": \"IF_TCP_S_001\",\n" +
                "  \"actionType\": \"CUSTOMIZE\",\n" +
                "  \"strategy\": \"StandardNetworkAdaptorStrategy\",\n" +
                "  \"processMode\": \"SYNC\",\n" +
                "  \"outMapperType\": \"PARSER\",\n" +
                "  \"inMapperType\": \"PARSER\",\n" +
                "  \"parserId\": \"SHineSCIParser\",\n" +
                "  \"sendSystemCode\": \"A01\",\n" +
                "  \"receiveSystemCode\": \"SCI\",\n" +
                "  \"receiverIds\": [\"SGI_TCP\"]\n" +
                "}";


        //스펙 생성
        Specifications specifications = new Specifications();
        specifications.setName(name);
        specifications.setNameKor(nameKor);
        specifications.setVersion(version);
        specifications.setDescription(description);
        specifications.setSender(contextJson);
        specifications.setReceiver(contextJson);

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
              - interfaceId: IF_TCP_S_001
                actionType: CUSTOMIZE
                strategy: StandardNetworkAdaptorStrategy
                process-mode: SYNC
                out-mapper-type: PARSER
                in-mapper-type: PARSER
                parser-id: SHineSCIParser
                send-system-code: A01
                receive-system-code: SCI
                receiver-ids: SGI_TCP
         */

        FieldsSchema senderSchema = new FieldsSchema();
        Set<String> requiredFields = Sets.newHashSet("interfaceId", "actionType", "sendSystemCode", "receiveSystemCode", "receiverIds");
        senderSchema.setRequiredFields(requiredFields);

        Set<String> fixedValueFields = Sets.newHashSet("actionType");
        senderSchema.setFixedValueFields(fixedValueFields);

        Map<String, String> fieldComment = Maps.newHashMap();
        fieldComment.put("interfaceId", "인터페이스 ID");
        fieldComment.put("actionType", "동작 유형 (CUSTOMIZE)");
        fieldComment.put("strategy", "전략(패턴)");
        fieldComment.put("processMode", "처리 방식 (SYNC, ASYNC)");
        fieldComment.put("outMapperType", "송신 맵핑 유형");
        fieldComment.put("inMapperType", "수신 맵핑 유형");
        fieldComment.put("parserId", "파서 콤퍼넌트 ID");
        fieldComment.put("sendSystemCode", "송신 시스템 코드");
        fieldComment.put("receiveSystemCode", "수신 시스템 코드");
        fieldComment.put("receiverIds", "수신자 ID 목록");
        senderSchema.setFieldComment(fieldComment);

        Map<String, String> defaultValue = Maps.newHashMap();
        defaultValue.put("interfaceId", "empty");
        defaultValue.put("actionType", "empty");
        defaultValue.put("strategy", "empty");
        defaultValue.put("processMode", "empty");
        defaultValue.put("outMapperType", "empty");
        defaultValue.put("inMapperType", "empty");
        defaultValue.put("parserId", "empty");
        defaultValue.put("sendSystemCode", "empty");
        defaultValue.put("receiveSystemCode", "empty");
        defaultValue.put("receiverIds", "empty");

        senderSchema.setDefaultValue(defaultValue);
        return senderSchema;
    }
}
