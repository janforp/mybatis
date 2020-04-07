package org.apache.ibatis.submitted.autodiscover.handlers;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * @version $Id: MyBatisSampleTest.java 2697 2010-10-14 13:04:41Z eduardo.macarron $
 */
@MappedTypes(BigInteger.class)
public class DummyTypeHandler implements TypeHandler<Object> {

    @Override
    public void setParameter(PreparedStatement preparedStatement, int i, Object parameter, JdbcType jdbcType) throws SQLException {
    }

    @Override
    public Object getResult(ResultSet rs, String columnName) throws SQLException {
        return null;
    }

    @Override
    public Object getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Object getResult(ResultSet rs, int columnIndex) throws SQLException {
        return null;
    }
}
