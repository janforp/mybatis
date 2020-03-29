package org.apache.ibatis.type;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BigDecimalTypeHandlerTest extends BaseTypeHandlerTest {

    private static final TypeHandler<BigDecimal> TYPE_HANDLER = new BigDecimalTypeHandler();

    @Test
    public void shouldSetParameter() throws Exception {
        TYPE_HANDLER.setParameter(preparedStatement, 1, new BigDecimal(1), null);
        verify(preparedStatement).setBigDecimal(1, new BigDecimal(1));
    }

    @Test
    public void shouldGetResultFromResultSet() throws Exception {
        when(resultSet.getBigDecimal("column")).thenReturn(new BigDecimal(1));
        when(resultSet.wasNull()).thenReturn(false);
        assertEquals(new BigDecimal(1), TYPE_HANDLER.getResult(resultSet, "column"));
    }

    @Test
    public void shouldGetResultFromCallableStatement() throws Exception {
        when(callableStatement.getBigDecimal(1)).thenReturn(new BigDecimal(1));
        when(callableStatement.wasNull()).thenReturn(false);
        assertEquals(new BigDecimal(1), TYPE_HANDLER.getResult(callableStatement, 1));
    }
}
