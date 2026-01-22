package mb.fw.atb.model.file;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FileMessage {
    List<FileInfo> fileInfoList;
    long count = 0;
    long errorCount = 0;
    boolean detailData = false;
    Map extendData;
}
