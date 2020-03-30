package org.apache.ibatis.type;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TimeOnlyTypeHandlerTest extends BaseTypeHandlerTest {

    private static final TypeHandler<Date> TYPE_HANDLER = new TimeOnlyTypeHandler();

    private static final Date DATE = new Date();

    private static final java.sql.Time SQL_TIME = new java.sql.Time(DATE.getTime());

    @Test
    public void shouldSetParameter() throws Exception {
        TYPE_HANDLER.setParameter(preparedStatement, 1, DATE, null);
        verify(preparedStatement).setTime(1, SQL_TIME);
    }

    @Test
    public void shouldGetResultFromResultSet() throws Exception {
        when(resultSet.getTime("column")).thenReturn(SQL_TIME);
        when(resultSet.wasNull()).thenReturn(false);
        assertEquals(DATE, TYPE_HANDLER.getResult(resultSet, "column"));
    }

    @Test
    public void shouldGetResultFromCallableStatement() throws Exception {
        when(callableStatement.getTime(1)).thenReturn(SQL_TIME);
        when(callableStatement.wasNull()).thenReturn(false);
        assertEquals(DATE, TYPE_HANDLER.getResult(callableStatement, 1));
    }

}