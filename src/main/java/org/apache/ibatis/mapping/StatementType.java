package org.apache.ibatis.mapping;

/**
 * @author Clinton Begin
 */
public enum StatementType {

    /**
     * 普通的 statement
     */
    STATEMENT,

    /**
     * 预处理，可以防止 sql 注入
     */
    PREPARED,

    /**
     * 存储过程
     */
    CALLABLE
}
