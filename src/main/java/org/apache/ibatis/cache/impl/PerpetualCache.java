package org.apache.ibatis.cache.impl;

import lombok.Getter;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * 永久缓存
 * 一旦存入就一直保持在HashMap中,由具体的Executor去clear缓存，具体看配置
 *
 * @author Clinton Begin
 */
public class PerpetualCache implements Cache {

    /**
     * 每个永久缓存有一个ID来识别 namespace
     */
    @Getter
    private String id;

    /**
     * 内部就是一个HashMap,所有方法基本就是直接调用HashMap的方法,不支持多线程？
     * 一级缓存基本上不存在并发的 问题
     *
     * key:一般情况下是 cacheKey 实例
     * value：该cacheKey对应的值
     */
    private Map<Object, Object> cache = new HashMap<Object, Object>();

    public PerpetualCache(String id) {
        this.id = id;
    }

    @Override
    public int getSize() {
        return cache.size();
    }

    @Override
    public void putObject(Object key, Object value) {
        cache.put(key, value);
    }

    @Override
    public Object getObject(Object key) {
        return cache.get(key);
    }

    @Override
    public Object removeObject(Object key) {
        return cache.remove(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (getId() == null) {
            throw new CacheException("Cache instances require an ID.");
        }
        if (this == o) {
            return true;
        }
        if (!(o instanceof Cache)) {
            return false;
        }

        Cache otherCache = (Cache) o;
        //只要id相等就认为两个cache相同
        return getId().equals(otherCache.getId());
    }

    @Override
    public int hashCode() {
        if (getId() == null) {
            throw new CacheException("Cache instances require an ID.");
        }
        return getId().hashCode();
    }
}
