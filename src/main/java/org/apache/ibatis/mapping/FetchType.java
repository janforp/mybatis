package org.apache.ibatis.mapping;

/**
 * @author Eduardo Macarron
 */
public enum FetchType {

    /**
     * 延迟加载
     */
    LAZY,

    /**
     * 贪婪模型
     */
    EAGER,

    /**
     * 默认
     */
    DEFAULT
}
