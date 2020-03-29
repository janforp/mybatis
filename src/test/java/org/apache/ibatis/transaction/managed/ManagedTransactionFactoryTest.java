package org.apache.ibatis.transaction.managed;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Connection;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ManagedTransactionFactoryTest extends BaseDataTest {

    @Mock
    private Connection connection;

    @Test
    public void shouldEnsureThatCallsToManagedTransactionAPIDoNotForwardToManagedConnections() throws Exception {
        TransactionFactory managedTransactionFactory = new ManagedTransactionFactory();
        managedTransactionFactory.setProperties(new Properties());
        //ManagedTransaction 实例
        Transaction transaction = managedTransactionFactory.newTransaction(connection);
        assertEquals(connection, transaction.getConnection());
        transaction.commit();
        transaction.rollback();
        transaction.close();
        //默认会关闭连接
        verify(connection).close();
    }

    @Test
    public void shouldEnsureThatCallsToManagedTransactionAPIDoNotForwardToManagedConnectionsAndDoesNotCloseConnection() throws Exception {
        TransactionFactory tf = new ManagedTransactionFactory();
        Properties props = new Properties();
        //当配置了 closeConnection = "false" 的时候，则调用 事务关闭的时候，就不会关闭连接
        props.setProperty("closeConnection", "false");
        tf.setProperties(props);
        //ManagedTransaction 实例
        Transaction tx = tf.newTransaction(connection);
        assertEquals(connection, tx.getConnection());
        tx.commit();
        tx.rollback();
        tx.close();
        verifyNoMoreInteractions(connection);
    }
}
