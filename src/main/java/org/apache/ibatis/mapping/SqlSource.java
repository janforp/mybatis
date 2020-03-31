package org.apache.ibatis.mapping;

/**
 * SQL源码
 * Represents the content of a mapped statement read from an XML file or an annotation.
 * It creates the SQL that will be passed to the database out of the input parameter received from the user.
 * 生成不带参数的sql
 *
 * @author Clinton Begin
 */
public interface SqlSource {

    /**
     * SQL + 参数 + 占位符列表
     *
     * @param parameterObject 参数
     * @return SQL + 参数 + 占位符列表
     */
    BoundSql getBoundSql(Object parameterObject);
}
