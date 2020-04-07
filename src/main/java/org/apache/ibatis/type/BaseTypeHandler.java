package org.apache.ibatis.type;

import lombok.Setter;
import org.apache.ibatis.session.Configuration;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 * @author Simone Tripodi
 */

/**
 * 类型处理器的基类
 */
public abstract class BaseTypeHandler<T> extends TypeReference<T> implements TypeHandler<T> {

    @Setter
    protected Configuration configuration;

    /**
     * 非NULL情况，怎么设参数还得交给不同的子类完成,因为 PreparedStatement 即使参数为null，也要指定具体类型
     *
     * @param ps 预处理语句
     * @param parameterIndex 参数下标
     * @param parameter 参数对象
     * @param jdbcType 该参数的数据库类型
     * @throws SQLException sql异常
     */
    public abstract void setNonNullParameter(PreparedStatement ps, int parameterIndex, T parameter, JdbcType jdbcType) throws SQLException;

    /**
     * 获取查询结果的时候就可能为null啦
     */
    public abstract T getNullableResult(ResultSet rs, String columnName) throws SQLException;

    /**
     * 获取查询结果的时候就可能为null啦
     */
    public abstract T getNullableResult(ResultSet rs, int columnIndex) throws SQLException;

    /**
     * 获取查询结果的时候就可能为null啦
     */
    public abstract T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException;

    @Override
    public void setParameter(PreparedStatement preparedStatement, int parameterIndex, T parameter, JdbcType jdbcType) throws SQLException {
        //特殊情况，设置NULL
        if (parameter == null) {
            if (jdbcType == null) {
                //如果没设置jdbcType，报错啦
                throw new TypeException("JDBC requires that the JdbcType must be specified for all nullable parameters.");
            }
            try {
                //设成NULL
                preparedStatement.setNull(parameterIndex, jdbcType.TYPE_CODE);
            } catch (SQLException e) {
                throw new TypeException("Error setting null for parameter #" + parameterIndex + " with JdbcType " + jdbcType + " . " +
                        "Try setting a different JdbcType for this parameter or a different jdbcTypeForNull configuration property. " +
                        "Cause: " + e, e);
            }
        } else {
            //非NULL情况，怎么设还得交给不同的子类完成, setNonNullParameter是一个抽象方法
            setNonNullParameter(preparedStatement, parameterIndex, parameter, jdbcType);
        }
    }

    @Override
    public T getResult(ResultSet rs, String columnName) throws SQLException {
        T result = getNullableResult(rs, columnName);
        //通过ResultSet.wasNull判断是否为NULL
        if (rs.wasNull()) {
            return null;
        } else {
            return result;
        }
    }

    @Override
    public T getResult(ResultSet rs, int columnIndex) throws SQLException {
        T result = getNullableResult(rs, columnIndex);
        if (rs.wasNull()) {
            return null;
        } else {
            return result;
        }
    }

    @Override
    public T getResult(CallableStatement cs, int columnIndex) throws SQLException {
        T result = getNullableResult(cs, columnIndex);
        //通过CallableStatement.wasNull判断是否为NULL
        if (cs.wasNull()) {
            return null;
        } else {
            return result;
        }
    }
}
