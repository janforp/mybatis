package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * DAO传入的参数类型 -----> 传入sql的类型
 * java.util.Date -----> java.sql.Date
 *
 * @author Clinton Begin
 */
public class DateOnlyTypeHandler extends BaseTypeHandler<Date> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int parameterIndex, Date parameter, JdbcType jdbcType) throws SQLException {
        long time = parameter.getTime();
        ps.setDate(parameterIndex, new java.sql.Date(time));
    }

    @Override
    public Date getNullableResult(ResultSet rs, String columnName) throws SQLException {
        java.sql.Date sqlDate = rs.getDate(columnName);
        if (sqlDate != null) {
            return new java.util.Date(sqlDate.getTime());
        }
        return null;
    }

    @Override
    public Date getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        java.sql.Date sqlDate = rs.getDate(columnIndex);
        if (sqlDate != null) {
            return new java.util.Date(sqlDate.getTime());
        }
        return null;
    }

    @Override
    public Date getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        java.sql.Date sqlDate = cs.getDate(columnIndex);
        if (sqlDate != null) {
            return new java.util.Date(sqlDate.getTime());
        }
        return null;
    }
}
