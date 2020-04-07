package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public class ByteObjectArrayTypeHandler extends BaseTypeHandler<Byte[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int parameterIndex, Byte[] parameter, JdbcType jdbcType) throws SQLException {
        //数据库接收的是基本类型
        byte[] bytes = ByteArrayUtils.convertToPrimitiveArray(parameter);
        ps.setBytes(parameterIndex, bytes);
    }

    @Override
    public Byte[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        byte[] bytes = rs.getBytes(columnName);
        return getBytes(bytes);
    }

    @Override
    public Byte[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        byte[] bytes = rs.getBytes(columnIndex);
        return getBytes(bytes);
    }

    @Override
    public Byte[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        byte[] bytes = cs.getBytes(columnIndex);
        return getBytes(bytes);
    }

    private Byte[] getBytes(byte[] bytes) {
        Byte[] returnValue = null;
        if (bytes != null) {
            returnValue = ByteArrayUtils.convertToObjectArray(bytes);
        }
        return returnValue;
    }

}
