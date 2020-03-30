package org.apache.ibatis.mapping;

/**
 * 参数模式（给SP用）
 *
 * @author Clinton Begin
 */
public enum ParameterMode {

    /**
     * 输入参数
     */
    IN,

    /**
     * 输出参数
     */
    OUT,

    /**
     * 输入输出参数
     */
    INOUT
}
