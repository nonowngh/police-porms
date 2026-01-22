package mb.fw.atb.model.data;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DetailData {
   String name;
   int size;
   List<Map<String, Object>> dataList;
}
