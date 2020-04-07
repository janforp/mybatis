package org.apache.ibatis.type;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public class BlobByteObjectArrayTypeHandler extends BaseTypeHandler<Byte[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int parameterIndex, Byte[] parameter, JdbcType jdbcType) throws SQLException {
        byte[] bytes = ByteArrayUtils.convertToPrimitiveArray(parameter);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

        ps.setBinaryStream(parameterIndex, bis, parameter.length);
    }

    @Override
    public Byte[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Blob blob = rs.getBlob(columnName);
        return getBytes(blob);
    }

    @Override
    public Byte[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Blob blob = rs.getBlob(columnIndex);
        return getBytes(blob);
    }

    @Override
    public Byte[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Blob blob = cs.getBlob(columnIndex);
        return getBytes(blob);
    }

    private Byte[] getBytes(Blob blob) throws SQLException {
        Byte[] returnValue = null;
        if (blob != null) {
            returnValue = ByteArrayUtils.convertToObjectArray(blob.getBytes(1, (int) blob.length()));
        }
        return returnValue;
    }
}
