package org.apache.ibatis.type;

import org.junit.Test;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public class GenericTypeSupportedInHierarchiesTestCase {

    @Test
    public void detectsTheGenericTypeTraversingTheHierarchy() {
        Type rawType = new CustomStringTypeHandler().getRawType();
        assertEquals(String.class, rawType);
    }

    public static final class CustomStringTypeHandler extends StringTypeHandler {

        /**
         * Defined as reported in #581
         */
        @Override
        public void setNonNullParameter(PreparedStatement preparedStatement, int parameterIndex, String parameter, JdbcType jdbcType) throws SQLException {
            // do something
            super.setNonNullParameter(preparedStatement, parameterIndex, parameter, jdbcType);
        }
    }
}
