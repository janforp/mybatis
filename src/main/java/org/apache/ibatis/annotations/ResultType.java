package org.apache.ibatis.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used when a @Select method is using a
 * ResultHandler.  Those methods must have void return type, so
 * this annotation can be used to tell MyBatis what kind of object
 * it should build for each row.
 *
 * @author Jeff Butler
 * @since 3.2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ResultType {

    Class<?> value();
}
