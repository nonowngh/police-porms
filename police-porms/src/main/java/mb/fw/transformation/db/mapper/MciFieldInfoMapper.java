package mb.fw.transformation.db.mapper;

import mb.fw.transformation.db.model.MciFieldInfo;
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

import static mb.fw.transformation.db.mapper.MciFieldInfoDynamicSqlSupport.*;

@Mapper
public interface MciFieldInfoMapper {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.299+09:00", comments="Source Table: MCI_FIELD_INFO")
    BasicColumn[] selectList = BasicColumn.columnList(groupId, interfaceId, messageId, seq, fieldId, fieldName, description, fieldType, fieldLength, childCount, srcSeq, mappingInfo, defaultValue, countField);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source Table: MCI_FIELD_INFO")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    long count(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source Table: MCI_FIELD_INFO")
    @DeleteProvider(type=SqlProviderAdapter.class, method="delete")
    int delete(DeleteStatementProvider deleteStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source Table: MCI_FIELD_INFO")
    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    int insert(InsertStatementProvider<MciFieldInfo> insertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source Table: MCI_FIELD_INFO")
    @InsertProvider(type=SqlProviderAdapter.class, method="insertMultiple")
    int insertMultiple(MultiRowInsertStatementProvider<MciFieldInfo> multipleInsertStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source Table: MCI_FIELD_INFO")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ResultMap("MciFieldInfoResult")
    Optional<MciFieldInfo> selectOne(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source Table: MCI_FIELD_INFO")
    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @Results(id="MciFieldInfoResult", value = {
        @Result(column="GROUP_ID", property="groupId", jdbcType=JdbcType.VARCHAR),
        @Result(column="INTERFACE_ID", property="interfaceId", jdbcType=JdbcType.VARCHAR),
        @Result(column="MESSAGE_ID", property="messageId", jdbcType=JdbcType.VARCHAR),
        @Result(column="SEQ", property="seq", jdbcType=JdbcType.INTEGER),
        @Result(column="FIELD_ID", property="fieldId", jdbcType=JdbcType.VARCHAR),
        @Result(column="FIELD_NAME", property="fieldName", jdbcType=JdbcType.VARCHAR),
        @Result(column="DESCRIPTION", property="description", jdbcType=JdbcType.VARCHAR),
        @Result(column="FIELD_TYPE", property="fieldType", jdbcType=JdbcType.VARCHAR),
        @Result(column="FIELD_LENGTH", property="fieldLength", jdbcType=JdbcType.VARCHAR),
        @Result(column="CHILD_COUNT", property="childCount", jdbcType=JdbcType.INTEGER),
        @Result(column="SRC_SEQ", property="srcSeq", jdbcType=JdbcType.INTEGER),
        @Result(column="MAPPING_INFO", property="mappingInfo", jdbcType=JdbcType.VARCHAR),
        @Result(column="DEFAULT_VALUE", property="defaultValue", jdbcType=JdbcType.VARCHAR),
        @Result(column="COUNT_FIELD", property="countField", jdbcType=JdbcType.INTEGER)
    })
    List<MciFieldInfo> selectMany(SelectStatementProvider selectStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source Table: MCI_FIELD_INFO")
    @UpdateProvider(type=SqlProviderAdapter.class, method="update")
    int update(UpdateStatementProvider updateStatement);

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source Table: MCI_FIELD_INFO")
    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, mciFieldInfo, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.299+09:00", comments="Source Table: MCI_FIELD_INFO")
    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, mciFieldInfo, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.299+09:00", comments="Source Table: MCI_FIELD_INFO")
    default int insert(MciFieldInfo record) {
        return MyBatis3Utils.insert(this::insert, record, mciFieldInfo, c ->
            c.map(groupId).toProperty("groupId")
            .map(interfaceId).toProperty("interfaceId")
            .map(messageId).toProperty("messageId")
            .map(seq).toProperty("seq")
            .map(fieldId).toProperty("fieldId")
            .map(fieldName).toProperty("fieldName")
            .map(description).toProperty("description")
            .map(fieldType).toProperty("fieldType")
            .map(fieldLength).toProperty("fieldLength")
            .map(childCount).toProperty("childCount")
            .map(srcSeq).toProperty("srcSeq")
            .map(mappingInfo).toProperty("mappingInfo")
            .map(defaultValue).toProperty("defaultValue")
            .map(countField).toProperty("countField")
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.299+09:00", comments="Source Table: MCI_FIELD_INFO")
    default int insertMultiple(Collection<MciFieldInfo> records) {
        return MyBatis3Utils.insertMultiple(this::insertMultiple, records, mciFieldInfo, c ->
            c.map(groupId).toProperty("groupId")
            .map(interfaceId).toProperty("interfaceId")
            .map(messageId).toProperty("messageId")
            .map(seq).toProperty("seq")
            .map(fieldId).toProperty("fieldId")
            .map(fieldName).toProperty("fieldName")
            .map(description).toProperty("description")
            .map(fieldType).toProperty("fieldType")
            .map(fieldLength).toProperty("fieldLength")
            .map(childCount).toProperty("childCount")
            .map(srcSeq).toProperty("srcSeq")
            .map(mappingInfo).toProperty("mappingInfo")
            .map(defaultValue).toProperty("defaultValue")
            .map(countField).toProperty("countField")
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.299+09:00", comments="Source Table: MCI_FIELD_INFO")
    default int insertSelective(MciFieldInfo record) {
        return MyBatis3Utils.insert(this::insert, record, mciFieldInfo, c ->
            c.map(groupId).toPropertyWhenPresent("groupId", record::getGroupId)
            .map(interfaceId).toPropertyWhenPresent("interfaceId", record::getInterfaceId)
            .map(messageId).toPropertyWhenPresent("messageId", record::getMessageId)
            .map(seq).toPropertyWhenPresent("seq", record::getSeq)
            .map(fieldId).toPropertyWhenPresent("fieldId", record::getFieldId)
            .map(fieldName).toPropertyWhenPresent("fieldName", record::getFieldName)
            .map(description).toPropertyWhenPresent("description", record::getDescription)
            .map(fieldType).toPropertyWhenPresent("fieldType", record::getFieldType)
            .map(fieldLength).toPropertyWhenPresent("fieldLength", record::getFieldLength)
            .map(childCount).toPropertyWhenPresent("childCount", record::getChildCount)
            .map(srcSeq).toPropertyWhenPresent("srcSeq", record::getSrcSeq)
            .map(mappingInfo).toPropertyWhenPresent("mappingInfo", record::getMappingInfo)
            .map(defaultValue).toPropertyWhenPresent("defaultValue", record::getDefaultValue)
            .map(countField).toPropertyWhenPresent("countField", record::getCountField)
        );
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.299+09:00", comments="Source Table: MCI_FIELD_INFO")
    default Optional<MciFieldInfo> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, mciFieldInfo, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.299+09:00", comments="Source Table: MCI_FIELD_INFO")
    default List<MciFieldInfo> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, mciFieldInfo, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.299+09:00", comments="Source Table: MCI_FIELD_INFO")
    default List<MciFieldInfo> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, mciFieldInfo, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.299+09:00", comments="Source Table: MCI_FIELD_INFO")
    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, mciFieldInfo, completer);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.299+09:00", comments="Source Table: MCI_FIELD_INFO")
    static UpdateDSL<UpdateModel> updateAllColumns(MciFieldInfo record, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(groupId).equalTo(record::getGroupId)
                .set(interfaceId).equalTo(record::getInterfaceId)
                .set(messageId).equalTo(record::getMessageId)
                .set(seq).equalTo(record::getSeq)
                .set(fieldId).equalTo(record::getFieldId)
                .set(fieldName).equalTo(record::getFieldName)
                .set(description).equalTo(record::getDescription)
                .set(fieldType).equalTo(record::getFieldType)
                .set(fieldLength).equalTo(record::getFieldLength)
                .set(childCount).equalTo(record::getChildCount)
                .set(srcSeq).equalTo(record::getSrcSeq)
                .set(mappingInfo).equalTo(record::getMappingInfo)
                .set(defaultValue).equalTo(record::getDefaultValue)
                .set(countField).equalTo(record::getCountField);
    }

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.299+09:00", comments="Source Table: MCI_FIELD_INFO")
    static UpdateDSL<UpdateModel> updateSelectiveColumns(MciFieldInfo record, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(groupId).equalToWhenPresent(record::getGroupId)
                .set(interfaceId).equalToWhenPresent(record::getInterfaceId)
                .set(messageId).equalToWhenPresent(record::getMessageId)
                .set(seq).equalToWhenPresent(record::getSeq)
                .set(fieldId).equalToWhenPresent(record::getFieldId)
                .set(fieldName).equalToWhenPresent(record::getFieldName)
                .set(description).equalToWhenPresent(record::getDescription)
                .set(fieldType).equalToWhenPresent(record::getFieldType)
                .set(fieldLength).equalToWhenPresent(record::getFieldLength)
                .set(childCount).equalToWhenPresent(record::getChildCount)
                .set(srcSeq).equalToWhenPresent(record::getSrcSeq)
                .set(mappingInfo).equalToWhenPresent(record::getMappingInfo)
                .set(defaultValue).equalToWhenPresent(record::getDefaultValue)
                .set(countField).equalToWhenPresent(record::getCountField);
    }
}