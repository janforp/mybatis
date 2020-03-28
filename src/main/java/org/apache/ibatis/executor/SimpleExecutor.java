package org.apache.ibatis.executor;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

/**
 * @author Clinton Begin
 */

/**
 * 简单执行器
 */
public class SimpleExecutor extends BaseExecutor {

    public SimpleExecutor(Configuration configuration, Transaction transaction) {
        super(configuration, transaction);
    }

    //update
    @Override
    public int doUpdate(MappedStatement mappedStatement, Object parameter) throws SQLException {
        Statement statement = null;
        try {
            Configuration configuration = mappedStatement.getConfiguration();
            //新建一个StatementHandler
            //这里看到ResultHandler传入的是null
            StatementHandler handler = configuration.newStatementHandler(this, mappedStatement, parameter, RowBounds.DEFAULT, null, null);
            //准备语句
            statement = prepareStatement(handler, mappedStatement.getStatementLog());
            //StatementHandler.update
            return handler.update(statement);
        } finally {
            closeStatement(statement);
        }
    }

    //select
    @Override
    public <E> List<E> doQuery(MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        Statement statement = null;
        try {
            Configuration configuration = mappedStatement.getConfiguration();
            //新建一个StatementHandler
            //这里看到ResultHandler传入了
            StatementHandler handler = configuration.newStatementHandler(wrapper, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
            //准备语句
            statement = prepareStatement(handler, mappedStatement.getStatementLog());
            //StatementHandler.query
            return handler.<E>query(statement, resultHandler);
        } finally {
            closeStatement(statement);
        }
    }

    @Override
    public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
        //doFlushStatements只是给batch用的，所以这里返回空
        return Collections.emptyList();
    }

    /**
     * 准备 Statement ，步骤：getConnection -》prepare -》parameterize
     *
     * @param statementHandler Statement处理器
     * @param statementLog log
     * @return Statement
     * @throws SQLException sql异常
     */
    private Statement prepareStatement(StatementHandler statementHandler, Log statementLog) throws SQLException {
        Statement statement;
        Connection connection = getConnection(statementLog);
        //调用StatementHandler.prepare
        statement = statementHandler.prepare(connection);
        //调用StatementHandler.parameterize
        statementHandler.parameterize(statement);
        return statement;
    }
}
