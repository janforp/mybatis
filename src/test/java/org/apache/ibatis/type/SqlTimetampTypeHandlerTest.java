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

import java.sql.Timestamp;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SqlTimetampTypeHandlerTest extends BaseTypeHandlerTest {

    private static final TypeHandler<Timestamp> TYPE_HANDLER = new SqlTimestampTypeHandler();

    private static final java.sql.Timestamp SQL_TIME = new java.sql.Timestamp(new Date().getTime());

    @Test
    public void shouldSetParameter() throws Exception {
        TYPE_HANDLER.setParameter(preparedStatement, 1, SQL_TIME, null);
        verify(preparedStatement).setTimestamp(1, SQL_TIME);
    }

    @Test
    public void shouldGetResultFromResultSet() throws Exception {
        when(resultSet.getTimestamp("column")).thenReturn(SQL_TIME);
        when(resultSet.wasNull()).thenReturn(false);
        assertEquals(SQL_TIME, TYPE_HANDLER.getResult(resultSet, "column"));
    }

    @Test
    public void shouldGetResultFromCallableStatement() throws Exception {
        when(callableStatement.getTimestamp(1)).thenReturn(SQL_TIME);
        when(callableStatement.wasNull()).thenReturn(false);
        assertEquals(SQL_TIME, TYPE_HANDLER.getResult(callableStatement, 1));
    }

}