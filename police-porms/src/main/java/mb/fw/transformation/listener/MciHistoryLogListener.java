package mb.fw.transformation.listener;

import lombok.extern.slf4j.Slf4j;
import mb.fw.net.product.entity.MciHistoryLogEntity;
import mb.fw.transformation.repository.MciHistoryLogRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.sql.DataSource;

@Slf4j
public class MciHistoryLogListener implements MessageListener , InitializingBean {

    @Autowired(required = false)
    NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    DataSource dataSource;

    MciHistoryLogRepository repository = null;
    PlatformTransactionManager transactionManager = null;
    @Override
    public void onMessage(Message message) {
        ObjectMessage objectMessage = null;
        if (message instanceof ObjectMessage) {
            objectMessage = (ObjectMessage) message;
        } else {
            log.info("Message is not an ObjectMessage");
            return;
        }

        MciHistoryLogEntity logData = null;
        try {
            logData = (MciHistoryLogEntity) objectMessage.getObject();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
        log.debug("rcv data :" + logData);
        repository.insertMciHistoryLog(logData , transactionManager);

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("jdbcTemplate : " + jdbcTemplate);
        log.info("dataSource : " + dataSource);
        repository  = new MciHistoryLogRepository(jdbcTemplate);
        transactionManager = new DataSourceTransactionManager(dataSource);
    }
}
