
package org.apache.ibatis.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.StatementType;

/**
 * @author Clinton Begin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Options {
  boolean useCache() default true;

  boolean flushCache() default false;

  ResultSetType resultSetType() default ResultSetType.FORWARD_ONLY;

  StatementType statementType() default StatementType.PREPARED;

  int fetchSize() default -1;

  int timeout() default -1;

  boolean useGeneratedKeys() default false;

  String keyProperty() default "id";

  String keyColumn() default "";
}
