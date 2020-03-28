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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Clinton Begin
 */

/**
 * 可重用的执行器
 * 其实就是缓存
 */
public class ReuseExecutor extends BaseExecutor {

    /**
     * 可重用的执行器内部用了一个map，用来缓存SQL语句对应的Statement
     * key:Sql
     * value:statement
     */
    private final Map<String, Statement> statementMap = new HashMap<String, Statement>();

    public ReuseExecutor(Configuration configuration, Transaction transaction) {
        super(configuration, transaction);
    }

    @Override
    public int doUpdate(MappedStatement mappedStatement, Object parameter) throws SQLException {
        Configuration configuration = mappedStatement.getConfiguration();
        //和SimpleExecutor一样，新建一个StatementHandler
        //这里看到ResultHandler传入的是null
        StatementHandler handler = configuration.newStatementHandler(this, mappedStatement, parameter, RowBounds.DEFAULT, null, null);
        //准备语句
        Statement stmt = prepareStatement(handler, mappedStatement.getStatementLog());
        return handler.update(stmt);
    }

    @Override
    public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        Configuration configuration = ms.getConfiguration();
        StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
        Statement stmt = prepareStatement(handler, ms.getStatementLog());
        return handler.query(stmt, resultHandler);
    }

    @Override
    public List<BatchResult> doFlushStatements(boolean isRollback) {
        for (Statement stmt : statementMap.values()) {
            closeStatement(stmt);
        }
        statementMap.clear();
        return Collections.emptyList();
    }

    private Statement prepareStatement(StatementHandler statementHandler, Log statementLog) throws SQLException {
        Statement statement;
        //得到绑定的SQL语句
        BoundSql boundSql = statementHandler.getBoundSql();
        String sql = boundSql.getSql();
        //如果缓存中已经有了，直接得到Statement
        //缓存命中
        boolean hasStatementFor = hasStatementFor(sql);
        if (hasStatementFor) {
            statement = getStatement(sql);
        } else {
            //如果缓存没有找到，则和SimpleExecutor处理完全一样，然后加入缓存
            Connection connection = getConnection(statementLog);
            statement = statementHandler.prepare(connection);
            //丢进缓存
            putStatement(sql, statement);
        }
        statementHandler.parameterize(statement);
        return statement;
    }

    private boolean hasStatementFor(String sql) {
        try {
            boolean containsSql = statementMap.keySet().contains(sql);
            boolean notClosed = !statementMap.get(sql).getConnection().isClosed();
            //statement存在，并且没有关闭，则true
            return containsSql && notClosed;
        } catch (SQLException e) {
            return false;
        }
    }

    private Statement getStatement(String sql) {
        return statementMap.get(sql);
    }

    private void putStatement(String sql, Statement statement) {
        statementMap.put(sql, statement);
    }
}
