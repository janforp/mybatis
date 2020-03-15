/*
 *    Copyright 2009-2013 the original author or authors.
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

package org.apache.ibatis.binding;

import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 */

/**
 * 映射器注册机
 */
public class MapperRegistry {

    private Configuration config;

    /**
     * 将已经添加的映射都放入HashMap
     * 注册的 mapperClass
     */
    private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<Class<?>, MapperProxyFactory<?>>();

    public MapperRegistry(Configuration config) {
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    //返回代理类
    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
        final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
        if (mapperProxyFactory == null) {
            throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
        }
        try {
            return mapperProxyFactory.newInstance(sqlSession);
        } catch (Exception e) {
            throw new BindingException("Error getting mapper instance. Cause: " + e, e);
        }
    }

    public <T> boolean hasMapper(Class<T> type) {
        return knownMappers.containsKey(type);
    }

    /**
     * 添加一个映射
     *
     * @param mapperClass 映射类
     * @param <T> 任何类型
     */
    public <T> void addMapper(Class<T> mapperClass) {
        //mapper必须是接口！才会添加
        if (mapperClass.isInterface()) {
            //knownMappers.containsKey(type) 该 class 已经注册到映射器中了，不能重复
            if (hasMapper(mapperClass)) {
                //如果重复添加了，报错
                throw new BindingException("Type " + mapperClass + " is already known to the MapperRegistry.");
            }
            boolean loadCompleted = false;
            try {
                //先把 mapper/dao 封装到 MapperProxyFactory 中
                MapperProxyFactory<T> mapperProxyFactory = new MapperProxyFactory<T>(mapperClass);
                knownMappers.put(mapperClass, mapperProxyFactory);
                // It's important that the mapperClass is added before the parser is run
                // otherwise the binding may automatically be attempted by the
                // mapper parser. If the mapperClass is already known, it won't try.
                MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, mapperClass);
                parser.parse();
                loadCompleted = true;
            } finally {
                //如果加载过程中出现异常需要再将这个mapper从mybatis中删除,这种方式比较丑陋吧，难道是不得已而为之？
                if (!loadCompleted) {
                    knownMappers.remove(mapperClass);
                }
            }
        }
    }

    /**
     * @since 3.2.2
     */
    public Collection<Class<?>> getMappers() {
        return Collections.unmodifiableCollection(knownMappers.keySet());
    }

    /**
     * @since 3.2.2
     */
    public void addMappers(String packageName, Class<?> superType) {
        //查找包下所有是superType的类
        ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
        //包下面的类的匹配器，该匹配器专门去匹配传入的class是不是superType类型
        ResolverUtil.IsA isATest = new ResolverUtil.IsA(superType);
        //在包packageName中找出 superType 的所有子类，并且放到resolverUtil的 matches 集合中
        resolverUtil.find(isATest, packageName);
        //从包中找出的class集合
        Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
        for (Class<?> mapperClass : mapperSet) {
            addMapper(mapperClass);
        }
    }

    /**
     * @since 3.2.2
     */
    //查找包下所有类
    public void addMappers(String packageName) {
        addMappers(packageName, Object.class);
    }

}
