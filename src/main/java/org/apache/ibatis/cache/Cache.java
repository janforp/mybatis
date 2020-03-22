/*
 *    Copyright 2009-2014 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.apache.ibatis.cache;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * https://tech.meituan.com/2018/01/19/mybatis-cache.html
 *
 *
 * SPI for cache providers.
 *
 * 每个 namespace 都有一个 cache 实例
 * One instance of cache will be created for each namespace.
 *
 * 缓存实例必须有一个特定的构造器来接收 String id :MyBatis will pass the namespace as id to the constructor.
 * The cache implementation must have a constructor that receives the cache id as an String parameter.
 *
 * MyBatis will pass the namespace as id to the constructor.
 *
 * <pre>
 * public MyCache(final String id) {
 *  if (id == null) {
 *    throw new IllegalArgumentException("Cache instances require an ID");
 *  }
 *  this.id = id;
 *  initialize();
 * }
 * </pre>
 *
 *
 * 缓存配置和缓存实例是绑定在 SQL 映射文件的命名空间是很重要的。因此,所有 在相同命名空间的语句正如绑定的缓存一样。
 * 语句可以修改和缓存交互的方式, 或在语句的 语句的基础上使用两种简单的属性来完全排除它们。默认情况下,语句可以这样来配置:
 *
 * <select ... flushCache="false" useCache="true"/>
 * <insert ... flushCache="true"/>
 * <update ... flushCache="true"/>
 * <delete ... flushCache="true"/>
 *
 *
 *
 * Mybatis 使用到了两种缓存：本地缓存（local cache）和二级缓存（second level cache）。
 *
 * 每当一个新 session 被创建，MyBatis 就会创建一个与之相关联的本地缓存。任何在 session 执行过的查询结果都会被保存在本地缓存中，
 * 所以，当再次执行参数相同的相同查询时，就不需要实际查询数据库了。本地缓存将会在做出修改、事务提交或回滚，以及关闭 session 时清空。
 *
 * 默认情况下，本地缓存数据的生命周期等同于整个 session 的周期。由于缓存会被用来解决循环引用问题和加快重复嵌套查询的速度，所以无法将其完全禁用。
 * 但是你可以通过设置 localCacheScope=STATEMENT 来只在语句执行时使用缓存。
 *
 * 注意，如果 localCacheScope 被设置为 SESSION，对于某个对象，MyBatis 将返回在本地缓存中唯一对象的引用。
 * 对返回的对象（例如 list）做出的任何修改将会影响本地缓存的内容，进而将会影响到在本次 session 中从缓存返回的值。因此，不要对 MyBatis 所返回的对象作出更改，以防后患。
 *
 * @author Clinton Begin
 */
public interface Cache {

    /**
     * @return The identifier of this cache
     */
    String getId();

    /**
     * @param key Can be any object but usually it is a {@link CacheKey}
     * @param value The result of a select.
     */
    void putObject(Object key, Object value);

    /**
     * @param key The key
     * @return The object stored in the cache.
     */
    Object getObject(Object key);

    /**
     * Optional. It is not called by the core.
     *
     * @param key The key
     * @return The object that was removed
     */
    Object removeObject(Object key);

    /**
     * Clears this cache instance
     */
    void clear();

    /**
     * Optional. This method is not called by the core.
     *
     * @return The number of elements stored in the cache (not its capacity).
     */
    int getSize();

    /**
     * Optional. As of 3.2.6 this method is no longer called by the core.
     *
     * Any locking needed by the cache must be provided internally by the cache provider.
     *
     * @return A ReadWriteLock
     */
    //取得读写锁, 从3.2.6开始没用了，要SPI自己实现锁
    ReadWriteLock getReadWriteLock();
}