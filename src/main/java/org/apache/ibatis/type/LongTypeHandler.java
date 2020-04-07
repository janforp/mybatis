package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public class LongTypeHandler extends BaseTypeHandler<Long> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int parameterIndex, Long parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setLong(parameterIndex, parameter);
    }

    @Override
    public Long getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        return rs.getLong(columnName);
    }

    @Override
    public Long getNullableResult(ResultSet rs, int columnIndex)
            throws SQLException {
        return rs.getLong(columnIndex);
    }

    @Override
    public Long getNullableResult(CallableStatement cs, int columnIndex)
            throws SQLException {
        return cs.getLong(columnIndex);
    }
}
