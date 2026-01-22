package mb.fw.atb.model.data;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DBMessage {
    List<Map<String, Object>> dataList;

    boolean detailData = false;
    List<Map<String, DetailData>> detailList;
    long count = 0;
    Map extendData;
}
