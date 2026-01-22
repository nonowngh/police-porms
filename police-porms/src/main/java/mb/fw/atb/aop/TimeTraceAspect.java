package mb.fw.atb.aop;

import com.google.common.collect.Lists;
import com.indigo.indigomq.memory.LRUMap;
import com.mb.indigo2.springsupport.AdaptorConfigBean;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.enums.THeader;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Aspect
public class TimeTraceAspect {


    public final static LRUMap<String, ArrayList<String>> LRU_MAP = new LRUMap<>(20000);

    @Pointcut("@annotation(mb.fw.atb.aop.TimeTrace)")
    private void timeTracePointcut() {
    }

    @Around("timeTracePointcut()")
    public Object traceTime(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        String txid = MDC.get(THeader.TRANSACTION_ID.key());
        String interfaceId = MDC.get(THeader.INTERFACE_ID.key());
        String adaptorName = MDC.get(THeader.ADAPTOR_NAME.key());
        try {

            Span currentSpan = Span.current();
            currentSpan.setAttribute("transactionId", txid);
            currentSpan.setAttribute("interfaceId", interfaceId);
            currentSpan.setAttribute("adaptorName", adaptorName);

            stopWatch.start();
            return joinPoint.proceed(); // 실제 타겟 호출
        } finally {
            stopWatch.stop();
//			MDC.put(THeader.ADAPTOR_NAME.key(), adaptorName);
//			MDC.put(THeader.TRANSACTION_ID.key(), txid);
//			MDC.put(THeader.INTERFACE_ID.key(), interfaceId);

            String logMsg = String.format("%s - Total time = %ss", joinPoint.getSignature().toShortString(), stopWatch.getTotalTimeSeconds());
            log.debug(logMsg);
            if (LRU_MAP.containsKey(txid)) {
                List list = LRU_MAP.get(txid);
                list.add(logMsg);
            } else {
                ArrayList newlist = Lists.newArrayList();
                newlist.add(logMsg);
                LRU_MAP.put(txid, newlist);
            }
        }
    }

    public static String generateTimeTraceAndRemove(String txid) {
        LRUMap<String, ArrayList<String>> timeTraceLruMap = TimeTraceAspect.LRU_MAP;
        if (timeTraceLruMap.containsKey(txid)) {
            StringBuilder timeTraceSB = new StringBuilder();
            ArrayList<String> timeTraceList = timeTraceLruMap.get(txid);
            for (String timeTrace : timeTraceList) {
                timeTraceSB.append(timeTrace).append("\n");
            }
            TimeTraceAspect.LRU_MAP.remove(txid);
            return timeTraceSB.toString();
        }
        return null;
    }
}