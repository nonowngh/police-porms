package mb.fw.atb.config;

import lombok.Data;

import java.util.Map;
import java.util.Set;


@Data
public class Specifications {

    //이름
    String name;
    //한글이름
    String nameKor;

    //버전
    String version;

    //설명
    String description;

    //송신자 json 설정 , 필드는 빠트림 없이 넣어야한다 (예제)
    String sender;
    //수신자 json 설정 , 필드는 빠트림 없이 넣어야한다 (예제)
    String receiver;

    FieldsSchema senderSchema;
    FieldsSchema receiverSchema;

}

