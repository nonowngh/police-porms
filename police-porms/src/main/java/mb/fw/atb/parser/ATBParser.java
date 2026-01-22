package mb.fw.atb.parser;

import java.util.Map;

public interface ATBParser {

    public byte[] MapToBytes(Map map) throws Exception;

    public Map MapToMap(Map map) throws Exception;

    public Map BytesToMap(byte[] bytes) throws Exception;


}
