package org.apache.ibatis.type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Clinton Begin
 */

/**
 * 别名
 * 2种方式可以注册别名
 * 1)xml方式
 * <typeAlias alias="Author" type="domain.blog.Author"/>
 * 2)annotation方式
 *
 * @Alias("author") public class Author {
 * ...
 * }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Alias {

    public String value();
}
