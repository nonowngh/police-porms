package mb.fw.transformation.db.mapper;

import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

import javax.annotation.Generated;
import java.sql.JDBCType;

public final class MciFieldInfoDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.297+09:00", comments="Source Table: MCI_FIELD_INFO")
    public static final MciFieldInfo mciFieldInfo = new MciFieldInfo();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.297+09:00", comments="Source field: MCI_FIELD_INFO.GROUP_ID")
    public static final SqlColumn<String> groupId = mciFieldInfo.groupId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.297+09:00", comments="Source field: MCI_FIELD_INFO.INTERFACE_ID")
    public static final SqlColumn<String> interfaceId = mciFieldInfo.interfaceId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source field: MCI_FIELD_INFO.MESSAGE_ID")
    public static final SqlColumn<String> messageId = mciFieldInfo.messageId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source field: MCI_FIELD_INFO.SEQ")
    public static final SqlColumn<Integer> seq = mciFieldInfo.seq;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source field: MCI_FIELD_INFO.FIELD_ID")
    public static final SqlColumn<String> fieldId = mciFieldInfo.fieldId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source field: MCI_FIELD_INFO.FIELD_NAME")
    public static final SqlColumn<String> fieldName = mciFieldInfo.fieldName;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source field: MCI_FIELD_INFO.DESCRIPTION")
    public static final SqlColumn<String> description = mciFieldInfo.description;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source field: MCI_FIELD_INFO.FIELD_TYPE")
    public static final SqlColumn<String> fieldType = mciFieldInfo.fieldType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source field: MCI_FIELD_INFO.FIELD_LENGTH")
    public static final SqlColumn<String> fieldLength = mciFieldInfo.fieldLength;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source field: MCI_FIELD_INFO.CHILD_COUNT")
    public static final SqlColumn<Integer> childCount = mciFieldInfo.childCount;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source field: MCI_FIELD_INFO.SRC_SEQ")
    public static final SqlColumn<Integer> srcSeq = mciFieldInfo.srcSeq;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source field: MCI_FIELD_INFO.MAPPING_INFO")
    public static final SqlColumn<String> mappingInfo = mciFieldInfo.mappingInfo;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source field: MCI_FIELD_INFO.DEFAULT_VALUE")
    public static final SqlColumn<String> defaultValue = mciFieldInfo.defaultValue;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.298+09:00", comments="Source field: MCI_FIELD_INFO.COUNT_FIELD")
    public static final SqlColumn<Integer> countField = mciFieldInfo.countField;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.297+09:00", comments="Source Table: MCI_FIELD_INFO")
    public static final class MciFieldInfo extends SqlTable {
        public final SqlColumn<String> groupId = column("GROUP_ID", JDBCType.VARCHAR);

        public final SqlColumn<String> interfaceId = column("INTERFACE_ID", JDBCType.VARCHAR);

        public final SqlColumn<String> messageId = column("MESSAGE_ID", JDBCType.VARCHAR);

        public final SqlColumn<Integer> seq = column("SEQ", JDBCType.INTEGER);

        public final SqlColumn<String> fieldId = column("FIELD_ID", JDBCType.VARCHAR);

        public final SqlColumn<String> fieldName = column("FIELD_NAME", JDBCType.VARCHAR);

        public final SqlColumn<String> description = column("DESCRIPTION", JDBCType.VARCHAR);

        public final SqlColumn<String> fieldType = column("FIELD_TYPE", JDBCType.VARCHAR);

        public final SqlColumn<String> fieldLength = column("FIELD_LENGTH", JDBCType.VARCHAR);

        public final SqlColumn<Integer> childCount = column("CHILD_COUNT", JDBCType.INTEGER);

        public final SqlColumn<Integer> srcSeq = column("SRC_SEQ", JDBCType.INTEGER);

        public final SqlColumn<String> mappingInfo = column("MAPPING_INFO", JDBCType.VARCHAR);

        public final SqlColumn<String> defaultValue = column("DEFAULT_VALUE", JDBCType.VARCHAR);

        public final SqlColumn<Integer> countField = column("COUNT_FIELD", JDBCType.INTEGER);

        public MciFieldInfo() {
            super("MCI_FIELD_INFO");
        }
    }
}