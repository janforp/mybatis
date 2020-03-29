package org.apache.ibatis.type;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BigIntegerTypeHandlerTest extends BaseTypeHandlerTest {

    private static final TypeHandler<BigInteger> TYPE_HANDLER = new BigIntegerTypeHandler();

    @Test
    public void shouldSetParameter() throws Exception {
        TYPE_HANDLER.setParameter(preparedStatement, 1, new BigInteger("707070656505050302797979792923232303"), null);
        verify(preparedStatement).setBigDecimal(1, new BigDecimal("707070656505050302797979792923232303"));
    }

    @Test
    public void shouldGetResultFromResultSet() throws Exception {
        when(resultSet.getBigDecimal("column")).thenReturn(new BigDecimal("707070656505050302797979792923232303"));
        when(resultSet.wasNull()).thenReturn(false);
        assertEquals(new BigInteger("707070656505050302797979792923232303"), TYPE_HANDLER.getResult(resultSet, "column"));
    }

    @Test
    public void shouldGetResultFromCallableStatement() throws Exception {
        when(callableStatement.getBigDecimal(1)).thenReturn(new BigDecimal("707070656505050302797979792923232303"));
        when(callableStatement.wasNull()).thenReturn(false);
        assertEquals(new BigInteger("707070656505050302797979792923232303"), TYPE_HANDLER.getResult(callableStatement, 1));
    }
}
