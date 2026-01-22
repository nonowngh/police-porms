package mb.fw.transformation.service;

import mb.fw.transformation.form.MessageFormBox;
import mb.fw.transformation.loader.DBMessageFormBoxLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class MapperServiceImpl implements MapperService {

	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	DataSource dataSource;

	@Override
	public MessageFormBox getMessageFormBox(String groupId) {

		DBMessageFormBoxLoader loader = null;
		Connection con = null;
		try {
			con = dataSource.getConnection();
		} catch (SQLException e) {
		} finally {
			DataSourceUtils.releaseConnection(con, dataSource);
		}

		loader = new DBMessageFormBoxLoader(dataSource);
		loader.setGroupId(groupId);
		MessageFormBox box = loader.formload();
		return box;
	}
}
