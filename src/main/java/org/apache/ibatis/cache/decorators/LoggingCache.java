package org.apache.ibatis.cache.decorators;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * @author Clinton Begin
 */

/**
 * 日志缓存
 * 添加功能：取缓存时打印命中率
 */
public class LoggingCache implements Cache {

    protected int requests = 0;

    protected int hits = 0;

    //用的mybatis自己的抽象Log
    private Log log;

    private Cache delegate;

    public LoggingCache(Cache delegate) {
        this.delegate = delegate;
        this.log = LogFactory.getLog(getId());
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
    public void putObject(Object key, Object object) {
        delegate.putObject(key, object);
    }

    //目的就是getObject时，打印命中率
    @Override
    public Object getObject(Object key) {
        //访问一次requests加一
        requests++;
        final Object value = delegate.getObject(key);
        //命中了则hits加一
        if (value != null) {
            hits++;
        }
        if (log.isDebugEnabled()) {
            //就是打印命中率 hits/requests
            log.debug("Cache Hit Ratio [" + getId() + "]: " + getHitRatio());
        }
        return value;
    }

    @Override
    public Object removeObject(Object key) {
        return delegate.removeObject(key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    private double getHitRatio() {
        return (double) hits / (double) requests;
    }
}
