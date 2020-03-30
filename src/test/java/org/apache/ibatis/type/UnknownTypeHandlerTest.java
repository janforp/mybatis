package org.apache.ibatis.type;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UnknownTypeHandlerTest extends BaseTypeHandlerTest {

    private static final TypeHandler<Object> TYPE_HANDLER = new UnknownTypeHandler(new TypeHandlerRegistry());

    @Test
    public void shouldSetParameter() throws Exception {
        TYPE_HANDLER.setParameter(preparedStatement, 1, "Hello", null);
        verify(preparedStatement).setString(1, "Hello");
    }

    @Test
    public void shouldGetResultFromResultSet() throws Exception {
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnName(1)).thenReturn("column");
        when(resultSetMetaData.getColumnClassName(1)).thenReturn(String.class.getName());
        when(resultSetMetaData.getColumnType(1)).thenReturn(JdbcType.VARCHAR.TYPE_CODE);
        when(resultSet.getString("column")).thenReturn("Hello");
        when(resultSet.wasNull()).thenReturn(false);
        assertEquals("Hello", TYPE_HANDLER.getResult(resultSet, "column"));
    }

    @Test
    public void shouldGetResultFromCallableStatement() throws Exception {
        when(callableStatement.getObject(1)).thenReturn("Hello");
        when(callableStatement.wasNull()).thenReturn(false);
        assertEquals("Hello", TYPE_HANDLER.getResult(callableStatement, 1));
    }
}