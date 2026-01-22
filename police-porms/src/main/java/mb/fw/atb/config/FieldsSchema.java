package mb.fw.atb.config;

import lombok.Data;

import java.util.Map;
import java.util.Set;


@Data
public class FieldsSchema {

    //필수가 되는 필드들(반드시 있어야하는 필드)
    Set<String> requiredFields;

    //고정값이 되는 필드들 (수정 불가)
    Set<String> fixedValueFields;

    //필드의 필드에 대한 설명 (어떤값이 들어가야하는지?)
    Map<String, String> fieldComment;

    //입력을 안했을경우 기본값
    Map<String, String> defaultValue;

    
}

