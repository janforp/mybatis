package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public class SqlDateTypeHandler extends BaseTypeHandler<Date> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int parameterIndex, Date parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setDate(parameterIndex, parameter);
    }

    @Override
    public Date getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        return rs.getDate(columnName);
    }

    @Override
    public Date getNullableResult(ResultSet rs, int columnIndex)
            throws SQLException {
        return rs.getDate(columnIndex);
    }

    @Override
    public Date getNullableResult(CallableStatement cs, int columnIndex)
            throws SQLException {
        return cs.getDate(columnIndex);
    }
}
