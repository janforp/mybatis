package org.apache.ibatis.datasource.unpooled;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.ibatis.io.Resources;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */

/**
 * 没有池化的数据源
 */
@NoArgsConstructor
public class UnpooledDataSource implements DataSource {

    /**
     * 已经注册的驱动
     */
    private static Map<String, Driver> registeredDrivers = new ConcurrentHashMap<String, Driver>();

    static {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();

            String driveClassName = driver.getClass().getName();
            registeredDrivers.put(driveClassName, driver);
        }
    }

    /**
     * 驱动类加载器
     */
    @Getter
    @Setter
    private ClassLoader driverClassLoader;

    //作为可选项,你可以传递数据库驱动的属性。要这样做,属性的前缀是以“driver.”开 头的,例如
    //driver.encoding=UTF8
    @Getter
    @Setter
    private Properties driverProperties;

    @Getter
    private String driver;

    @Getter
    @Setter
    private String url;

    @Getter
    @Setter
    private String username;

    @Getter
    @Setter
    private String password;

    @Setter
    private Boolean autoCommit;

    /**
     * 事务级别
     */
    @Getter
    @Setter
    private Integer defaultTransactionIsolationLevel;

    public UnpooledDataSource(String driver, String url, String username, String password) {
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public UnpooledDataSource(String driver, String url, Properties driverProperties) {
        this.driver = driver;
        this.url = url;
        this.driverProperties = driverProperties;
    }

    public UnpooledDataSource(ClassLoader driverClassLoader, String driver, String url, String username, String password) {
        this.driverClassLoader = driverClassLoader;
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public UnpooledDataSource(ClassLoader driverClassLoader, String driver, String url, Properties driverProperties) {
        this.driverClassLoader = driverClassLoader;
        this.driver = driver;
        this.url = url;
        this.driverProperties = driverProperties;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return doGetConnection(username, password);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return doGetConnection(username, password);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int loginTimeout) throws SQLException {
        DriverManager.setLoginTimeout(loginTimeout);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return DriverManager.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter logWriter) throws SQLException {
        DriverManager.setLogWriter(logWriter);
    }

    public synchronized void setDriver(String driver) {
        this.driver = driver;
    }

    private Connection doGetConnection(String username, String password) throws SQLException {
        Properties props = new Properties();
        if (driverProperties != null) {
            props.putAll(driverProperties);
        }
        if (username != null) {
            props.setProperty("user", username);
        }
        if (password != null) {
            props.setProperty("password", password);
        }
        return doGetConnection(props);
    }

    private Connection doGetConnection(Properties properties) throws SQLException {
        //初始化驱动，主要是类加载
        initializeDriver();
        //属性的前缀是以“driver.”开 头的,它 是 通 过 DriverManager.getConnection(url,driverProperties)方法传递给数据库驱动
        Connection connection = DriverManager.getConnection(url, properties);
        //设置自动提交以及事务级别
        configureConnection(connection);
        return connection;
    }

    private synchronized void initializeDriver() throws SQLException {
        //这里便是大家熟悉的初学JDBC时的那几句话了 Class.forName newInstance()
        boolean containsKey = registeredDrivers.containsKey(driver);
        if (!containsKey) {
            Class<?> driverType;
            try {
                if (driverClassLoader != null) {
                    driverType = Class.forName(driver, true, driverClassLoader);
                } else {
                    driverType = Resources.classForName(driver);
                }
                // DriverManager requires the driver to be loaded via the system ClassLoader.
                // http://www.kfu.com/~nsayer/Java/dyn-jdbc.html
                Driver driverInstance = (Driver) driverType.newInstance();
                //TODO ？ 为什么注册一个驱动代理对象？
                DriverManager.registerDriver(new DriverProxy(driverInstance));
                registeredDrivers.put(driver, driverInstance);
            } catch (Exception e) {
                throw new SQLException("Error setting driver on UnpooledDataSource. Cause: " + e);
            }
        }
    }

    private void configureConnection(Connection conn) throws SQLException {
        if (autoCommit != null && autoCommit != conn.getAutoCommit()) {
            conn.setAutoCommit(autoCommit);
        }
        if (defaultTransactionIsolationLevel != null) {
            conn.setTransactionIsolation(defaultTransactionIsolationLevel);
        }
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException(getClass().getName() + " is not a wrapper.");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    // @Override only valid jdk7+
    public Logger getParentLogger() {
        // requires JDK version 1.6
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    /**
     * 驱动代理
     * 不是很懂
     * 代理模型
     */
    @AllArgsConstructor
    private static class DriverProxy implements Driver {

        private Driver driver;

        @Override
        public boolean acceptsURL(String u) throws SQLException {
            return this.driver.acceptsURL(u);
        }

        @Override
        public Connection connect(String u, Properties p) throws SQLException {
            return this.driver.connect(u, p);
        }

        @Override
        public int getMajorVersion() {
            return this.driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return this.driver.getMinorVersion();
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
            return this.driver.getPropertyInfo(u, p);
        }

        @Override
        public boolean jdbcCompliant() {
            return this.driver.jdbcCompliant();
        }

        // @Override only valid jdk7+
        public Logger getParentLogger() {
            return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        }
    }
}
