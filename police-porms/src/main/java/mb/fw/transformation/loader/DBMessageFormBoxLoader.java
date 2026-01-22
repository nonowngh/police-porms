package mb.fw.transformation.loader;

import mb.fw.transformation.db.mapper.*;
import mb.fw.transformation.db.model.MciFieldInfo;
import mb.fw.transformation.db.model.MciMessageInfo;
import mb.fw.transformation.db.model.MciMessageMappingInfo;
import mb.fw.transformation.form.*;
import mb.fw.transformation.util.Default;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Iterator;
import java.util.List;

import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

/**
 * @author clupine
 */
public class DBMessageFormBoxLoader implements MessageFormBoxLoader {

    private static Logger logger = LoggerFactory.getLogger(DBMessageFormBoxLoader.class);

    SqlSessionFactory sqlSessionFactory;

    String groupId;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

    public DBMessageFormBoxLoader(DataSource dataSource) {
        // TransactionFactory transactionFactory = new JdbcTransactionFactory();
        ManagedTransactionFactory transactionFactory = new ManagedTransactionFactory();
        Environment environment = new Environment("transducer", transactionFactory, dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(MciMessageInfoMapper.class);
        configuration.addMapper(MciFieldInfoMapper.class);
        configuration.addMapper(MciMessageMappingInfoMapper.class);
        this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    public DBMessageFormBoxLoader(SqlSessionFactory sqlSessionFactory) {
        if (!sqlSessionFactory.getConfiguration().getMapperRegistry().hasMapper(MciMessageInfoMapper.class)) {
            sqlSessionFactory.getConfiguration().addMapper(MciMessageInfoMapper.class);
        }
        if (!sqlSessionFactory.getConfiguration().getMapperRegistry().hasMapper(MciFieldInfoMapper.class)) {
            sqlSessionFactory.getConfiguration().addMapper(MciFieldInfoMapper.class);
        }
        if (!sqlSessionFactory.getConfiguration().getMapperRegistry().hasMapper(MciMessageMappingInfoMapper.class)) {
            sqlSessionFactory.getConfiguration().addMapper(MciMessageMappingInfoMapper.class);
        }
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public MessageFormBox formload() {

        SqlSession session = sqlSessionFactory.openSession(false);

        MciMessageInfoMapper messageInfo = session.getMapper(MciMessageInfoMapper.class);
        MciFieldInfoMapper fieldInfo = session.getMapper(MciFieldInfoMapper.class);
        MciMessageMappingInfoMapper mappingInfo = session.getMapper(MciMessageMappingInfoMapper.class);

        List<MciMessageMappingInfo> mapplingInfoList = mappingInfo.select(c -> {
            c.where(MciMessageMappingInfoDynamicSqlSupport.groupId, isEqualTo(groupId));
            return c;
        });

//		System.out.println("mapplingInfoList : " + mapplingInfoList.size());
        MessageFormBox box = new MessageFormBox();

        try {
            for (Iterator<MciMessageMappingInfo> iterator = mapplingInfoList.iterator(); iterator.hasNext(); ) {

                MciMessageMappingInfo mciMessageInfo = iterator.next();

                String srcMessageId = mciMessageInfo.getSrcMessageId();

                MciMessageInfo srcMessageInfo = messageInfo.selectOne(c -> {
                    c.where(MciMessageInfoDynamicSqlSupport.groupId, isEqualTo(groupId))
                            .and(MciMessageInfoDynamicSqlSupport.interfaceId, isEqualTo(mciMessageInfo.getInterfaceId()))
                            .and(MciMessageInfoDynamicSqlSupport.messageId, isEqualTo(srcMessageId));
                    return c;
                });

                logger.info("srcMessageInfo : " + srcMessageInfo);

                String trgMessageId = mciMessageInfo.getTrgMessageId();
                MciMessageInfo trgMessageInfo = messageInfo.selectOne(c -> {
                    c.where(MciMessageInfoDynamicSqlSupport.groupId, isEqualTo(groupId))
                            .and(MciMessageInfoDynamicSqlSupport.interfaceId, isEqualTo(mciMessageInfo.getInterfaceId()))
                            .and(MciMessageInfoDynamicSqlSupport.messageId, isEqualTo(trgMessageId));
                    return c;
                });

                logger.info("trgMessageInfo : " + trgMessageInfo);

                Infomation infomation = new Infomation();
                MessageForm form = new MessageForm();

                infomation.setInType(srcMessageInfo.getMessageType());
                infomation.setTranCode(srcMessageInfo.getMessageId());
                infomation.setOutType(trgMessageInfo.getMessageType());
                infomation.setAgentName(groupId);
                infomation.setTranDirection(srcMessageInfo.getMessageAttr());
                infomation.setServiceName(srcMessageInfo.getMessageName());
                form.setInfomation(infomation);

                List<MciFieldInfo> srcFieldList = fieldInfo.select(c -> {
                    c.where(MciFieldInfoDynamicSqlSupport.groupId, isEqualTo(groupId))
                            .and(MciFieldInfoDynamicSqlSupport.interfaceId, isEqualTo(mciMessageInfo.getInterfaceId()))
                            .and(MciFieldInfoDynamicSqlSupport.messageId, isEqualTo(srcMessageId));
                    return c;
                });
                logger.info("srcMciFieldInfo : " + srcFieldList);

                List<MciFieldInfo> trgFieldList = fieldInfo.select(c -> {
                    c.where(MciFieldInfoDynamicSqlSupport.groupId, isEqualTo(groupId))
                            .and(MciFieldInfoDynamicSqlSupport.interfaceId, isEqualTo(mciMessageInfo.getInterfaceId()))
                            .and(MciFieldInfoDynamicSqlSupport.messageId, isEqualTo(trgMessageId));
                    return c;
                });

                logger.info("trgMciFieldInfo : " + trgFieldList);
                RecordContext inContext = new RecordContext();
                RecordContext outContext = new RecordContext();

                for (MciFieldInfo mciFieldInfo : srcFieldList) {
                    Record record = new Record();
                    record.setAgentName(groupId);
                    record.setNo(mciFieldInfo.getSeq());
                    record.setName(mciFieldInfo.getFieldId());
                    record.setLength(Integer.valueOf(mciFieldInfo.getFieldLength()));
                    record.setType(mciFieldInfo.getFieldType());
                    record.setRank(String.valueOf(mciFieldInfo.getSrcSeq()));
                    record.setNameKor(mciFieldInfo.getFieldName());
                    record.setChildCount(Default.toInt(mciFieldInfo.getChildCount(), 0));
                    record.setCountNo(Default.toInt(mciFieldInfo.getCountField(), 0));
                    record.setDefaultValue(mciFieldInfo.getDefaultValue());
                    record.setFunction(mciFieldInfo.getMappingInfo());
                    inContext.put(record.getNo(), record);
                }

                for (MciFieldInfo mciFieldInfo : trgFieldList) {
                    Record record = new Record();
                    record.setAgentName(groupId);
                    record.setNo(mciFieldInfo.getSeq());
                    record.setName(mciFieldInfo.getFieldId());
                    record.setLength(Integer.valueOf(mciFieldInfo.getFieldLength()));
                    record.setType(mciFieldInfo.getFieldType());
                    record.setRank(String.valueOf(mciFieldInfo.getSrcSeq()));
                    record.setNameKor(mciFieldInfo.getFieldName());
                    record.setChildCount(Default.toInt(mciFieldInfo.getChildCount(), 0));
                    record.setCountNo(Default.toInt(mciFieldInfo.getCountField(), 0));
                    record.setDefaultValue(mciFieldInfo.getDefaultValue());
                    record.setFunction(mciFieldInfo.getMappingInfo());
                    outContext.put(record.getNo(), record);
                }

                form.setInContext(inContext);
                form.setOutContext(outContext);
                box.put(form);
                logger.debug("put " + form.getTransCode());
            }
        } finally {
            if (session != null)
                session.close();
        }

        return box;
    }

}
