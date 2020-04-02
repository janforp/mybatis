package org.apache.ibatis.mapping;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;
import org.apache.ibatis.cache.decorators.BlockingCache;
import org.apache.ibatis.cache.decorators.LoggingCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.ScheduledCache;
import org.apache.ibatis.cache.decorators.SerializedCache;
import org.apache.ibatis.cache.decorators.SynchronizedCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 缓存构建器,建造者模式
 */
public class CacheBuilder {

    /**
     * 主要看该方法
     *
     * @return 一个缓存实例
     */
    public Cache build() {
        setDefaultImplementations();//保险
        //先new一个base的cache(PerpetualCache)
        //实例化一个cache的实现对象 class org.apache.ibatis.cache.impl.PerpetualCache
        Cache cache = newBaseCacheInstance(implementation, id);
        //为缓存实例塞入用户配置的值
        setCacheProperties(cache);

        // issue #352, do not apply decorators to custom caches
        //其实实现只支持这一个缓存才支持淘汰策略
        if (PerpetualCache.class.equals(cache.getClass())) {
            for (Class<? extends Cache> decorator : decorators) {
                //为了让该缓存实例具备这些淘汰策略，使用装饰者模式，把该缓存实例设置到装饰者中
                cache = newCacheDecoratorInstance(decorator, cache);
                //又要来一遍设额外属性
                //为缓存实例塞入用户配置的值，此时是塞入装饰者缓存对象
                setCacheProperties(cache);
            }
            //最后附加上标准的装饰者
            cache = setStandardDecorators(cache);
        } else if (!LoggingCache.class.isAssignableFrom(cache.getClass())) {
            //如果是custom缓存，且不是日志，要加日志
            cache = new LoggingCache(cache);
        }
        return cache;
    }

    private void setDefaultImplementations() {
        //又是一重保险，如果为null则设默认值,和XMLMapperBuilder.cacheElement以及MapperBuilderAssistant.useNewCache逻辑重复了
        if (implementation == null) {
            //默认缓存实现
            implementation = PerpetualCache.class;
            if (decorators.isEmpty()) {
                //模型缓存淘汰规则
                decorators.add(LruCache.class);
            }
        }
    }

    /**
     * currentNamespace
     */
    private String id;

    /**
     * 缓存实现类型，如：PerpetualCache
     */
    private Class<? extends Cache> implementation;

    /**
     * 缓存实现类型，如：LruCache
     */
    private List<Class<? extends Cache>> decorators;

    private Integer size;

    private Long clearInterval;

    private boolean readWrite;

    /**
     * 配置cache的时候传入的一些属性
     */
    private Properties properties;

    private boolean blocking;

    public CacheBuilder(String id) {
        this.id = id;
        this.decorators = new ArrayList<Class<? extends Cache>>();
    }

    public CacheBuilder implementation(Class<? extends Cache> implementation) {
        this.implementation = implementation;
        return this;
    }

    public CacheBuilder addDecorator(Class<? extends Cache> decorator) {
        if (decorator != null) {
            this.decorators.add(decorator);
        }
        return this;
    }

    public CacheBuilder size(Integer size) {
        this.size = size;
        return this;
    }

    public CacheBuilder clearInterval(Long clearInterval) {
        this.clearInterval = clearInterval;
        return this;
    }

    public CacheBuilder readWrite(boolean readWrite) {
        this.readWrite = readWrite;
        return this;
    }

    public CacheBuilder blocking(boolean blocking) {
        this.blocking = blocking;
        return this;
    }

    public CacheBuilder properties(Properties properties) {
        this.properties = properties;
        return this;
    }

    //最后附加上标准的装饰者
    private Cache setStandardDecorators(Cache cache) {
        try {
            MetaObject metaCache = SystemMetaObject.forObject(cache);
            boolean hasSizeSetter = metaCache.hasSetter("size");
            if (this.size != null && hasSizeSetter) {
                metaCache.setValue("size", this.size);
            }
            if (clearInterval != null) {
                //刷新缓存间隔,怎么刷新呢，用ScheduledCache来刷，还是装饰者模式，漂亮！
                cache = new ScheduledCache(cache);
                ((ScheduledCache) cache).setClearInterval(clearInterval);
            }
            if (readWrite) {
                //如果readOnly=false,可读写的缓存 会返回缓存对象的拷贝(通过序列化) 。这会慢一些,但是安全,因此默认是 false。
                cache = new SerializedCache(cache);
            }
            //日志缓存
            cache = new LoggingCache(cache);
            //同步缓存, 3.2.6以后这个类已经没用了，考虑到Hazelcast, EhCache已经有锁机制了，所以这个锁就画蛇添足了。
            cache = new SynchronizedCache(cache);
            if (blocking) {
                cache = new BlockingCache(cache);
            }
            return cache;
        } catch (Exception e) {
            throw new CacheException("Error building standard cache decorators.  Cause: " + e, e);
        }
    }

