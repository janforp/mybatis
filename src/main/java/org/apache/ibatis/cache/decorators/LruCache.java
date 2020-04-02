package org.apache.ibatis.cache.decorators;

import org.apache.ibatis.cache.Cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * 最近最少使用缓存
 * 基于 LinkedHashMap 覆盖其 removeEldestEntry 方法实现。
 * Lru  cache decorator
 *
 * @author Clinton Begin
 */
public class LruCache implements Cache {

    /**
     * 被装饰的缓存对象，缓存的功能是由该对象完成，LruCache 只是提供了一个 lru 的功能
     */
    private final Cache delegate;

    /**
     * 额外用了一个map才达到lru效果，但是委托的Cache里面其实也是一个map，这样等于用2倍的内存实现lru功能
     * 其实是使用一个重新了removeEldestEntry函数的LinkedHashMap实例来达到lru效果
     *
     * key:一个 delegate 中的缓存key
     * value:跟key一样
     */
    private Map<Object, Object> keyMap;

    /**
     * 最老的缓存的 key
     */
    private Object eldestKey;

    /**
     * 该构造器是由反射的方式调用的
     *
     * @param delegate 被装饰的缓存实例，就是实例提供缓存功能的实例
     */
    public LruCache(Cache delegate) {
        this.delegate = delegate;
        //其实就是设置链表的最大容量，实现超过该容量的时候删除最老元素的效果
        setSize(1024);
    }

    public void setSize(final int size) {
        keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {

            private static final long serialVersionUID = 4267176411845948333L;

            //核心就是覆盖 LinkedHashMap.removeEldestEntry方法,
            //返回true或false告诉 LinkedHashMap要不要删除此最老键值
            //LinkedHashMap内部其实就是每次访问或者插入一个元素都会把元素放到链表末尾，
            //这样不经常访问的键值肯定就在链表开头

            /**
             *  * Returns <tt>true</tt> if this map should remove its eldest entry.
             *      * This method is invoked by <tt>put</tt> and <tt>putAll</tt> after
             *      * inserting a new entry into the map.  It provides the implementor
             *      * with the opportunity to remove the eldest entry each time a new one
             *      * is added.  This is useful if the map represents a cache: it allows
             *      * the map to reduce memory consumption by deleting stale entries.
             *      *
             *      * <p>Sample use: this override will allow the map to grow up to 100
             *      * entries and then delete the eldest entry each time a new entry is
             *      * added, maintaining a steady state of 100 entries.
             *      * <pre>
             *      *     private static final int MAX_ENTRIES = 100;
             *      *
             *      *     protected boolean removeEldestEntry(Map.Entry eldest) {
             *      *        return size() &gt; MAX_ENTRIES;
             *      *     }
             *      * </pre>
             */
            @Override
            protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
                //实例缓存 delegate 中的缓存数据量是否大于 1024
                boolean tooBig = size() > size;
                if (tooBig) {
                    //这里没辙了，把eldestKey存入实例变量
                    eldestKey = eldest.getKey();
                }
                return tooBig;
            }
        };
    }

    @Override
    public void putObject(Object key, Object value) {
        //缓存直接存入被装饰对象
        delegate.putObject(key, value);
        //增加新纪录后，判断是否要将最老元素移除
        cycleKeyList(key);
    }

    private void cycleKeyList(Object key) {
        keyMap.put(key, key);
        //keyMap是linkedHashMap，最老的记录已经被移除了，然后这里我们还需要移除被委托的那个cache的记录
        if (eldestKey != null) {
            delegate.removeObject(eldestKey);
            eldestKey = null;
        }
    }

    @Override
    public Object getObject(Object key) {
        //get的时候调用一下LinkedHashMap.get，让经常访问的值移动到链表末尾
        keyMap.get(key); //touch
        return delegate.getObject(key);
    }

    @Override
    public Object removeObject(Object key) {
        return delegate.removeObject(key);
    }

    @Override
    public void clear() {
        delegate.clear();
        keyMap.clear();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return null;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }
}
