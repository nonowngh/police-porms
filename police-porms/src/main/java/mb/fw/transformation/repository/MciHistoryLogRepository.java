package mb.fw.transformation.repository;

import lombok.extern.slf4j.Slf4j;
import mb.fw.net.product.entity.MciHistoryLogEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
public class MciHistoryLogRepository {

    NamedParameterJdbcTemplate jdbcTemplate;

    String sqlLog = "INSERT INTO MCI_HISTORY_LOG\r\n"
            + "(INTERFACE_ID, TRANSACTION_ID, ADAPTOR, DIRECTION ,PATTERN, SRC_ORG, TRG_ORG, RESULT_CD, RESULT_MSG, RESULT_DT, REF_INTERFACE_ID, REF_TRANSACTION_ID , REF_GROUP_ID )\r\n"
            + "VALUES(:interfaceId, :transactionId, :adaptor, :direction, :pattern, :srcOrg, :trgOrg, :resultCd, :resultMsg, :resultDt, :refInterfaceId, :refTransactionId , :groupId)";
    String sqlData = "INSERT INTO MCI_HISTORY_DATA (TRANSACTION_ID, DIRECTION, MSG_DATA) VALUES(:transactionId, :direction, :msgData)";
    String sqlFile = "INSERT INTO MCI_HISTORY_FILE (TRANSACTION_ID, FILE_NO, FILE_NAME)  VALUES(:transactionId, :fileNo,    :fileName)";

    public MciHistoryLogRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertMciHistoryLog(MciHistoryLogEntity logdata, PlatformTransactionManager transactionManager) {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                BeanPropertySqlParameterSource parameter = new BeanPropertySqlParameterSource(logdata);
                jdbcTemplate.update(sqlLog, parameter);
                if (logdata.getMsgData() != null) {
                    jdbcTemplate.update(sqlData, parameter);
                }
                if (!StringUtils.isEmpty(logdata.getFileName())) {
                    String fileNames = logdata.getFileName();
                    String[] fileName = fileNames.split(",", -1);
                    for (int i = 0; i < fileName.length; i++) {
                        logdata.setFileNo(i + 1);
                        logdata.setFileName(fileName[i]);
                        jdbcTemplate.update(sqlFile, parameter);
                    }
                }
            }
        });
    }

}
