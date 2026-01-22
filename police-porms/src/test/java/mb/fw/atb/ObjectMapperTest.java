package mb.fw.atb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mb.fw.atb.model.data.DBMessage;

public class ObjectMapperTest {
    public static void main(String[] args) throws JsonProcessingException {

        String ss = "{\n" +
                "\"dataList\" : [ ],\n" +
                "  \"count\" : 0,\n" +
                "  \"extendData\" : null\n" +
                "}";
        ObjectMapper objectMapper = new ObjectMapper();
        DBMessage dbMessage = objectMapper.readValue(ss, DBMessage.class);
    }
}
