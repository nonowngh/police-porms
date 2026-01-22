package mb.fw.transformation.db;

import lombok.extern.slf4j.Slf4j;
import mb.fw.transformation.db.mapper.MciFieldInfoMapper;
import mb.fw.transformation.db.mapper.MciMessageInfoMapper;
import mb.fw.transformation.db.mapper.MciMessageMappingInfoMapper;
import mb.fw.transformation.db.model.MciFieldInfo;
import mb.fw.transformation.db.model.MciMessageInfo;
import mb.fw.transformation.db.model.MciMessageMappingInfo;
import mb.fw.transformation.form.*;
import mb.fw.transformation.loader.ExcelFileMessageFormBoxLoader;
import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.io.File;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

@Slf4j
public class DBTransduceUtil implements TransduceUtil {

    SqlSessionFactory sqlSessionFactory = null;

    public DBTransduceUtil(SqlSessionFactory sqlSessionFactory) throws Exception {
        MapperRegistry mr = sqlSessionFactory.getConfiguration().getMapperRegistry();

        if (!mr.hasMapper(MciFieldInfoMapper.class)) {
            mr.addMapper(MciFieldInfoMapper.class);
        }
        if (!mr.hasMapper(MciMessageInfoMapper.class)) {
            mr.addMapper(MciMessageInfoMapper.class);
        }
        if (!mr.hasMapper(MciMessageMappingInfoMapper.class)) {
            mr.addMapper(MciMessageMappingInfoMapper.class);
        }

        this.sqlSessionFactory = sqlSessionFactory;

    }

    public DBTransduceUtil(DataSource ds) throws Exception {

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("transducer", transactionFactory, ds);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(MciFieldInfoMapper.class);
        configuration.addMapper(MciMessageInfoMapper.class);
        configuration.addMapper(MciMessageMappingInfoMapper.class);
        this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);

    }

    @Override
    public void excelFileImport(File file) throws Exception {

        ExcelFileMessageFormBoxLoader loader = new ExcelFileMessageFormBoxLoader();
        loader.setFile(file);
        MessageFormBox box = loader.formload();
        Set<String> keySet = box.keySet();
        SqlSession session = sqlSessionFactory.openSession(false);

        MciMessageInfoMapper infoMapper = session.getMapper(MciMessageInfoMapper.class);
        MciMessageMappingInfoMapper mappingInfoMapper = session.getMapper(MciMessageMappingInfoMapper.class);
        MciFieldInfoMapper fieldMapper = session.getMapper(MciFieldInfoMapper.class);

        try {

            for (Iterator<String> iterator = keySet.iterator(); iterator.hasNext(); ) {

                String key = (String) iterator.next();

                MessageForm form = box.get(key);

                Infomation infomation = form.getInfomation();
                RecordContext inContext = form.getInContext();
                RecordContext outContext = form.getOutContext();

                log.debug("infomation : " + infomation);
                log.debug("inContext : " + inContext);
                log.debug("outContext : " + outContext);

                MciMessageInfo infoIn = new MciMessageInfo();
                MciMessageInfo infoOut = new MciMessageInfo();
                String srcMessageId = infomation.getTranCode();
                String trgMessageId = infomation.getChangeTranCode();

                // in
                infoIn.setGroupId(infomation.getAgentName());
                infoIn.setMessageId(srcMessageId);
                infoIn.setInterfaceId(infomation.getReserve());
                infoIn.setMessageAttr(infomation.getTranDirection());
                infoIn.setMessageName(infomation.getTranComment());
                infoIn.setMessageType(infomation.getInType());
                infoMapper.insert(infoIn);

                // out
                if (trgMessageId != null && !trgMessageId.equals("")) {
                    infoOut.setGroupId(infomation.getAgentName());
                    infoOut.setMessageId(trgMessageId);
                    infoOut.setInterfaceId(infomation.getReserve());
                    infoOut.setMessageAttr(infomation.getTranDirection());
                    infoOut.setMessageName(infomation.getTranComment());
                    infoOut.setMessageType(infomation.getOutType());
                    infoMapper.insert(infoOut);
                }

                MciMessageMappingInfo mappingInfo = new MciMessageMappingInfo();

                mappingInfo.setGroupId(infomation.getAgentName());
                mappingInfo.setSrcMessageId(srcMessageId);
                mappingInfo.setTrgMessageId(trgMessageId);
                mappingInfo.setInterfaceId(infomation.getReserve());

                log.debug("mappingInfo insert : " + mappingInfo);

                mappingInfoMapper.insert(mappingInfo);

                for (int i = 1; i <= inContext.size(); i++) {
                    Record ctxRecord = inContext.getRecord(i);
                    MciFieldInfo fieldInfo = new MciFieldInfo();

                    // PK
                    fieldInfo.setGroupId(infoIn.getGroupId());
                    fieldInfo.setInterfaceId(infoIn.getInterfaceId());
                    fieldInfo.setMessageId(infoIn.getMessageId());
                    fieldInfo.setSeq(ctxRecord.getNo());
                    fieldInfo.setFieldId(ctxRecord.getName());
                    fieldInfo.setFieldName(ctxRecord.getNameKor());
                    fieldInfo.setFieldType(ctxRecord.getType());
                    fieldInfo.setFieldLength("" + ctxRecord.getLength());
                    fieldInfo.setChildCount(ctxRecord.getChildCount());
//					fieldInfo.setSrcSeq(Integer.parseInt(ctxRecord.getRank()));
                    fieldInfo.setMappingInfo(ctxRecord.getFunction());
                    fieldInfo.setDefaultValue(ctxRecord.getDefaultValue());
                    fieldInfo.setCountField(ctxRecord.getCountNo());

                    int resultInt = fieldMapper.insert(fieldInfo);

                    log.debug(fieldInfo.getSeq() + " input 입력 결과 " + resultInt);
                }

                for (int i = 1; i <= outContext.size(); i++) {
                    Record ctxRecord = outContext.getRecord(i);
                    MciFieldInfo fieldInfo = new MciFieldInfo();

                    // PK
                    fieldInfo.setGroupId(infoOut.getGroupId());
                    fieldInfo.setInterfaceId(infoOut.getInterfaceId());
                    fieldInfo.setMessageId(infoOut.getMessageId());
                    fieldInfo.setSeq(ctxRecord.getNo());
                    fieldInfo.setFieldId(ctxRecord.getName());
                    fieldInfo.setFieldName(ctxRecord.getNameKor());
                    fieldInfo.setFieldType(ctxRecord.getType());
                    fieldInfo.setFieldLength("" + ctxRecord.getLength());
                    fieldInfo.setChildCount(ctxRecord.getChildCount());
                    fieldInfo.setSrcSeq(Integer.parseInt(ctxRecord.getRank()));
                    fieldInfo.setMappingInfo(ctxRecord.getFunction());
                    fieldInfo.setDefaultValue(ctxRecord.getDefaultValue());
                    fieldInfo.setCountField(ctxRecord.getCountNo());

                    int resultInt = fieldMapper.insert(fieldInfo);

                    log.debug(fieldInfo.getSeq() + " input 입력 결과 " + resultInt);
                }
            }
            session.commit();
        } catch (Exception e) {
            log.error("upload fail : ", e);
            log.debug("rollback");
            session.rollback();
            throw e;
        } finally {
            session.close();
        }
    }

    public static void main(String[] args) {

        Random rd = new Random();

        int max = 0;

        for (int i = 0; i < 1000000000; i++) {
            int value = rd.nextInt(9000) + 1000;
            if (max < value) {
                max = value;
                System.out.println(max);
            }
        }
    }
}
