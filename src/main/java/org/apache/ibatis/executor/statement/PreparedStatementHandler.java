package org.apache.ibatis.executor.statement;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * @author Clinton Begin
 */

/**
 * 预处理语句处理器(PREPARED)
 */
public class PreparedStatementHandler extends BaseStatementHandler {

    public PreparedStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
    }

    @Override
    public int update(Statement statement) throws SQLException {
        //调用PreparedStatement.execute和PreparedStatement.getUpdateCount
        PreparedStatement preparedStatement = (PreparedStatement) statement;
        //执行
        preparedStatement.execute();
        //修改行，the current result as an update count; -1 if the current result is a
        int rows = preparedStatement.getUpdateCount();
        Object parameterObject = boundSql.getParameterObject();
        KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
        keyGenerator.processAfter(executor, mappedStatement, preparedStatement, parameterObject);
        return rows;
    }

    @Override
    public void batch(Statement statement) throws SQLException {
        PreparedStatement preparedStatement = (PreparedStatement) statement;
        preparedStatement.addBatch();
    }

    @Override
    public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
        PreparedStatement preparedStatement = (PreparedStatement) statement;
        preparedStatement.execute();//执行数据库查询，之后结果就以及在 preparedStatement 中了，需要通结果处理器处理
        List<E> resultSetList = resultSetHandler.handleResultSets(preparedStatement);
        System.out.println(resultSetList);
        return resultSetList;
    }

    @Override
    protected Statement instantiateStatement(Connection connection) throws SQLException {
        //调用Connection.prepareStatement
        String sql = boundSql.getSql();
        //TODO
        KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
        ResultSetType resultSetType = mappedStatement.getResultSetType();
        if (keyGenerator instanceof Jdbc3KeyGenerator) {
            String[] keyColumnNames = mappedStatement.getKeyColumns();
            if (keyColumnNames == null) {
                return connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            } else {
                return connection.prepareStatement(sql, keyColumnNames);
            }
        } else if (resultSetType != null) {
            int value = resultSetType.getValue();
            return connection.prepareStatement(sql, value, ResultSet.CONCUR_READ_ONLY);
        } else {
            return connection.prepareStatement(sql);
        }
    }

    @Override
    public void parameterize(Statement statement) throws SQLException {
        //调用ParameterHandler.setParameters
        parameterHandler.setParameters((PreparedStatement) statement);
    }
}
