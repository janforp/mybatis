package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public class ByteTypeHandler extends BaseTypeHandler<Byte> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int parameterIndex, Byte parameter, JdbcType jdbcType) throws SQLException {
        ps.setByte(parameterIndex, parameter);
    }

    @Override
    public Byte getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getByte(columnName);
    }

    @Override
    public Byte getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getByte(columnIndex);
    }

    @Override
    public Byte getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getByte(columnIndex);
    }
}
