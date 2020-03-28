package org.apache.ibatis.cache;

/**
 * @author Clinton Begin
 */

/**
 * Null值缓存key
 */
public final class NullCacheKey extends CacheKey {

    private static final long serialVersionUID = 3704229911977019465L;

    public NullCacheKey() {
        //只有hash值和校验码为key?
        super();
    }

    @Override
    public void update(Object object) {
        throw new CacheException("Not allowed to update a NullCacheKey instance.");
    }

    @Override
    public void updateAll(Object[] objects) {
        throw new CacheException("Not allowed to update a NullCacheKey instance.");
    }
}
