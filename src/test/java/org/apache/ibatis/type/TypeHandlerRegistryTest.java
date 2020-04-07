package org.apache.ibatis.type;

import org.apache.ibatis.domain.misc.RichType;
import org.junit.Test;

import java.net.URI;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TypeHandlerRegistryTest {

    private TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();

    /**
     * 重新得到
     */
    @Test
    public void shouldRegisterAndRetrieveTypeHandler() {
        TypeHandler<String> stringTypeHandler = typeHandlerRegistry.getTypeHandler(String.class);
        typeHandlerRegistry.register(String.class, JdbcType.LONGVARCHAR, stringTypeHandler);
        assertEquals(stringTypeHandler, typeHandlerRegistry.getTypeHandler(String.class, JdbcType.LONGVARCHAR));

        assertTrue(typeHandlerRegistry.hasTypeHandler(String.class));
        assertFalse(typeHandlerRegistry.hasTypeHandler(RichType.class));
        assertTrue(typeHandlerRegistry.hasTypeHandler(String.class, JdbcType.LONGVARCHAR));
        assertTrue(typeHandlerRegistry.hasTypeHandler(String.class, JdbcType.INTEGER));
        assertTrue(typeHandlerRegistry.getUnknownTypeHandler() instanceof UnknownTypeHandler);
    }

    @Test
    public void shouldRegisterAndRetrieveComplexTypeHandler() {

        TypeHandler<List<URI>> fakeHandler = new TypeHandler<List<URI>>() {

            @Override
            public void setParameter(PreparedStatement preparedStatement, int i, List<URI> parameter, JdbcType jdbcType)
                    throws SQLException {
                // do nothing, fake method
            }

            @Override
            public List<URI> getResult(CallableStatement cs, int columnIndex)
                    throws SQLException {
                // do nothing, fake method
                return null;
            }

            @Override
            public List<URI> getResult(ResultSet rs, int columnIndex)
                    throws SQLException {
                // do nothing, fake method
                return null;
            }

            @Override
            public List<URI> getResult(ResultSet rs, String columnName)
                    throws SQLException {
                // do nothing, fake method
                return null;
            }

        };

        TypeReference<List<URI>> type = new TypeReference<List<URI>>() {
        };

        typeHandlerRegistry.register(type, fakeHandler);
        assertSame(fakeHandler, typeHandlerRegistry.getTypeHandler(type));
    }

    @Test
    public void shouldAutoRegisterAndRetrieveComplexTypeHandler() {
        TypeHandler<List<URI>> fakeHandler = new BaseTypeHandler<List<URI>>() {

            @Override
            public void setNonNullParameter(PreparedStatement ps, int parameterIndex, List<URI> parameter, JdbcType jdbcType)
                    throws SQLException {
                // do nothing, fake method
            }

            @Override
            public List<URI> getNullableResult(ResultSet rs, String columnName)
                    throws SQLException {
                // do nothing, fake method
                return null;
            }

            @Override
            public List<URI> getNullableResult(ResultSet rs, int columnIndex)
                    throws SQLException {
                // do nothing, fake method
                return null;
            }

            @Override
            public List<URI> getNullableResult(CallableStatement cs, int columnIndex)
                    throws SQLException {
                // do nothing, fake method
                return null;
            }

        };

        typeHandlerRegistry.register(fakeHandler);

        assertSame(fakeHandler, typeHandlerRegistry.getTypeHandler(new TypeReference<List<URI>>() {
        }));
    }

    @Test
    public void shouldBindHandlersToWrapersAndPrimitivesIndividually() {
        typeHandlerRegistry.register(Integer.class, DateTypeHandler.class);
        assertSame(IntegerTypeHandler.class, typeHandlerRegistry.getTypeHandler(int.class).getClass());
        typeHandlerRegistry.register(Integer.class, IntegerTypeHandler.class);
        typeHandlerRegistry.register(int.class, DateTypeHandler.class);
        assertSame(IntegerTypeHandler.class, typeHandlerRegistry.getTypeHandler(Integer.class).getClass());
        typeHandlerRegistry.register(Integer.class, IntegerTypeHandler.class);
    }
}
