package org.apache.ibatis.annotations;

import org.apache.ibatis.mapping.FetchType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Clinton Begin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface One {

    String select() default "";

    FetchType fetchType() default FetchType.DEFAULT;
}
