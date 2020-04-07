package org.apache.ibatis.type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mybatis注册TypeHandler最基本的方式就是建立一个javaType、jdbcType和TypeHandler的对应关系。在使用typeHandler子元素进行注册的时候，有三种类型的注册方式：
 *
 * 1.如果我们指定了javaType和jdbcType，那么Mybatis会注册一个对应javaType和jdbcType的TypeHandler。
 *
 * 2.如果我们只指定了javaType属性，那么这个时候又分两种情况：
 *
 * （1）如果我们通过注解的形式在TypeHandler类上用@MappedJdbcTypes指定了对应的jdbcType，那么Mybatis会一一注册指定的javaType、
 * jdbcType和TypeHandler的组合，也包括使用这种形式指定了jdbcType为null的情况。
 *
 * @author Eduardo Macarron
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MappedJdbcTypes {

    JdbcType[] value();

    boolean includeNullJdbcType() default false;
}
