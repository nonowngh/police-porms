package mb.fw.atb.model.file;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
public class FileInfo {
    String fileName;
    long size;
    long parseErrorCount;
    List<FileDataError> errors;

}