package org.apache.ibatis.datasource.pooled;

import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.reflection.ExceptionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */

/**
 * 池化的连接
 */
class PooledConnection implements InvocationHandler {

    private static final String CLOSE = "close";

    private static final Class<?>[] IFACES = new Class<?>[] { Connection.class };

    private int hashCode = 0;

    private PooledDataSource dataSource;

    /**
     * the *real* connection that this wraps
     */
    @Getter
    private Connection realConnection;

    //代理的连接
    @Getter
    private Connection proxyConnection;

    /**
     * the timestamp that this connection was checked out
     */
    @Getter
    @Setter
    private long checkoutTimestamp;

    /**
     * the time that the connection was created
     */
    @Getter
    @Setter
    private long createdTimestamp;

    /**
     * the time that the connection was last used
     */
    @Getter
    @Setter
    private long lastUsedTimestamp;

    /**
     * connection type (based on url + user + password)
     */
    @Getter
    @Setter
    private int connectionTypeCode;

    private boolean valid;

    /*
     * Constructor for SimplePooledConnection that uses the Connection and PooledDataSource passed in
     *
     * @param connection - the connection that is to be presented as a pooled connection
     * @param dataSource - the dataSource that the connection is from
     */
    public PooledConnection(Connection connection, PooledDataSource dataSource) {
        this.hashCode = connection.hashCode();
        this.realConnection = connection;
        this.dataSource = dataSource;
        this.createdTimestamp = System.currentTimeMillis();
        this.lastUsedTimestamp = System.currentTimeMillis();
        this.valid = true;
        this.proxyConnection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), IFACES, this);
    }

    /*
     * Invalidates the connection
     */
    public void invalidate() {
        valid = false;
    }

    /*
     * Method to see if the connection is usable
     *
     * @return True if the connection is usable
     */
    public boolean isValid() {
        return valid && realConnection != null && dataSource.pingConnection(this);
    }

    /*
     * Gets the hashcode of the real connection (or 0 if it is null)
     *
     * @return The hashcode of the real connection (or 0 if it is null)
     */
    public int getRealHashCode() {
        return realConnection == null ? 0 : realConnection.hashCode();
    }

    /*
     * Getter for the time since this connection was last used
     *
     * @return - the time since the last use
     */
    public long getTimeElapsedSinceLastUse() {
        return System.currentTimeMillis() - lastUsedTimestamp;
    }

    /*
     * Getter for the age of the connection
     *
     * @return the age
     */
    public long getAge() {
        return System.currentTimeMillis() - createdTimestamp;
    }

    /*
     * Getter for the time that this connection has been checked out
     *
     * @return the time
     */
    public long getCheckoutTime() {
        return System.currentTimeMillis() - checkoutTimestamp;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /*
     * Allows comparing this connection to another
     *
     * @param obj - the other connection to test for equality
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PooledConnection) {
            return realConnection.hashCode() == (((PooledConnection) obj).realConnection.hashCode());
        } else if (obj instanceof Connection) {
            return hashCode == obj.hashCode();
        } else {
            return false;
        }
    }

    /**
     * Required for InvocationHandler implementation.
     *
     * @param proxy  - not used
     * @param method - the method to be executed
     * @param args   - the parameters to be passed to the method
     * @see java.lang.reflect.InvocationHandler#invoke(Object, java.lang.reflect.Method, Object[])
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        //如果调用close的话，忽略它，反而将这个connection加入到池中
        if (CLOSE.hashCode() == methodName.hashCode() && CLOSE.equals(methodName)) {
            dataSource.pushConnection(this);
            return null;
        } else {
            try {
                Class<?> declaringClass = method.getDeclaringClass();
                if (!Object.class.equals(declaringClass)) {
                    // issue #579 toString() should never fail
                    // throw an SQLException instead of a Runtime
                    //除了toString()方法，其他方法调用之前要检查connection是否还是合法的,不合法要抛出SQLException
                    checkConnection();
                }
                //其他的方法，则交给真正的connection去调用
                return method.invoke(realConnection, args);
            } catch (Throwable t) {
                throw ExceptionUtil.unwrapThrowable(t);
            }
        }
    }

    private void checkConnection() throws SQLException {
        if (!valid) {
            throw new SQLException("Error accessing PooledConnection. Connection is invalid.");
        }
    }
}
