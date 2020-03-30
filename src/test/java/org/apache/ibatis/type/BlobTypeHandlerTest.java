package org.apache.ibatis.type;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.InputStream;
import java.sql.Blob;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BlobTypeHandlerTest extends BaseTypeHandlerTest {

    private static final TypeHandler<byte[]> TYPE_HANDLER = new BlobTypeHandler();

    @Mock
    protected Blob blob;

    @Test
    public void shouldSetParameter() throws Exception {
        TYPE_HANDLER.setParameter(preparedStatement, 1, new byte[] { 1, 2, 3 }, null);
        //TODO ？
        verify(preparedStatement).setBinaryStream(Mockito.eq(1), Mockito.any(InputStream.class), Mockito.eq(3));
    }

    @Test
    public void shouldGetResultFromResultSet() throws Exception {
        when(resultSet.getBlob("column")).thenReturn(blob);
        when(resultSet.wasNull()).thenReturn(false);
        when(blob.length()).thenReturn(3L);
        when(blob.getBytes(1, 3)).thenReturn(new byte[] { 1, 2, 3 });
        assertArrayEquals(new byte[] { 1, 2, 3 }, TYPE_HANDLER.getResult(resultSet, "column"));
    }

    @Test
    public void shouldGetResultFromCallableStatement() throws Exception {
        when(callableStatement.getBlob(1)).thenReturn(blob);
        when(callableStatement.wasNull()).thenReturn(false);
        when(blob.length()).thenReturn(3L);
        when(blob.getBytes(1, 3)).thenReturn(new byte[] { 1, 2, 3 });
        assertArrayEquals(new byte[] { 1, 2, 3 }, TYPE_HANDLER.getResult(callableStatement, 1));
    }
}
