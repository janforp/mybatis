package org.apache.ibatis.executor.statement;

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 语句处理器的基类
 * 模版方法模型，一般的套路逻辑在该类实现，留给子类一个模版方法：instantiateStatement
 */
public abstract class BaseStatementHandler implements StatementHandler {

    protected final Configuration configuration;

    protected final ObjectFactory objectFactory;

    protected final TypeHandlerRegistry typeHandlerRegistry;

    /**
     * 结果处理器
     */
    protected final ResultSetHandler resultSetHandler;

    /**
     * 参数处理器
     */
    protected final ParameterHandler parameterHandler;

    /**
     * 执行器
     */
    protected final Executor executor;

    /**
     * 映射语句详情
     */
    protected final MappedStatement mappedStatement;

    protected final RowBounds rowBounds;

    protected BoundSql boundSql;

    /**
     * 如何实例化Statement，交给子类做，有3个实现
     *
     * @param connection 数据库连接
     * @return Statement
     */
    protected abstract Statement instantiateStatement(Connection connection) throws SQLException;

    protected BaseStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        this.configuration = mappedStatement.getConfiguration();
        this.executor = executor;
        this.mappedStatement = mappedStatement;
        this.rowBounds = rowBounds;

        this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        this.objectFactory = configuration.getObjectFactory();

        if (boundSql == null) { // issue #435, get the key before calculating the statement
            generateKeys(parameterObject);
            boundSql = mappedStatement.getBoundSql(parameterObject);
        }

        this.boundSql = boundSql;

        //生成parameterHandler
        this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
        //生成resultSetHandler
        this.resultSetHandler = configuration.newResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler, resultHandler, boundSql);
    }

    @Override
    public BoundSql getBoundSql() {
        return boundSql;
    }

    @Override
    public ParameterHandler getParameterHandler() {
        return parameterHandler;
    }

    //准备语句
    @Override
    public Statement prepare(Connection connection) throws SQLException {
        ErrorContext.instance().sql(boundSql.getSql());
        Statement statement = null;
        try {
            //实例化Statement
            //模版方法
            statement = instantiateStatement(connection);
            //设置超时
            setStatementTimeout(statement);
            //设置读取条数
            setFetchSize(statement);
            return statement;
        } catch (SQLException e) {
            closeStatement(statement);
            throw e;
        } catch (Exception e) {
            closeStatement(statement);
            throw new ExecutorException("Error preparing statement.  Cause: " + e, e);
        }
    }

    //设置超时,其实就是调用Statement.setQueryTimeout
    protected void setStatementTimeout(Statement statement) throws SQLException {
        Integer timeout = mappedStatement.getTimeout();
        Integer defaultTimeout = configuration.getDefaultStatementTimeout();
        if (timeout != null) {
            statement.setQueryTimeout(timeout);
        } else if (defaultTimeout != null) {
            statement.setQueryTimeout(defaultTimeout);
        }
    }

    //设置读取条数,其实就是调用Statement.setFetchSize
    protected void setFetchSize(Statement statement) throws SQLException {
        Integer fetchSize = mappedStatement.getFetchSize();
        if (fetchSize != null) {
            statement.setFetchSize(fetchSize);
        }
    }

    //关闭语句
    protected void closeStatement(Statement statement) {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            //ignore
        }
    }

    /**
     * 生成key
     *
     * @param parameter 参数
     */
    protected void generateKeys(Object parameter) {
        KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();

        ErrorContext.instance().store();

        keyGenerator.processBefore(executor, mappedStatement, null, parameter);

        ErrorContext.instance().recall();
    }
}
