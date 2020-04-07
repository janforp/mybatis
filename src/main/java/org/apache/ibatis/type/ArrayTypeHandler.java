package org.apache.ibatis.type;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public class ArrayTypeHandler extends BaseTypeHandler<Object> {

    public ArrayTypeHandler() {
        super();
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int parameterIndex, Object parameter, JdbcType jdbcType) throws SQLException {
        ps.setArray(parameterIndex, (Array) parameter);
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Array array = rs.getArray(columnName);
        return array == null ? null : array.getArray();
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Array array = rs.getArray(columnIndex);
        return array == null ? null : array.getArray();
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Array array = cs.getArray(columnIndex);
        return array == null ? null : array.getArray();
    }

}
