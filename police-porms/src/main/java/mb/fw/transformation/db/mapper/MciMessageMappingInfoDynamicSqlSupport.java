package mb.fw.transformation.db.mapper;

import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

import javax.annotation.Generated;
import java.sql.JDBCType;
import java.util.Date;

public final class MciMessageMappingInfoDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.303+09:00", comments="Source Table: MCI_MESSAGE_MAPPING_INFO")
    public static final MciMessageMappingInfo mciMessageMappingInfo = new MciMessageMappingInfo();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.303+09:00", comments="Source field: MCI_MESSAGE_MAPPING_INFO.GROUP_ID")
    public static final SqlColumn<String> groupId = mciMessageMappingInfo.groupId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.303+09:00", comments="Source field: MCI_MESSAGE_MAPPING_INFO.INTERFACE_ID")
    public static final SqlColumn<String> interfaceId = mciMessageMappingInfo.interfaceId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.303+09:00", comments="Source field: MCI_MESSAGE_MAPPING_INFO.SRC_MESSAGE_ID")
    public static final SqlColumn<String> srcMessageId = mciMessageMappingInfo.srcMessageId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.303+09:00", comments="Source field: MCI_MESSAGE_MAPPING_INFO.TRG_MESSAGE_ID")
    public static final SqlColumn<String> trgMessageId = mciMessageMappingInfo.trgMessageId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.304+09:00", comments="Source field: MCI_MESSAGE_MAPPING_INFO.CREATION_DATE")
    public static final SqlColumn<Date> creationDate = mciMessageMappingInfo.creationDate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.304+09:00", comments="Source field: MCI_MESSAGE_MAPPING_INFO.MODIFIED_DATE")
    public static final SqlColumn<Date> modifiedDate = mciMessageMappingInfo.modifiedDate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.304+09:00", comments="Source field: MCI_MESSAGE_MAPPING_INFO.CONSTRUCTOR_ID")
    public static final SqlColumn<String> constructorId = mciMessageMappingInfo.constructorId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.304+09:00", comments="Source field: MCI_MESSAGE_MAPPING_INFO.MODIFIER_ID")
    public static final SqlColumn<String> modifierId = mciMessageMappingInfo.modifierId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.304+09:00", comments="Source field: MCI_MESSAGE_MAPPING_INFO.USAGE_STATUS")
    public static final SqlColumn<String> usageStatus = mciMessageMappingInfo.usageStatus;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.304+09:00", comments="Source field: MCI_MESSAGE_MAPPING_INFO.START_USE_DATE")
    public static final SqlColumn<Date> startUseDate = mciMessageMappingInfo.startUseDate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.304+09:00", comments="Source field: MCI_MESSAGE_MAPPING_INFO.END_USE_DATE")
    public static final SqlColumn<Date> endUseDate = mciMessageMappingInfo.endUseDate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.303+09:00", comments="Source Table: MCI_MESSAGE_MAPPING_INFO")
    public static final class MciMessageMappingInfo extends SqlTable {
        public final SqlColumn<String> groupId = column("GROUP_ID", JDBCType.VARCHAR);

        public final SqlColumn<String> interfaceId = column("INTERFACE_ID", JDBCType.VARCHAR);

        public final SqlColumn<String> srcMessageId = column("SRC_MESSAGE_ID", JDBCType.VARCHAR);

        public final SqlColumn<String> trgMessageId = column("TRG_MESSAGE_ID", JDBCType.VARCHAR);

        public final SqlColumn<Date> creationDate = column("CREATION_DATE", JDBCType.TIMESTAMP);

        public final SqlColumn<Date> modifiedDate = column("MODIFIED_DATE", JDBCType.TIMESTAMP);

        public final SqlColumn<String> constructorId = column("CONSTRUCTOR_ID", JDBCType.VARCHAR);

        public final SqlColumn<String> modifierId = column("MODIFIER_ID", JDBCType.VARCHAR);

        public final SqlColumn<String> usageStatus = column("USAGE_STATUS", JDBCType.VARCHAR);

        public final SqlColumn<Date> startUseDate = column("START_USE_DATE", JDBCType.DATE);

        public final SqlColumn<Date> endUseDate = column("END_USE_DATE", JDBCType.DATE);

        public MciMessageMappingInfo() {
            super("MCI_MESSAGE_MAPPING_INFO");
        }
    }
}