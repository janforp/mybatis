package org.apache.ibatis.mapping;

import lombok.Getter;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.sql.ResultSet;

/**
 * 参数映射
 * 如：#{property,javaType=int,jdbcType=NUMERIC}
 *
 * @author Clinton Begin
 */
public class ParameterMapping {

    private Configuration configuration;

    //property
    @Getter
    private String property;

    /**
     * Used for handling output of callable statements
     * IN,
     * OUT,
     * INOUT
     */
    @Getter
    private ParameterMode mode;

    /**
     * Used for handling output of callable statements
     */
    //javaType=int
    @Getter
    private Class<?> javaType = Object.class;

    /**
     * Used in the UnknownTypeHandler in case there is no handler for the property type
     */
    //jdbcType=NUMERIC
    @Getter
    private JdbcType jdbcType;

    /**
     * Used for handling output of callable statements
     */
    //numericScale
    @Getter
    private Integer numericScale;

    /**
     * Used when setting parameters to the PreparedStatement
     */
    @Getter
    private TypeHandler<?> typeHandler;

    /**
     * Used for handling output of callable statements
     */
    @Getter
    private String resultMapId;

    /**
     * Used for handling output of callable statements
     */
    //jdbcType=NUMERIC
    @Getter
    private String jdbcTypeName;

    /**
     * Not used
     */
    @Getter
    private String expression;

    private ParameterMapping() {
    }

    //静态内部类，建造者模式
    public static class Builder {

        private ParameterMapping parameterMapping = new ParameterMapping();

        public Builder(Configuration configuration, String property, TypeHandler<?> typeHandler) {
            parameterMapping.configuration = configuration;
            parameterMapping.property = property;
            parameterMapping.typeHandler = typeHandler;
            parameterMapping.mode = ParameterMode.IN;
        }

        public Builder(Configuration configuration, String property, Class<?> javaType) {
            parameterMapping.configuration = configuration;
            parameterMapping.property = property;
            parameterMapping.javaType = javaType;
            parameterMapping.mode = ParameterMode.IN;
        }

        public Builder mode(ParameterMode mode) {
            parameterMapping.mode = mode;
            return this;
        }

        public Builder javaType(Class<?> javaType) {
            parameterMapping.javaType = javaType;
            return this;
        }

        public Builder jdbcType(JdbcType jdbcType) {
            parameterMapping.jdbcType = jdbcType;
            return this;
        }

        public Builder numericScale(Integer numericScale) {
            parameterMapping.numericScale = numericScale;
            return this;
        }

        public Builder resultMapId(String resultMapId) {
            parameterMapping.resultMapId = resultMapId;
            return this;
        }

        public Builder typeHandler(TypeHandler<?> typeHandler) {
            parameterMapping.typeHandler = typeHandler;
            return this;
        }

        public Builder jdbcTypeName(String jdbcTypeName) {
            parameterMapping.jdbcTypeName = jdbcTypeName;
            return this;
        }

        public Builder expression(String expression) {
            parameterMapping.expression = expression;
            return this;
        }

        /**
         * build构建
         *
         * @return 封装参数对象
         */
        public ParameterMapping build() {
            resolveTypeHandler();
            validate();
            return parameterMapping;
        }

        /**
         * 校验参数的java类型不是 ResultSet
         * 确保有一个 typeHandler
         */
        private void validate() {
            if (ResultSet.class.equals(parameterMapping.javaType)) {
                if (parameterMapping.resultMapId == null) {
                    throw new IllegalStateException("Missing resultmap in property '"
                            + parameterMapping.property + "'.  "
                            + "Parameters of type java.sql.ResultSet require a resultmap.");
                }
            } else {
                if (parameterMapping.typeHandler == null) {
                    throw new IllegalStateException("Type handler was null on parameter mapping for property '"
                            + parameterMapping.property + "'.  "
                            + "It was either not specified and/or could not be found for the javaType / jdbcType combination specified.");
                }
            }
        }

        private void resolveTypeHandler() {
            //如果没有指定特殊的typeHandler，则根据javaType，jdbcType来查表确定一个默认的typeHandler
            if (parameterMapping.typeHandler == null && parameterMapping.javaType != null) {
                Configuration configuration = parameterMapping.configuration;
                TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
                parameterMapping.typeHandler = typeHandlerRegistry.getTypeHandler(parameterMapping.javaType, parameterMapping.jdbcType);
            }
        }
    }
}
