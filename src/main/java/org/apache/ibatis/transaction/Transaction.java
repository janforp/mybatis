package org.apache.ibatis.transaction;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wraps a database connection.
 * Handles the connection lifecycle that comprises: its creation, preparation, commit/rollback and close.
 * 事务，包装了一个Connection, 包含commit,rollback,close方法
 * 在 MyBatis 中有两种事务管理器类型(也就是 type=”[JDBC|MANAGED]”):
 *
 * 事务当然是连接的事务，所以事务会封装包含了该数据库链接
 *
 * @author Clinton Begin
 */
public interface Transaction {

    /**
     * Retrieve inner database connection
     *
     * @return DataBase connection
     * @throws SQLException 异常
     */
    Connection getConnection() throws SQLException;

    /**
     * Commit inner database connection.
     *
     * @throws SQLException 异常
     */
    void commit() throws SQLException;

    /**
     * Rollback inner database connection.
     *
     * @throws SQLException 异常
     */
    void rollback() throws SQLException;

    /**
     * Close inner database connection.
     *
     * @throws SQLException 异常 异常
     */
    void close() throws SQLException;
}
