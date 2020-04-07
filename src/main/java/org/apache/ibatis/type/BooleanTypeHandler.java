package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public class BooleanTypeHandler extends BaseTypeHandler<Boolean> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int parameterIndex, Boolean parameter, JdbcType jdbcType) throws SQLException {
        ps.setBoolean(parameterIndex, parameter);
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getBoolean(columnName);
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getBoolean(columnIndex);
    }

    @Override
    public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getBoolean(columnIndex);
    }
}
