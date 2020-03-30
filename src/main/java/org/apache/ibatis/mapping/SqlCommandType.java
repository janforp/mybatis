package org.apache.ibatis.mapping;

/**
 * SQL命令类型
 *
 * @author Clinton Begin
 */
public enum SqlCommandType {
    UNKNOWN,
    INSERT,
    UPDATE,
    DELETE,
    SELECT;
}
