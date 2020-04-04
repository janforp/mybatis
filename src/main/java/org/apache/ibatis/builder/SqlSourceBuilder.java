package org.apache.ibatis.builder;

import lombok.Getter;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Clinton Begin
 */

/**
 * SQL源码构建器
 */
public class SqlSourceBuilder extends BaseBuilder {

    private static final String PARAMETER_PROPERTIES = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";

    public SqlSourceBuilder(Configuration configuration) {
        super(configuration);
    }

    /**
     * 解析成占位符格式的sql，并且解析出参数对象
     *
     * @param fullSqlWithPlaceholder xml中的原始sql
     * @param parameterType 参数类型
     * @param additionalParameters 参数
     * @return 占位符sql + 该sql对应的参数列表
     */
    public SqlSource parse(String fullSqlWithPlaceholder, Class<?> parameterType, Map<String, Object> additionalParameters) {
        //字符串标记处理器，一个静态内部类
        ParameterMappingTokenHandler tokenHandler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);
        //替换#{}中间的部分,如何替换，逻辑在ParameterMappingTokenHandler
        GenericTokenParser parser = new GenericTokenParser(
                "#{", //开始标记
                "}",//结束标记
                tokenHandler);//具体如何处理标记中的字符串还是看这个参数
        //把 #{XXX} 替换成 ?,并且其他sql不变，此处生成的sql是符合preparedStatement的格式的
        //select *
        //        from users
        //        where '1' = '1'
        //
        //            and id = ?
        //
        //
        //        select *
        //        from users
        //        where '1' = '1'
        //
        //            and id = ?
        //
        //
        //            and name = ?
        String sql = parser.parse(fullSqlWithPlaceholder);
        //参数占位符对象列表
        List<ParameterMapping> parameterMappingList = tokenHandler.getParameterMappings();
        //返回静态SQL源码
        return new StaticSqlSource(configuration, sql, parameterMappingList);
    }

    //参数映射记号处理器，静态内部类
    private static class ParameterMappingTokenHandler extends BaseBuilder implements TokenHandler {

        /**
         * 该属性是该类型的一个重要输出属性
         * #{property,javaType=int,jdbcType=NUMERIC} 列表
         */
        @Getter
        private List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();

        /**
         * 参数类型
         */
        private Class<?> parameterType;

        /**
         * 参数对象
         */
        private MetaObject metaParameters;

        public ParameterMappingTokenHandler(Configuration configuration, Class<?> parameterType, Map<String, Object> additionalParameters) {
            super(configuration);
            this.parameterType = parameterType;
            this.metaParameters = configuration.newMetaObject(additionalParameters);
        }

        /**
         * 把传入的#{username, jdbcType=VARCHAR, typeHandler= XXX, 。。。} 中的 username 转换为 ?
         * 当然，中间的一些信息会组合成一个 ParameterMapping 对象
         *
         * @param content 输入字符串
         * @return ?
         */
        @Override
        public String handleToken(String content) {
            //先构建参数映射
            //#{username, jdbcType=VARCHAR, typeHandler= XXX, 。。。}
            ParameterMapping parameterMapping = buildParameterMapping(content);
            parameterMappings.add(parameterMapping);
            //如何替换很简单，永远是一个问号，但是参数的信息要记录在parameterMappings里面供后续使用
            return "?";
        }

        /**
         * 参数对象
         *
         * @param content #{username, jdbcType=VARCHAR, typeHandler= XXX, 。。。}
         * @return 参数对象
         */
        private ParameterMapping buildParameterMapping(String content) {
            //#{favouriteSection,jdbcType=VARCHAR}
            //先解析参数映射,就是转化成一个hashMap
            Map<String, String> propertiesMap = parseParameterMapping(content);
            String property = propertiesMap.get("property");
            Class<?> propertyType;
            //这里分支比较多，需要逐个理解
            if (metaParameters.hasGetter(property)) { // issue #448 get type from additional params
                propertyType = metaParameters.getGetterType(property);
            } else if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
                propertyType = parameterType;
            } else if (JdbcType.CURSOR.name().equals(propertiesMap.get("jdbcType"))) {
                propertyType = java.sql.ResultSet.class;
            } else if (property != null) {
                MetaClass metaClass = MetaClass.forClass(parameterType);
                if (metaClass.hasGetter(property)) {
                    propertyType = metaClass.getGetterType(property);
                } else {
                    propertyType = Object.class;
                }
            } else {
                propertyType = Object.class;
            }
            ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, propertyType);
            Class<?> javaType = propertyType;
            String typeHandlerAlias = null;
            for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();
                if ("javaType".equals(name)) {
                    javaType = resolveClass(value);
                    builder.javaType(javaType);
                } else if ("jdbcType".equals(name)) {
                    builder.jdbcType(resolveJdbcType(value));
                } else if ("mode".equals(name)) {
                    builder.mode(resolveParameterMode(value));
                } else if ("numericScale".equals(name)) {
                    builder.numericScale(Integer.valueOf(value));
                } else if ("resultMap".equals(name)) {
                    builder.resultMapId(value);
                } else if ("typeHandler".equals(name)) {
                    typeHandlerAlias = value;
                } else if ("jdbcTypeName".equals(name)) {
                    builder.jdbcTypeName(value);
                } else if ("property".equals(name)) {
                    // Do Nothing
                } else if ("expression".equals(name)) {
                    throw new BuilderException("Expression based parameters are not supported yet");
                } else {
                    throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content + "}.  Valid properties are " + PARAMETER_PROPERTIES);
                }
            }
            //#{age,javaType=int,jdbcType=NUMERIC,typeHandler=MyTypeHandler}
            if (typeHandlerAlias != null) {
                builder.typeHandler(resolveTypeHandler(javaType, typeHandlerAlias));
            }
            return builder.build();
        }

        /**
         * #{username, jdbcType=VARCHAR, typeHandler= XXX, 。。。}
         *
         * @param content #{username, jdbcType=VARCHAR, typeHandler= XXX, 。。。}
         * @return map
         */
        private Map<String, String> parseParameterMapping(String content) {
            try {
                return new ParameterExpression(content);
            } catch (BuilderException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BuilderException("Parsing error was found in mapping #{" + content + "}.  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
            }
        }
    }
}
