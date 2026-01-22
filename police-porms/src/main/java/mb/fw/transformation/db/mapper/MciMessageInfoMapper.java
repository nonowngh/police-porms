package mb.fw.transformation.db.mapper;

import mb.fw.transformation.db.model.MciMessageInfo;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.delete.DeleteDSLCompleter;
import org.mybatis.dynamic.sql.delete.render.DeleteStatementProvider;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.insert.render.MultiRowInsertStatementProvider;
import org.mybatis.dynamic.sql.select.CountDSLCompleter;
import org.mybatis.dynamic.sql.select.SelectDSLCompleter;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.mybatis.dynamic.sql.update.UpdateDSLCompleter;
import org.mybatis.dynamic.sql.update.UpdateModel;
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;
import org.mybatis.dynamic.sql.util.mybatis3.MyBatis3Utils;

import javax.annotation.Generated;
import java.util.Collection;
import java.util.List;

import static mb.fw.transformation.db.mapper.MciMessageInfoDynamicSqlSupport.*;

@Mapper
public interface MciMessageInfoMapper {
    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.302+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    BasicColumn[] selectList = BasicColumn.columnList(groupId, messageId, messageName, interfaceId, creationDate, modifiedDate, constructorId, modifierId, usageStatus, startUseDate, endUseDate, description, messageVersion, messageType, messageAttr);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.301+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.301+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    @DeleteProvider(type = SqlProviderAdapter.class, method = "delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.301+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    @InsertProvider(type = SqlProviderAdapter.class, method = "insert")
    int insert(InsertStatementProvider<MciMessageInfo> insertStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.301+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    @InsertProvider(type = SqlProviderAdapter.class, method = "insertMultiple")
    int insertMultiple(MultiRowInsertStatementProvider<MciMessageInfo> multipleInsertStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.301+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @ResultMap("MciMessageInfoResult")
    MciMessageInfo selectOne(SelectStatementProvider selectStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.301+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @Results(id = "MciMessageInfoResult", value = {
            @Result(column = "GROUP_ID", property = "groupId", jdbcType = JdbcType.VARCHAR),
            @Result(column = "MESSAGE_ID", property = "messageId", jdbcType = JdbcType.VARCHAR),
            @Result(column = "MESSAGE_NAME", property = "messageName", jdbcType = JdbcType.VARCHAR),
            @Result(column = "INTERFACE_ID", property = "interfaceId", jdbcType = JdbcType.VARCHAR),
            @Result(column = "CREATION_DATE", property = "creationDate", jdbcType = JdbcType.TIMESTAMP),
            @Result(column = "MODIFIED_DATE", property = "modifiedDate", jdbcType = JdbcType.TIMESTAMP),
            @Result(column = "CONSTRUCTOR_ID", property = "constructorId", jdbcType = JdbcType.VARCHAR),
            @Result(column = "MODIFIER_ID", property = "modifierId", jdbcType = JdbcType.VARCHAR),
            @Result(column = "USAGE_STATUS", property = "usageStatus", jdbcType = JdbcType.VARCHAR),
            @Result(column = "START_USE_DATE", property = "startUseDate", jdbcType = JdbcType.DATE),
            @Result(column = "END_USE_DATE", property = "endUseDate", jdbcType = JdbcType.DATE),
            @Result(column = "DESCRIPTION", property = "description", jdbcType = JdbcType.VARCHAR),
            @Result(column = "MESSAGE_VERSION", property = "messageVersion", jdbcType = JdbcType.VARCHAR),
            @Result(column = "MESSAGE_TYPE", property = "messageType", jdbcType = JdbcType.VARCHAR),
            @Result(column = "MESSAGE_ATTR", property = "messageAttr", jdbcType = JdbcType.VARCHAR)
    })
    List<MciMessageInfo> selectMany(SelectStatementProvider selectStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.302+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    @UpdateProvider(type = SqlProviderAdapter.class, method = "update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.302+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, mciMessageInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.302+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, mciMessageInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.302+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    default int insert(MciMessageInfo record) {
        return MyBatis3Utils.insert(this::insert, record, mciMessageInfo, c ->
                c.map(groupId).toProperty("groupId")
                        .map(messageId).toProperty("messageId")
                        .map(messageName).toProperty("messageName")
                        .map(interfaceId).toProperty("interfaceId")
                        .map(creationDate).toProperty("creationDate")
                        .map(modifiedDate).toProperty("modifiedDate")
                        .map(constructorId).toProperty("constructorId")
                        .map(modifierId).toProperty("modifierId")
                        .map(usageStatus).toProperty("usageStatus")
                        .map(startUseDate).toProperty("startUseDate")
                        .map(endUseDate).toProperty("endUseDate")
                        .map(description).toProperty("description")
                        .map(messageVersion).toProperty("messageVersion")
                        .map(messageType).toProperty("messageType")
                        .map(messageAttr).toProperty("messageAttr")
        );
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.302+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    default int insertMultiple(Collection<MciMessageInfo> records) {
        return MyBatis3Utils.insertMultiple(this::insertMultiple, records, mciMessageInfo, c ->
                c.map(groupId).toProperty("groupId")
                        .map(messageId).toProperty("messageId")
                        .map(messageName).toProperty("messageName")
                        .map(interfaceId).toProperty("interfaceId")
                        .map(creationDate).toProperty("creationDate")
                        .map(modifiedDate).toProperty("modifiedDate")
                        .map(constructorId).toProperty("constructorId")
                        .map(modifierId).toProperty("modifierId")
                        .map(usageStatus).toProperty("usageStatus")
                        .map(startUseDate).toProperty("startUseDate")
                        .map(endUseDate).toProperty("endUseDate")
                        .map(description).toProperty("description")
                        .map(messageVersion).toProperty("messageVersion")
                        .map(messageType).toProperty("messageType")
                        .map(messageAttr).toProperty("messageAttr")
        );
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.302+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    default int insertSelective(MciMessageInfo record) {
        return MyBatis3Utils.insert(this::insert, record, mciMessageInfo, c ->
                c.map(groupId).toPropertyWhenPresent("groupId", record::getGroupId)
                        .map(messageId).toPropertyWhenPresent("messageId", record::getMessageId)
                        .map(messageName).toPropertyWhenPresent("messageName", record::getMessageName)
                        .map(interfaceId).toPropertyWhenPresent("interfaceId", record::getInterfaceId)
                        .map(creationDate).toPropertyWhenPresent("creationDate", record::getCreationDate)
                        .map(modifiedDate).toPropertyWhenPresent("modifiedDate", record::getModifiedDate)
                        .map(constructorId).toPropertyWhenPresent("constructorId", record::getConstructorId)
                        .map(modifierId).toPropertyWhenPresent("modifierId", record::getModifierId)
                        .map(usageStatus).toPropertyWhenPresent("usageStatus", record::getUsageStatus)
                        .map(startUseDate).toPropertyWhenPresent("startUseDate", record::getStartUseDate)
                        .map(endUseDate).toPropertyWhenPresent("endUseDate", record::getEndUseDate)
                        .map(description).toPropertyWhenPresent("description", record::getDescription)
                        .map(messageVersion).toPropertyWhenPresent("messageVersion", record::getMessageVersion)
                        .map(messageType).toPropertyWhenPresent("messageType", record::getMessageType)
                        .map(messageAttr).toPropertyWhenPresent("messageAttr", record::getMessageAttr)
        );
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.302+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    default MciMessageInfo selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, mciMessageInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.302+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    default List<MciMessageInfo> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, mciMessageInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.302+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    default List<MciMessageInfo> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, mciMessageInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.302+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, mciMessageInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.302+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    static UpdateDSL<UpdateModel> updateAllColumns(MciMessageInfo record, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(groupId).equalTo(record::getGroupId)
                .set(messageId).equalTo(record::getMessageId)
                .set(messageName).equalTo(record::getMessageName)
                .set(interfaceId).equalTo(record::getInterfaceId)
                .set(creationDate).equalTo(record::getCreationDate)
                .set(modifiedDate).equalTo(record::getModifiedDate)
                .set(constructorId).equalTo(record::getConstructorId)
                .set(modifierId).equalTo(record::getModifierId)
                .set(usageStatus).equalTo(record::getUsageStatus)
                .set(startUseDate).equalTo(record::getStartUseDate)
                .set(endUseDate).equalTo(record::getEndUseDate)
                .set(description).equalTo(record::getDescription)
                .set(messageVersion).equalTo(record::getMessageVersion)
                .set(messageType).equalTo(record::getMessageType)
                .set(messageAttr).equalTo(record::getMessageAttr);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.302+09:00", comments = "Source Table: MCI_MESSAGE_INFO")
    static UpdateDSL<UpdateModel> updateSelectiveColumns(MciMessageInfo record, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(groupId).equalToWhenPresent(record::getGroupId)
                .set(messageId).equalToWhenPresent(record::getMessageId)
                .set(messageName).equalToWhenPresent(record::getMessageName)
                .set(interfaceId).equalToWhenPresent(record::getInterfaceId)
                .set(creationDate).equalToWhenPresent(record::getCreationDate)
                .set(modifiedDate).equalToWhenPresent(record::getModifiedDate)
                .set(constructorId).equalToWhenPresent(record::getConstructorId)
                .set(modifierId).equalToWhenPresent(record::getModifierId)
                .set(usageStatus).equalToWhenPresent(record::getUsageStatus)
                .set(startUseDate).equalToWhenPresent(record::getStartUseDate)
                .set(endUseDate).equalToWhenPresent(record::getEndUseDate)
                .set(description).equalToWhenPresent(record::getDescription)
                .set(messageVersion).equalToWhenPresent(record::getMessageVersion)
                .set(messageType).equalToWhenPresent(record::getMessageType)
                .set(messageAttr).equalToWhenPresent(record::getMessageAttr);
    }
}