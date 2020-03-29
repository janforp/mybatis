/*
 *    Copyright 2009-2012 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.apache.ibatis.type;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DateOnlyTypeHandlerTest extends BaseTypeHandlerTest {

    private static final TypeHandler<Date> TYPE_HANDLER = new DateOnlyTypeHandler();

    private static final Date DATE = new Date();

    private static final java.sql.Date SQL_DATE = new java.sql.Date(DATE.getTime());

    @Test
    public void shouldSetParameter() throws Exception {
        TYPE_HANDLER.setParameter(preparedStatement, 1, DATE, null);
        verify(preparedStatement).setDate(1, new java.sql.Date(DATE.getTime()));
    }

    @Test
    public void shouldGetResultFromResultSet() throws Exception {
        when(resultSet.getDate("column")).thenReturn(SQL_DATE);
        when(resultSet.wasNull()).thenReturn(false);
        assertEquals(DATE, TYPE_HANDLER.getResult(resultSet, "column"));
    }

    @Test
    public void shouldGetResultFromCallableStatement() throws Exception {
        when(callableStatement.getDate(1)).thenReturn(SQL_DATE);
        when(callableStatement.wasNull()).thenReturn(false);
        assertEquals(DATE, TYPE_HANDLER.getResult(callableStatement, 1));
    }

}