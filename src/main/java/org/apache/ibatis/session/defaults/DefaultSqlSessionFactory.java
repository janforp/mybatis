package org.apache.ibatis.session.defaults;

import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */

/**
 * 默认的SqlSessionFactory
 */
public class DefaultSqlSessionFactory implements SqlSessionFactory {

    private final Configuration configuration;

    public DefaultSqlSessionFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * SqlSession
     *
     * @param executorType 执行器类型
     * @param level 事务级别
     * @param autoCommit 是否自动提交
     * @return SqlSession
     */
    //为执行和数据库的交互，首先需要初始化SqlSession，通过DefaultSqlSessionFactory开启SqlSession
    private SqlSession openSessionFromDataSource(ExecutorType executorType, TransactionIsolationLevel level, boolean autoCommit) {
        Transaction transaction = null;
        try {
            final Environment environment = configuration.getEnvironment();
            final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
            //通过事务工厂来产生一个事务
            transaction = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
            //生成一个执行器(事务包含在执行器里)
            final Executor executor = configuration.newExecutor(transaction, executorType);
            //然后产生一个DefaultSqlSession
            return new DefaultSqlSession(configuration, executor, autoCommit);
        } catch (Exception e) {
            //如果打开事务出错，则关闭它
            closeTransaction(transaction); // may have fetched a connection so lets call close()
            throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
        } finally {
            //最后清空错误上下文
            ErrorContext.instance().reset();
        }
    }

    private SqlSession openSessionFromConnection(ExecutorType execType, Connection connection) {
        try {
            boolean autoCommit;
            try {
                autoCommit = connection.getAutoCommit();
            } catch (SQLException e) {
                // Failover to true, as most poor drivers
                // or databases won't support transactions
                autoCommit = true;
            }
            final Environment environment = configuration.getEnvironment();
            final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
            final Transaction tx = transactionFactory.newTransaction(connection);
            final Executor executor = configuration.newExecutor(tx, execType);
            return new DefaultSqlSession(configuration, executor, autoCommit);
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    //最终都会调用2种方法：openSessionFromDataSource,openSessionFromConnection
    //以下6个方法都会调用openSessionFromDataSource
    @Override
    public SqlSession openSession() {
        ExecutorType defaultExecutorType = configuration.getDefaultExecutorType();
        return openSessionFromDataSource(defaultExecutorType, null, false);
    }

    @Override
    public SqlSession openSession(boolean autoCommit) {
        return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, autoCommit);
    }

    @Override
    public SqlSession openSession(ExecutorType execType) {
        return openSessionFromDataSource(execType, null, false);
    }

    @Override
    public SqlSession openSession(TransactionIsolationLevel level) {
        return openSessionFromDataSource(configuration.getDefaultExecutorType(), level, false);
    }

    @Override
    public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
        return openSessionFromDataSource(execType, level, false);
    }

    @Override
    public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
        return openSessionFromDataSource(execType, null, autoCommit);
    }

    //以下2个方法都会调用openSessionFromConnection
    @Override
    public SqlSession openSession(Connection connection) {
        return openSessionFromConnection(configuration.getDefaultExecutorType(), connection);
    }

    @Override
    public SqlSession openSession(ExecutorType execType, Connection connection) {
        return openSessionFromConnection(execType, connection);
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * 获取 TransactionFactory
     *
     * @param environment 当前的环境
     * @return TransactionFactory
     */
    private TransactionFactory getTransactionFactoryFromEnvironment(Environment environment) {
        //如果没有配置事务工厂，则返回托管事务工厂
        if (environment == null || environment.getTransactionFactory() == null) {
            return new ManagedTransactionFactory();
        }
        return environment.getTransactionFactory();
    }

    private void closeTransaction(Transaction tx) {
        if (tx != null) {
            try {
                tx.close();
            } catch (SQLException ignore) {
                // Intentionally ignore. Prefer previous error.
            }
        }
    }
}
