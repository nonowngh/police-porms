package mb.fw.atb.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class OnSignalInfo {
    long count;
    Object sendObject;
    boolean processEnd;

    @Override
    public String toString() {
        return "OnSignalInfo{" +
                "count=" + count +
                ", sendObject=" + sendObject.getClass().getName() +
                ", processEnd=" + processEnd +
                '}';
    }
}
