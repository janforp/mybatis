package org.apache.ibatis.scripting.defaults;

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */

/**
 * 默认参数处理器
 */
public class DefaultParameterHandler implements ParameterHandler {

    /**
     * 类型处理器注册机
     */
    private final TypeHandlerRegistry typeHandlerRegistry;

    /**
     * 映射的statement
     */
    private final MappedStatement mappedStatement;

    /**
     * 映射语句的参数对象
     */
    private final Object parameterObject;

    /**
     * sql语句
     */
    private BoundSql boundSql;

    /**
     * 没什么好说的
     */
    private Configuration configuration;

    public DefaultParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
        this.mappedStatement = mappedStatement;
        this.configuration = mappedStatement.getConfiguration();
        this.typeHandlerRegistry = mappedStatement.getConfiguration().getTypeHandlerRegistry();
        this.parameterObject = parameterObject;
        this.boundSql = boundSql;
    }

    @Override
    public Object getParameterObject() {
        return parameterObject;
    }

    //设置参数
    @Override
    public void setParameters(PreparedStatement preparedStatement) throws SQLException {
        ErrorContext.instance().activity("setting parameters").object(mappedStatement.getParameterMap().getId());
        //映射参数列表 #{property,javaType=int,jdbcType=NUMERIC}
        List<ParameterMapping> parameterMappingList = boundSql.getParameterMappings();
        if (parameterMappingList == null) {
            return;
        }
        //循环设参数
        for (int i = 0; i < parameterMappingList.size(); i++) {
            ParameterMapping parameterMapping = parameterMappingList.get(i);
            //如果不是OUT，才设进去
            if (parameterMapping.getMode() == ParameterMode.OUT) {
                continue;
            }
            Object value;
            //参数名称,如：#{username}
            String propertyName = parameterMapping.getProperty();
            if (boundSql.hasAdditionalParameter(propertyName)) { // issue #448 ask first for additional params
                value = boundSql.getAdditionalParameter(propertyName);
            } else if (parameterObject == null) {
                //若参数为null，直接设null
                value = null;
            } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                //若参数有相应的TypeHandler，直接设object
                value = parameterObject;
            } else {
                //除此以外，MetaObject.getValue反射取得值设进去
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                value = metaObject.getValue(propertyName);
            }
            TypeHandler typeHandler = parameterMapping.getTypeHandler();
            JdbcType jdbcType = parameterMapping.getJdbcType();
            if (value == null && jdbcType == null) {
                //不同类型的set方法不同，所以委派给子类的setParameter方法
                jdbcType = configuration.getJdbcTypeForNull();
            }
            int parameterIndex = i + 1;
            //因为循环从0开始，而参数的下标从1开始
            typeHandler.setParameter(preparedStatement, parameterIndex, value, jdbcType);
        }
    }
}
