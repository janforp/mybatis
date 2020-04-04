package org.apache.ibatis.mapping;

import lombok.Getter;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 这个对象：什么都有了，1：完成的带？的sq，2：？对应的参数，3：每个？对应的参数的各种类型，4：meta封装之后的参数，可以与 statement 进行交互啦
 *
 *
 * 一个 MappedStatement 以及传入的 参数，就可以获得一个 BoundSql 对象，也就是没次调用都会生成一个这样的对象
 *
 * 绑定的SQL,是从SqlSource而来，将动态内容都处理完成得到的SQL语句字符串，其中包括?,还有绑定的参数
 *
 * An actual SQL String got form an {@link SqlSource} after having processed any dynamic content.
 * The SQL may have SQL placeholders "?" and an list (ordered) of an parameter mappings
 * with the additional information for each parameter (at least the property name of the input object to read
 * the value from).
 * </br>
 * Can also have additional parameters that are created by the dynamic language (for loops, bind...).
 *
 * @author Clinton Begin
 */
public class BoundSql {

    /**
     * 可以直接给 statement 使用的sql
     */
    @Getter
    private String sql;

    /**
     * xml中的参数
     *
     * #{property,javaType=int,jdbcType=NUMERIC} 列表
     */
    @Getter
    private List<ParameterMapping> parameterMappings;

    /**
     * 入参数
     */
    @Getter
    private Object parameterObject;

    /**
     * 参数:如：{"_parameter":{"finalParamMap1":1,"finalParamMap2":"User1","name":"User1","id":1}}
     */
    private MetaObject metaParameters;

    public BoundSql(Configuration configuration, String sql, List<ParameterMapping> parameterMappings, Object parameterObject) {
        this.sql = sql;
        this.parameterMappings = parameterMappings;
        this.parameterObject = parameterObject;
        Map<String, Object> additionalParameters = new HashMap<String, Object>();
        this.metaParameters = configuration.newMetaObject(additionalParameters);
    }

    public boolean hasAdditionalParameter(String name) {
        return metaParameters.hasGetter(name);
    }

    public void setAdditionalParameter(String name, Object value) {
        metaParameters.setValue(name, value);
    }

    public Object getAdditionalParameter(String name) {
        return metaParameters.getValue(name);
    }
}
