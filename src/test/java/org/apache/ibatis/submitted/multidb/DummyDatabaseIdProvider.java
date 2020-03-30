package org.apache.ibatis.submitted.multidb;

import org.apache.ibatis.mapping.DatabaseIdProvider;

import javax.sql.DataSource;
import java.util.Properties;

/**
 *  假的，仿真的
 */
public class DummyDatabaseIdProvider implements DatabaseIdProvider {

    private Properties properties;

    @Override
    public String getDatabaseId(DataSource dataSource) {
        return properties.getProperty("name");
    }

    @Override
    public void setProperties(Properties p) {
        this.properties = p;
    }
}