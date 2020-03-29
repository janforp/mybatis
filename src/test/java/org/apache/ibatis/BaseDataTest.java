package org.apache.ibatis;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public abstract class BaseDataTest {

    public static final String BLOG_PROPERTIES = "org/apache/ibatis/databases/blog/blog-derby.properties";

    public static final String BLOG_DDL = "org/apache/ibatis/databases/blog/blog-derby-schema.sql";

    public static final String BLOG_DATA = "org/apache/ibatis/databases/blog/blog-derby-dataload.sql";

    public static final String JPETSTORE_PROPERTIES = "org/apache/ibatis/databases/jpetstore/jpetstore-hsqldb.properties";

    public static final String JPETSTORE_DDL = "org/apache/ibatis/databases/jpetstore/jpetstore-hsqldb-schema.sql";

    public static final String JPETSTORE_DATA = "org/apache/ibatis/databases/jpetstore/jpetstore-hsqldb-dataload.sql";

    public static UnpooledDataSource createUnpooledDataSource(String resource) throws IOException {
        Properties props = Resources.getResourceAsProperties(resource);
        UnpooledDataSource ds = new UnpooledDataSource();
        ds.setDriver(props.getProperty("driver"));
        ds.setUrl(props.getProperty("url"));
        ds.setUsername(props.getProperty("username"));
        ds.setPassword(props.getProperty("password"));
        return ds;
    }

    public static PooledDataSource createPooledDataSource(String resource) throws IOException {
        Properties props = Resources.getResourceAsProperties(resource);
        PooledDataSource ds = new PooledDataSource();
        ds.setDriver(props.getProperty("driver"));
        ds.setUrl(props.getProperty("url"));
        ds.setUsername(props.getProperty("username"));
        ds.setPassword(props.getProperty("password"));
        return ds;
    }

    public static void runScript(DataSource ds, String resource) throws IOException, SQLException {
        Connection connection = ds.getConnection();
        try {
            //跑脚本
            ScriptRunner runner = new ScriptRunner(connection);
            runner.setAutoCommit(true);
            runner.setStopOnError(false);
            runner.setLogWriter(null);
            runner.setErrorLogWriter(null);
            runScript(runner, resource);
        } finally {
            connection.close();
        }
    }

    public static void runScript(ScriptRunner runner, String resource) throws IOException, SQLException {
        Reader reader = Resources.getResourceAsReader(resource);
        try {
            runner.runScript(reader);
        } finally {
            reader.close();
        }
    }

    /**
     * 加载数据库驱动以及¬初始化数据库脚本以及数据
     *
     * @return 数据源
     */
    public static DataSource createBlogDataSource() throws IOException, SQLException {
        DataSource ds = createUnpooledDataSource(BLOG_PROPERTIES);
        runScript(ds, BLOG_DDL);
        runScript(ds, BLOG_DATA);
        return ds;
    }

    public static DataSource createJPetstoreDataSource() throws IOException, SQLException {
        DataSource ds = createUnpooledDataSource(JPETSTORE_PROPERTIES);
        runScript(ds, JPETSTORE_DDL);
        runScript(ds, JPETSTORE_DATA);
        return ds;
    }
}
