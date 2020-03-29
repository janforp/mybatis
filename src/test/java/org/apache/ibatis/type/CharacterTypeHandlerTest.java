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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CharacterTypeHandlerTest extends BaseTypeHandlerTest {

    private static final TypeHandler<Character> TYPE_HANDLER = new CharacterTypeHandler();

    @Test
    public void shouldSetParameter() throws Exception {
        TYPE_HANDLER.setParameter(preparedStatement, 1, 'a', null);
        verify(preparedStatement).setString(1, "a");
    }

    @Test
    public void shouldSetNullParameter() throws Exception {
        TYPE_HANDLER.setParameter(preparedStatement, 1, null, JdbcType.VARCHAR);
        verify(preparedStatement).setNull(1, JdbcType.VARCHAR.TYPE_CODE);
    }

    @Test
    public void shouldGetResultFromResultSet() throws Exception {
        when(resultSet.getString("column")).thenReturn("a");
        when(resultSet.wasNull()).thenReturn(false);
        assertEquals(new Character('a'), TYPE_HANDLER.getResult(resultSet, "column"));
    }

    @Test
    public void shouldGetNullResultFromResultSet() throws Exception {
        when(resultSet.getString("column")).thenReturn(null);
        when(resultSet.wasNull()).thenReturn(true);
        assertEquals(null, TYPE_HANDLER.getResult(resultSet, "column"));
    }

    @Test
    public void shouldGetResultFromCallableStatement() throws Exception {
        when(callableStatement.getString(1)).thenReturn("a");
        when(callableStatement.wasNull()).thenReturn(false);
        assertEquals(new Character('a'), TYPE_HANDLER.getResult(callableStatement, 1));
    }

    @Test
    public void shouldGetNullResultFromCallableStatement() throws Exception {
        when(callableStatement.getString("column")).thenReturn(null);
        when(callableStatement.wasNull()).thenReturn(true);
        assertEquals(null, TYPE_HANDLER.getResult(callableStatement, 1));
    }

}
