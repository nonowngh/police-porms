package mb.fw.atb.model.ui;

import lombok.Data;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.config.sub.Iftp;

import java.util.ArrayList;
import java.util.List;

/**
 * 어뎁터 설정 정보를 가져오는데 개별 설정이 많을거같다 여러개로 나누자
 */
@Data
public class ATBModel {
    List<IFContext> ifContext = new ArrayList<>();
    Iftp iftp;
}