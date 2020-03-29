package org.apache.ibatis.session;

/**
 * @author Eduardo Macarron
 */

/**
 * 本地缓存机制（Local Cache）防止循环引用（circular references）和加速重复嵌套查询。
 * 默认值为 SESSION，这种情况下会缓存一个会话中执行的所有查询。
 * 若设置值为 STATEMENT，本地会话仅用在语句执行上，对相同 SqlSession 的不同调用将不会共享数据。
 */
public enum LocalCacheScope {

    /**
     * 一级缓存作用域是 SqlSession
     */
    SESSION,

    /**
     * 一级缓存作用一是 STATEMENT
     */
    STATEMENT
}
