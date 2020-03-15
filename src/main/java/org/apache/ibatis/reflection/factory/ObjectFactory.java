/*
 *    Copyright 2009-2012 the original author or authors.
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

package org.apache.ibatis.reflection.factory;

import java.util.List;
import java.util.Properties;

/**
 * ObjectFactory 接口很简单，它包含两个创建实例用的方法，一个是处理默认无参构造方法的，另外一个是处理带参数的构造方法的。
 * 另外，setProperties 方法可以被用来配置 ObjectFactory，在初始化你的 ObjectFactory 实例后，
 * objectFactory 元素体中定义的属性会被传递给 setProperties 方法。
 *
 *
 * MyBatis uses an ObjectFactory to create all needed new Objects.
 *
 * @author Clinton Begin
 */
public interface ObjectFactory {

    /**
     * Sets configuration properties.
     * 设置属性
     *
     * @param properties configuration properties
     */
    void setProperties(Properties properties);

    /**
     * Creates a new object with default constructor.
     * 生产对象
     *
     * @param type Object type
     * @return
     */
    <T> T create(Class<T> type);

    /**
     * Creates a new object with the specified constructor and params.
     * 生产对象，使用指定的构造函数和构造函数参数
     *
     * @param clazz Object clazz 类型
     * @param constructorArgTypes Constructor argument types 实例化时候使用的构造器的参数
     * @param constructorArgs Constructor argument values 实例化时候使用的构造器的参数值
     * @return 一个实例对象
     */
    <T> T create(Class<T> clazz, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);

    /**
     * Returns true if this object can have a set of other objects.
     * It's main purpose is to support non-java.util.Collection objects like Scala collections.
     * 返回这个对象是否是集合，为了支持Scala collections？
     *
     * @param type Object type
     * @return whether it is a collection or not
     * @since 3.1.0
     */
    <T> boolean isCollection(Class<T> type);

}
