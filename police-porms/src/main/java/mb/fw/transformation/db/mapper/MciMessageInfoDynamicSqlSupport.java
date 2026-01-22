package mb.fw.transformation.db.mapper;

import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;

import javax.annotation.Generated;
import java.sql.JDBCType;
import java.util.Date;

public final class MciMessageInfoDynamicSqlSupport {
    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source Table: MCI_MESSAGE_INFO")
    public static final MciMessageInfo mciMessageInfo = new MciMessageInfo();

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source field: MCI_MESSAGE_INFO.GROUP_ID")
    public static final SqlColumn<String> groupId = mciMessageInfo.groupId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source field: MCI_MESSAGE_INFO.MESSAGE_ID")
    public static final SqlColumn<String> messageId = mciMessageInfo.messageId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source field: MCI_MESSAGE_INFO.MESSAGE_NAME")
    public static final SqlColumn<String> messageName = mciMessageInfo.messageName;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source field: MCI_MESSAGE_INFO.INTERFACE_ID")
    public static final SqlColumn<String> interfaceId = mciMessageInfo.interfaceId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source field: MCI_MESSAGE_INFO.CREATION_DATE")
    public static final SqlColumn<Date> creationDate = mciMessageInfo.creationDate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source field: MCI_MESSAGE_INFO.MODIFIED_DATE")
    public static final SqlColumn<Date> modifiedDate = mciMessageInfo.modifiedDate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source field: MCI_MESSAGE_INFO.CONSTRUCTOR_ID")
    public static final SqlColumn<String> constructorId = mciMessageInfo.constructorId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source field: MCI_MESSAGE_INFO.MODIFIER_ID")
    public static final SqlColumn<String> modifierId = mciMessageInfo.modifierId;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source field: MCI_MESSAGE_INFO.USAGE_STATUS")
    public static final SqlColumn<String> usageStatus = mciMessageInfo.usageStatus;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source field: MCI_MESSAGE_INFO.START_USE_DATE")
    public static final SqlColumn<Date> startUseDate = mciMessageInfo.startUseDate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source field: MCI_MESSAGE_INFO.END_USE_DATE")
    public static final SqlColumn<Date> endUseDate = mciMessageInfo.endUseDate;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source field: MCI_MESSAGE_INFO.DESCRIPTION")
    public static final SqlColumn<String> description = mciMessageInfo.description;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source field: MCI_MESSAGE_INFO.MESSAGE_VERSION")
    public static final SqlColumn<String> messageVersion = mciMessageInfo.messageVersion;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source field: MCI_MESSAGE_INFO.MESSAGE_TYPE")
    public static final SqlColumn<String> messageType = mciMessageInfo.messageType;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source field: MCI_MESSAGE_INFO.MESSAGE_ATTR")
    public static final SqlColumn<String> messageAttr = mciMessageInfo.messageAttr;

    @Generated(value="org.mybatis.generator.api.MyBatisGenerator", date="2021-05-11T13:40:41.301+09:00", comments="Source Table: MCI_MESSAGE_INFO")
    public static final class MciMessageInfo extends SqlTable {
        public final SqlColumn<String> groupId = column("GROUP_ID", JDBCType.VARCHAR);

        public final SqlColumn<String> messageId = column("MESSAGE_ID", JDBCType.VARCHAR);

        public final SqlColumn<String> messageName = column("MESSAGE_NAME", JDBCType.VARCHAR);

        public final SqlColumn<String> interfaceId = column("INTERFACE_ID", JDBCType.VARCHAR);

        public final SqlColumn<Date> creationDate = column("CREATION_DATE", JDBCType.TIMESTAMP);

        public final SqlColumn<Date> modifiedDate = column("MODIFIED_DATE", JDBCType.TIMESTAMP);

        public final SqlColumn<String> constructorId = column("CONSTRUCTOR_ID", JDBCType.VARCHAR);

        public final SqlColumn<String> modifierId = column("MODIFIER_ID", JDBCType.VARCHAR);

        public final SqlColumn<String> usageStatus = column("USAGE_STATUS", JDBCType.VARCHAR);

        public final SqlColumn<Date> startUseDate = column("START_USE_DATE", JDBCType.DATE);

        public final SqlColumn<Date> endUseDate = column("END_USE_DATE", JDBCType.DATE);

        public final SqlColumn<String> description = column("DESCRIPTION", JDBCType.VARCHAR);

        public final SqlColumn<String> messageVersion = column("MESSAGE_VERSION", JDBCType.VARCHAR);

        public final SqlColumn<String> messageType = column("MESSAGE_TYPE", JDBCType.VARCHAR);

        public final SqlColumn<String> messageAttr = column("MESSAGE_ATTR", JDBCType.VARCHAR);

        public MciMessageInfo() {
            super("MCI_MESSAGE_INFO");
        }
    }
}