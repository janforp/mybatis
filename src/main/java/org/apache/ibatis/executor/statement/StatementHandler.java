package org.apache.ibatis.executor.statement;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.ResultHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * @author Clinton Begin
 */

/**
 * 语句处理器
 */
public interface StatementHandler {

    //准备语句
    Statement prepare(Connection connection) throws SQLException;

    //参数化
    void parameterize(Statement statement) throws SQLException;

    //批处理
    void batch(Statement statement) throws SQLException;

    //update
    int update(Statement statement) throws SQLException;

    //select-->结果给ResultHandler
    <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException;

    //得到绑定sql
    BoundSql getBoundSql();

    //得到参数处理器
    ParameterHandler getParameterHandler();
}
