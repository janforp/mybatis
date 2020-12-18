package org.apache.ibatis.cache.decorators;

import org.apache.ibatis.cache.Cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * The 2nd level cache transactional buffer.
 * 二级缓存
 *
 * This class holds all cache entries that are to be added to the 2nd level cache during a Session.
 * Entries are sent to the cache when commit is called or discarded if the Session is rolled back.
 * Blocking cache support has been added. Therefore any get() that returns a cache miss
 * will be followed by a put() so any lock associated with the key can be released.
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */

/**
 * 总结
 * MyBatis的二级缓存相对于一级缓存来说，实现了SqlSession之间缓存数据的共享，同时粒度更加的细，能够到namespace级别，通过Cache接口实现类不同的组合，对Cache的可控性也更强。
 * MyBatis在多表查询时，极大可能会出现脏数据，有设计上的缺陷，安全使用二级缓存的条件比较苛刻。
 * 在分布式环境下，由于默认的MyBatis Cache实现都是基于本地的，分布式环境下必然会出现读取到脏数据，
 * 需要使用集中式缓存将MyBatis的Cache接口实现，有一定的开发成本，直接使用Redis、Memcached等分布式缓存可能成本更低，安全性也更高。
 *
 * 事务缓存
 * 一次性存入多个缓存，移除多个缓存
 */
public class TransactionalCache implements Cache {

    private Cache delegate;

    /**
     * commit时要不要清缓存
     */
    private boolean clearOnCommit;

    /**
     * commit时要添加的元素
     * 只有事务提交的时候，二级缓存才生效
     */
    private Map<Object, Object> entriesToAddOnCommit;

    /**
     * 缓存未命中的对象集合
     * 主要是为了统计缓存命中率
     */
    private Set<Object> entriesMissedInCache;

    public TransactionalCache(Cache delegate) {
        this.delegate = delegate;
        //默认commit时不清缓存
        this.clearOnCommit = false;
        this.entriesToAddOnCommit = new HashMap<Object, Object>();
        this.entriesMissedInCache = new HashSet<Object>();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    @Override
    public Object getObject(Object key) {
        // issue #116
        Object object = delegate.getObject(key);
        if (object == null) {
            //如果没有查到，会把key加入Miss集合，这个主要是为了统计命中率
            entriesMissedInCache.add(key);
        }
        // issue #146
        if (clearOnCommit) {
            return null;
        } else {
            return object;
        }
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    /**
     * tcm的put方法也不是直接操作缓存，只是在把这次的数据和key放入待提交的Map中。
     *
     * @param key Can be any object but usually it is a {@link CacheKey}
     */
    @Override
    public void putObject(Object key, Object object) {
        //tcm的put方法也不是直接操作缓存，只是在把这次的数据和key放入待提交的Map中。
        entriesToAddOnCommit.put(key, object);
    }

    /**
     * 多了commit方法，提供事务功能
     * 二级缓存只有在一个事务提交之后，其他的时候才能使用，否则岂不是出现 另外一个事务读到了该事务没有提交的数据了？
     *
     * 读未提交(Read Uncommitted)：允许脏读，也就是可能读取到其他会话中未提交事务修改的数据
     */
    public void commit() {
        if (clearOnCommit) {
            delegate.clear();
        }
        //如果不调用commit方法的话，由于 TransactionalCache 的作用，并不会对二级缓存造成直接的影响
        flushPendingEntries();
        reset();
    }

    /**
     * 把map中的缓存刷到代理缓存中去，实现垮 sqlSession 的缓存
     */
    private void flushPendingEntries() {
        //提交的时候需要放到缓存中的数据
        Set<Map.Entry<Object, Object>> entrySet = entriesToAddOnCommit.entrySet();
        for (Map.Entry<Object, Object> entry : entrySet) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            delegate.putObject(key, value);
        }

        //没有命中的缓存 key
        for (Object entry : entriesMissedInCache) {
            if (!entriesToAddOnCommit.containsKey(entry)) {
                delegate.putObject(entry, null);
            }
        }
    }

    @Override
    public Object removeObject(Object key) {
        return null;
    }

    @Override
    public void clear() {
        clearOnCommit = true;
        entriesToAddOnCommit.clear();
    }

    public void rollback() {
        unlockMissedEntries();
        reset();
    }

    private void reset() {
        clearOnCommit = false;
        entriesToAddOnCommit.clear();
        entriesMissedInCache.clear();
    }

    private void unlockMissedEntries() {
        for (Object entry : entriesMissedInCache) {
            delegate.putObject(entry, null);
        }
    }
}
