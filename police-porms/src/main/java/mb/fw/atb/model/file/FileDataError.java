package mb.fw.atb.model.file;

import lombok.Data;
import net.sf.flatpack.DataError;

@Data
public class FileDataError {
    private String errorDesc;
    private int lineNo;
    private int errorLevel;
    private String rawData;
    private String lastColumnName;
    private String lastColumnValue;


    public void parseDataError(DataError de) {
        errorDesc = de.getErrorDesc();
        lineNo = de.getLineNo();
        errorLevel = de.getErrorLevel();
        rawData = de.getRawData();
        lastColumnName = de.getLastColumnName();
        lastColumnValue = de.getLastColumnValue();
    }
}