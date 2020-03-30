package org.apache.ibatis.executor.resultset;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 结果集处理器
 */
public interface ResultSetHandler {

    /**
     * 处理结果集
     *
     * @param statement statement
     * @param <E> 结果范型
     * @return 查询结果
     * @throws SQLException 异常
     */
    <E> List<E> handleResultSets(Statement statement) throws SQLException;

    /**
     * 处理OUT参数,只有存储过程采用
     *
     * <mapper namespace="mybatis2.Tssss">
     * <select id="getCount" resultType="hashmap" statementType="CALLABLE">
     * {call
     * ges_user_count(#{sex_id,mode=IN,jdbcType=INTEGER},#{result,mode=OUT,jdbcType=INTEGER})
     * }
     * </select>
     * </mapper>
     *
     * @param callableStatement 存储过程的statement
     * @throws SQLException 异常
     */
    void handleOutputParameters(CallableStatement callableStatement) throws SQLException;
}
