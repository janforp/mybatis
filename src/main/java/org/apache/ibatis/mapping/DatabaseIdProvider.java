package org.apache.ibatis.mapping;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Should return an id to identify the type of this database.
 * That id can be used later on to build different queries for each database type
 * This mechanism enables supporting multiple vendors or versions
 * DB id 提供者
 *
 * @author Eduardo Macarron
 */
public interface DatabaseIdProvider {

    void setProperties(Properties p);

    //根据数据源来得到一个DB id
    String getDatabaseId(DataSource dataSource) throws SQLException;
}
