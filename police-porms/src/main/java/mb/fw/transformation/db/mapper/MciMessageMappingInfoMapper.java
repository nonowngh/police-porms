package mb.fw.transformation.db.mapper;

import mb.fw.transformation.db.model.MciMessageMappingInfo;
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
import java.util.Optional;

import static mb.fw.transformation.db.mapper.MciMessageMappingInfoDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

@Mapper
public interface MciMessageMappingInfoMapper {
    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    BasicColumn[] selectList = BasicColumn.columnList(groupId, interfaceId, srcMessageId, trgMessageId, creationDate, modifiedDate, constructorId, modifierId, usageStatus, startUseDate, endUseDate);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    @DeleteProvider(type = SqlProviderAdapter.class, method = "delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    @InsertProvider(type = SqlProviderAdapter.class, method = "insert")
    int insert(InsertStatementProvider<MciMessageMappingInfo> insertStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    @InsertProvider(type = SqlProviderAdapter.class, method = "insertMultiple")
    int insertMultiple(MultiRowInsertStatementProvider<MciMessageMappingInfo> multipleInsertStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @ResultMap("MciMessageMappingInfoResult")
    Optional<MciMessageMappingInfo> selectOne(SelectStatementProvider selectStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @Results(id = "MciMessageMappingInfoResult", value = {
            @Result(column = "GROUP_ID", property = "groupId", jdbcType = JdbcType.VARCHAR, id = true),
            @Result(column = "INTERFACE_ID", property = "interfaceId", jdbcType = JdbcType.VARCHAR, id = true),
            @Result(column = "SRC_MESSAGE_ID", property = "srcMessageId", jdbcType = JdbcType.VARCHAR),
            @Result(column = "TRG_MESSAGE_ID", property = "trgMessageId", jdbcType = JdbcType.VARCHAR),
            @Result(column = "CREATION_DATE", property = "creationDate", jdbcType = JdbcType.TIMESTAMP),
            @Result(column = "MODIFIED_DATE", property = "modifiedDate", jdbcType = JdbcType.TIMESTAMP),
            @Result(column = "CONSTRUCTOR_ID", property = "constructorId", jdbcType = JdbcType.VARCHAR),
            @Result(column = "MODIFIER_ID", property = "modifierId", jdbcType = JdbcType.VARCHAR),
            @Result(column = "USAGE_STATUS", property = "usageStatus", jdbcType = JdbcType.VARCHAR),
            @Result(column = "START_USE_DATE", property = "startUseDate", jdbcType = JdbcType.DATE),
            @Result(column = "END_USE_DATE", property = "endUseDate", jdbcType = JdbcType.DATE)
    })
    List<MciMessageMappingInfo> selectMany(SelectStatementProvider selectStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    @UpdateProvider(type = SqlProviderAdapter.class, method = "update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, mciMessageMappingInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, mciMessageMappingInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    default int deleteByPrimaryKey(String groupId_, String interfaceId_) {
        return delete(c ->
                c.where(groupId, isEqualTo(groupId_))
                        .and(interfaceId, isEqualTo(interfaceId_))
        );
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    default int insert(MciMessageMappingInfo record) {
        return MyBatis3Utils.insert(this::insert, record, mciMessageMappingInfo, c ->
                c.map(groupId).toProperty("groupId")
                        .map(interfaceId).toProperty("interfaceId")
                        .map(srcMessageId).toProperty("srcMessageId")
                        .map(trgMessageId).toProperty("trgMessageId")
                        .map(creationDate).toProperty("creationDate")
                        .map(modifiedDate).toProperty("modifiedDate")
                        .map(constructorId).toProperty("constructorId")
                        .map(modifierId).toProperty("modifierId")
                        .map(usageStatus).toProperty("usageStatus")
                        .map(startUseDate).toProperty("startUseDate")
                        .map(endUseDate).toProperty("endUseDate")
        );
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    default int insertMultiple(Collection<MciMessageMappingInfo> records) {
        return MyBatis3Utils.insertMultiple(this::insertMultiple, records, mciMessageMappingInfo, c ->
                c.map(groupId).toProperty("groupId")
                        .map(interfaceId).toProperty("interfaceId")
                        .map(srcMessageId).toProperty("srcMessageId")
                        .map(trgMessageId).toProperty("trgMessageId")
                        .map(creationDate).toProperty("creationDate")
                        .map(modifiedDate).toProperty("modifiedDate")
                        .map(constructorId).toProperty("constructorId")
                        .map(modifierId).toProperty("modifierId")
                        .map(usageStatus).toProperty("usageStatus")
                        .map(startUseDate).toProperty("startUseDate")
                        .map(endUseDate).toProperty("endUseDate")
        );
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    default int insertSelective(MciMessageMappingInfo record) {
        return MyBatis3Utils.insert(this::insert, record, mciMessageMappingInfo, c ->
                c.map(groupId).toPropertyWhenPresent("groupId", record::getGroupId)
                        .map(interfaceId).toPropertyWhenPresent("interfaceId", record::getInterfaceId)
                        .map(srcMessageId).toPropertyWhenPresent("srcMessageId", record::getSrcMessageId)
                        .map(trgMessageId).toPropertyWhenPresent("trgMessageId", record::getTrgMessageId)
                        .map(creationDate).toPropertyWhenPresent("creationDate", record::getCreationDate)
                        .map(modifiedDate).toPropertyWhenPresent("modifiedDate", record::getModifiedDate)
                        .map(constructorId).toPropertyWhenPresent("constructorId", record::getConstructorId)
                        .map(modifierId).toPropertyWhenPresent("modifierId", record::getModifierId)
                        .map(usageStatus).toPropertyWhenPresent("usageStatus", record::getUsageStatus)
                        .map(startUseDate).toPropertyWhenPresent("startUseDate", record::getStartUseDate)
                        .map(endUseDate).toPropertyWhenPresent("endUseDate", record::getEndUseDate)
        );
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    default Optional<MciMessageMappingInfo> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, mciMessageMappingInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    default List<MciMessageMappingInfo> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, mciMessageMappingInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    default List<MciMessageMappingInfo> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, mciMessageMappingInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    default Optional<MciMessageMappingInfo> selectByPrimaryKey(String groupId_, String interfaceId_) {
        return selectOne(c ->
                c.where(groupId, isEqualTo(groupId_))
                        .and(interfaceId, isEqualTo(interfaceId_))
        );
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, mciMessageMappingInfo, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.304+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    static UpdateDSL<UpdateModel> updateAllColumns(MciMessageMappingInfo record, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(groupId).equalTo(record::getGroupId)
                .set(interfaceId).equalTo(record::getInterfaceId)
                .set(srcMessageId).equalTo(record::getSrcMessageId)
                .set(trgMessageId).equalTo(record::getTrgMessageId)
                .set(creationDate).equalTo(record::getCreationDate)
                .set(modifiedDate).equalTo(record::getModifiedDate)
                .set(constructorId).equalTo(record::getConstructorId)
                .set(modifierId).equalTo(record::getModifierId)
                .set(usageStatus).equalTo(record::getUsageStatus)
                .set(startUseDate).equalTo(record::getStartUseDate)
                .set(endUseDate).equalTo(record::getEndUseDate);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.305+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    static UpdateDSL<UpdateModel> updateSelectiveColumns(MciMessageMappingInfo record, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(groupId).equalToWhenPresent(record::getGroupId)
                .set(interfaceId).equalToWhenPresent(record::getInterfaceId)
                .set(srcMessageId).equalToWhenPresent(record::getSrcMessageId)
                .set(trgMessageId).equalToWhenPresent(record::getTrgMessageId)
                .set(creationDate).equalToWhenPresent(record::getCreationDate)
                .set(modifiedDate).equalToWhenPresent(record::getModifiedDate)
                .set(constructorId).equalToWhenPresent(record::getConstructorId)
                .set(modifierId).equalToWhenPresent(record::getModifierId)
                .set(usageStatus).equalToWhenPresent(record::getUsageStatus)
                .set(startUseDate).equalToWhenPresent(record::getStartUseDate)
                .set(endUseDate).equalToWhenPresent(record::getEndUseDate);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.305+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    default int updateByPrimaryKey(MciMessageMappingInfo record) {
        return update(c ->
                c.set(srcMessageId).equalTo(record::getSrcMessageId)
                        .set(trgMessageId).equalTo(record::getTrgMessageId)
                        .set(creationDate).equalTo(record::getCreationDate)
                        .set(modifiedDate).equalTo(record::getModifiedDate)
                        .set(constructorId).equalTo(record::getConstructorId)
                        .set(modifierId).equalTo(record::getModifierId)
                        .set(usageStatus).equalTo(record::getUsageStatus)
                        .set(startUseDate).equalTo(record::getStartUseDate)
                        .set(endUseDate).equalTo(record::getEndUseDate)
                        .where(groupId, isEqualTo(record::getGroupId))
                        .and(interfaceId, isEqualTo(record::getInterfaceId))
        );
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", date = "2021-05-11T13:40:41.305+09:00", comments = "Source Table: MCI_MESSAGE_MAPPING_INFO")
    default int updateByPrimaryKeySelective(MciMessageMappingInfo record) {
        return update(c ->
                c.set(srcMessageId).equalToWhenPresent(record::getSrcMessageId)
                        .set(trgMessageId).equalToWhenPresent(record::getTrgMessageId)
                        .set(creationDate).equalToWhenPresent(record::getCreationDate)
                        .set(modifiedDate).equalToWhenPresent(record::getModifiedDate)
                        .set(constructorId).equalToWhenPresent(record::getConstructorId)
                        .set(modifierId).equalToWhenPresent(record::getModifierId)
                        .set(usageStatus).equalToWhenPresent(record::getUsageStatus)
                        .set(startUseDate).equalToWhenPresent(record::getStartUseDate)
                        .set(endUseDate).equalToWhenPresent(record::getEndUseDate)
                        .where(groupId, isEqualTo(record::getGroupId))
                        .and(interfaceId, isEqualTo(record::getInterfaceId))
        );
    }
}