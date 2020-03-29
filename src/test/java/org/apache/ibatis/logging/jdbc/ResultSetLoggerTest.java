

package org.apache.ibatis.logging.jdbc;

import org.apache.ibatis.logging.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResultSetLoggerTest {

    @Mock
    private ResultSet rs;

    @Mock
    private Log log;

    @Mock
    private ResultSetMetaData metaData;

    public void setup(int type) throws SQLException {
        when(rs.next()).thenReturn(true);
        when(rs.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnType(1)).thenReturn(type);
        when(metaData.getColumnLabel(1)).thenReturn("ColumnName");
        when(rs.getString(1)).thenReturn("value");
        when(log.isTraceEnabled()).thenReturn(true);
        ResultSet resultSet = ResultSetLogger.newInstance(rs, log, 1);
        resultSet.next();
    }

    @Test
    public void shouldNotPrintBlobs() throws SQLException {
        setup(Types.LONGNVARCHAR);
        verify(log).trace("<==    Columns: ColumnName");
        verify(log).trace("<==        Row: <<BLOB>>");
    }

    @Test
    public void shouldPrintVarchars() throws SQLException {
        setup(Types.VARCHAR);
        verify(log).trace("<==    Columns: ColumnName");
        verify(log).trace("<==        Row: value");
    }

}
