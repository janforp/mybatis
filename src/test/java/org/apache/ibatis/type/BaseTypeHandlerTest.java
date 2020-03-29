package org.apache.ibatis.type;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

@RunWith(MockitoJUnitRunner.class)
public abstract class BaseTypeHandlerTest {

    @Mock
    protected ResultSet resultSet;

    @Mock
    protected PreparedStatement preparedStatement;

    @Mock
    protected CallableStatement callableStatement;

    @Mock
    protected ResultSetMetaData resultSetMetaData;

    public abstract void shouldSetParameter() throws Exception;

    public abstract void shouldGetResultFromResultSet() throws Exception;

    public abstract void shouldGetResultFromCallableStatement() throws Exception;
}
