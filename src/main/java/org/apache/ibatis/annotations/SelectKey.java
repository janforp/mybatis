package org.apache.ibatis.annotations;

import org.apache.ibatis.mapping.StatementType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Clinton Begin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SelectKey {

    String[] statement();

    String keyProperty();

    String keyColumn() default "";

    boolean before();

    Class<?> resultType();

    StatementType statementType() default StatementType.PREPARED;
}
