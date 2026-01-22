package mb.fw.atb.config.sub;

import lombok.Data;

import java.util.List;

@Data
public class FTIInfo {
    String RequesterCode;
    //제공기관
    List<String> ProviderCodeList;
}
