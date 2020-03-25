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

import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 映射器代理工厂，通过这个工厂类可以获取到对应的映射器代理类MapperProxy，
 * 这里只需要保存一个映射器的代理工厂，根据工厂就可以获取到对应的映射器。
 *
 * @author Lasse Voss
 */
public class MapperProxyFactory<T> {

    /**
     * DAO.class
     */
    private final Class<T> mapperInterfaceClass;

    /**
     * key:dao里面的方法，value:封装了该方法的对象
     * 这个缓存集合用于保存当前映射器中的映射方法的。
     *
     * 映射方法单独定义，是因为这里并不存在一个真正的类和方法供调用，只是通过反射和代理的原理来实现的假的调用，
     * 映射方法是调用的最小单位（独立个体），将映射方法定义之后，它就成为一个实实在在的存在，
     * 我们可以将调用过的方法保存到对应的映射器的缓存中，以供下次调用，
     * 避免每次调用相同的方法的时候都需要重新进行方法的生成。很明显，方法的生成比较复杂，会消耗一定的时间，
     * 将其保存在缓存集合中备用，可以极大的解决这种时耗问题。
     */
    private Map<Method, MapperMethod> methodCache = new ConcurrentHashMap<Method, MapperMethod>();

    public MapperProxyFactory(Class<T> mapperInterface) {
        this.mapperInterfaceClass = mapperInterface;
    }

    public Class<T> getMapperInterface() {
        return mapperInterfaceClass;
    }

    public Map<Method, MapperMethod> getMethodCache() {
        return methodCache;
    }

    @SuppressWarnings("unchecked")
    protected T newInstance(MapperProxy<T> mapperProxy) {
        //用JDK自带的动态代理生成映射器
        ClassLoader classLoader = mapperInterfaceClass.getClassLoader();
        Class[] classes = { mapperInterfaceClass };
        return (T) Proxy.newProxyInstance(classLoader, classes, mapperProxy);
    }

    public T newInstance(SqlSession sqlSession) {
        final MapperProxy<T> mapperProxy = new MapperProxy<T>(sqlSession, mapperInterfaceClass, methodCache);
        return newInstance(mapperProxy);
    }

}
