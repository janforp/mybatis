package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */

/**
 * Integer类型处理器
 * 调用PreparedStatement.setInt, ResultSet.getInt, CallableStatement.getInt
 */
public class IntegerTypeHandler extends BaseTypeHandler<Integer> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int parameterIndex, Integer parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(parameterIndex, parameter);
    }

    @Override
    public Integer getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        return rs.getInt(columnName);
    }

    @Override
    public Integer getNullableResult(ResultSet rs, int columnIndex)
            throws SQLException {
        return rs.getInt(columnIndex);
    }

    @Override
    public Integer getNullableResult(CallableStatement cs, int columnIndex)
            throws SQLException {
        return cs.getInt(columnIndex);
    }
}
