package org.apache.ibatis.transaction.managed;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

/**
 * Creates {@link ManagedTransaction} instances.
 * 托管事务工厂
 * 默认 情况下它会关闭连接。
 * 然而一些容器并不希望这样, 因此如果你需要从连接中停止 它,将 closeConnection 属性设置为 false。
 */
public class ManagedTransactionFactory implements TransactionFactory {

    /**
     * 是否需要关闭连接
     */
    private boolean closeConnection = true;

    @Override
    public void setProperties(Properties props) {
        //设置closeConnection属性
        if (props != null) {
            String closeConnectionProperty = props.getProperty("closeConnection");
            if (closeConnectionProperty != null) {
                closeConnection = Boolean.parseBoolean(closeConnectionProperty);
            }
        }
    }

    @Override
    public Transaction newTransaction(Connection conn) {
        return new ManagedTransaction(conn, closeConnection);
    }

    @Override
    public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
        // Silently ignores autocommit and isolation level, as managed transactions are entirely
        // controlled by an external manager.  It's silently ignored so that
        // code remains portable between managed and unmanaged configurations.
        return new ManagedTransaction(dataSource, level, closeConnection);
    }
}