    private void setCacheProperties(Cache cache) {
        if (properties == null) {
            return;
        }
        //缓存对象
        MetaObject metaCache = SystemMetaObject.forObject(cache);
        //用反射设置额外的property属性
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();
            boolean hasSetterOfName = metaCache.hasSetter(name);
            if (!hasSetterOfName) {
                continue;
            }
            //获取该缓存的该属性的class
            Class<?> type = metaCache.getSetterType(name);
            //下面就是各种基本类型的判断了，味同嚼蜡但是又不得不写
            if (String.class == type) {
                metaCache.setValue(name, value);

            } else if (int.class == type || Integer.class == type) {
                metaCache.setValue(name, Integer.valueOf(value));

            } else if (long.class == type || Long.class == type) {
                metaCache.setValue(name, Long.valueOf(value));

            } else if (short.class == type || Short.class == type) {
                metaCache.setValue(name, Short.valueOf(value));

            } else if (byte.class == type || Byte.class == type) {
                metaCache.setValue(name, Byte.valueOf(value));

            } else if (float.class == type || Float.class == type) {
                metaCache.setValue(name, Float.valueOf(value));

            } else if (boolean.class == type || Boolean.class == type) {
                metaCache.setValue(name, Boolean.valueOf(value));

            } else if (double.class == type || Double.class == type) {
                metaCache.setValue(name, Double.valueOf(value));

            } else {
                //只支持这几种基本类型
                throw new CacheException("Unsupported property type for cache: '" + name + "' of type " + type);
            }
        }
    }

    private Cache newBaseCacheInstance(Class<? extends Cache> cacheClass, String id) {
        Constructor<? extends Cache> cacheConstructor = getBaseCacheConstructor(cacheClass);
        try {
            //缓存的实现类必须要求有一个带id的构造器
            return cacheConstructor.newInstance(id);
        } catch (Exception e) {
            throw new CacheException("Could not instantiate cache implementation (" + cacheClass + "). Cause: " + e, e);
        }
    }

    /**
     * 缓存的实现类必须要求有一个带id的构造器
     *
     * @param cacheClass 缓存实现类的class
     * @return 构造器
     */
    private Constructor<? extends Cache> getBaseCacheConstructor(Class<? extends Cache> cacheClass) {
        try {
            return cacheClass.getConstructor(String.class);
        } catch (Exception e) {
            throw new CacheException("Invalid base cache implementation (" + cacheClass + ").  " +
                    "Base cache implementations must have a constructor that takes a String id as a parameter.  Cause: " + e, e);
        }
    }

    /**
     * 为了让该缓存实例具备这些淘汰策略，使用装饰者模式，把该缓存实例设置到装饰者中
     *
     * @param decoratorCacheClass 装饰者类型
     * @param base 被装饰的缓存实例
     * @return 一个装饰者对象
     */
    private Cache newCacheDecoratorInstance(Class<? extends Cache> decoratorCacheClass, Cache base) {
        Constructor<? extends Cache> cacheConstructor = getCacheDecoratorConstructor(decoratorCacheClass);
        try {
            return cacheConstructor.newInstance(base);
        } catch (Exception e) {
            throw new CacheException("Could not instantiate cache decorator (" + decoratorCacheClass + "). Cause: " + e, e);
        }
    }

    /**
     * 装饰者缓存实例，必须有一个特定的构造器，入参数只有一个缓存实例
     *
     * @param cacheClass 被装饰缓存class
     * @return 一个特定的构造器
     */
    private Constructor<? extends Cache> getCacheDecoratorConstructor(Class<? extends Cache> cacheClass) {
        try {
            return cacheClass.getConstructor(Cache.class);
        } catch (Exception e) {
            throw new CacheException("Invalid cache decorator (" + cacheClass + ").  " +
                    "Cache decorators must have a constructor that takes a Cache instance as a parameter.  Cause: " + e, e);
        }
    }
}
