package org.apache.ibatis.builder;

import org.apache.ibatis.cache.Cache;

/**
 * 缓存引用解析器
 *
 * @author Clinton Begin
 */
public class CacheRefResolver {

    private final MapperBuilderAssistant assistant;

    private final String cacheRefNamespace;

    public CacheRefResolver(MapperBuilderAssistant assistant, String cacheRefNamespace) {
        this.assistant = assistant;
        this.cacheRefNamespace = cacheRefNamespace;
    }

    public Cache resolveCacheRef() {
        //反调MapperBuilderAssistant解析
        return assistant.useCacheRef(cacheRefNamespace);
    }
}