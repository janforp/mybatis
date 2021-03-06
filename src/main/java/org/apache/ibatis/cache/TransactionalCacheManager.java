package org.apache.ibatis.cache;

import org.apache.ibatis.cache.decorators.TransactionalCache;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Clinton Begin
 */

/**
 * 事务缓存管理器，被CachingExecutor使用
 *
 * TransactionalCache实现了Cache接口，CachingExecutor会默认使用他包装初始生成的Cache，
 * 作用是如果事务提交，对缓存的操作才会生效，如果事务回滚或者不提交事务，则不对缓存产生影响
 */
public class TransactionalCacheManager {

    /**
     * 管理了许多TransactionalCache
     * key:?
     * value:?
     */
    private Map<Cache, TransactionalCache> transactionalCacheMap = new HashMap<Cache, TransactionalCache>();

    public void clear(Cache cache) {
        TransactionalCache transactionalCache = getTransactionalCache(cache);
        transactionalCache.clear();
    }

    //得到某个TransactionalCache的值
    public Object getObject(Cache cache, CacheKey key) {

        //二级缓存
        TransactionalCache transactionalCache = getTransactionalCache(cache);
        //结果
        Object object = transactionalCache.getObject(key);
        System.out.println(object);
        return object;
    }

    public void putObject(Cache cache, CacheKey key, Object value) {
        TransactionalCache transactionalCache = getTransactionalCache(cache);
        transactionalCache.putObject(key, value);
    }

    //提交时全部提交
    public void commit() {
        for (TransactionalCache transactionalCache : transactionalCacheMap.values()) {
            transactionalCache.commit();
        }
    }

    //回滚时全部回滚
    public void rollback() {
        for (TransactionalCache txCache : transactionalCacheMap.values()) {
            txCache.rollback();
        }
    }

    private TransactionalCache getTransactionalCache(Cache cache) {
        TransactionalCache transactionalCache = transactionalCacheMap.get(cache);
        if (transactionalCache == null) {
            transactionalCache = new TransactionalCache(cache);
            transactionalCacheMap.put(cache, transactionalCache);
        }
        return transactionalCache;
    }
}
