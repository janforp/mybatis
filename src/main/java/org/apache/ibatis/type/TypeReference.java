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

package org.apache.ibatis.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * References a generic type.
 * 使用BaseTypeHandler还有一个好处是它继承了另外一个叫做TypeReference的抽象类，
 * 通过TypeReference的getRawType()方法可以获取到当前TypeHandler所使用泛型的原始类型
 * 。这对Mybatis在注册TypeHandler的时候是非常有好处的。在没有指定javaType的情况下
 * ，Mybatis在注册TypeHandler时可以通过它来获取当前TypeHandler所使用泛型的原始类型作为要注册的TypeHandler的javaType类型
 *
 * @param <T> the referenced type
 * @author Simone Tripodi
 * @since 3.1.0
 * 3.1新加的类型引用,为了引用一个泛型类型
 */
public abstract class TypeReference<T> {

    /**
     * 引用的原生类型
     * javaType
     */
    private final Type rawType;

    protected TypeReference() {
        rawType = getSuperclassTypeParameter(this.getClass());
    }

    Type getSuperclassTypeParameter(Class<?> clazz) {
        //得到泛型T的实际类型
        Type genericSuperclass = clazz.getGenericSuperclass();
        //???? 怎么会是Class型
        if (genericSuperclass instanceof Class) {
            // try to climb up the hierarchy until meet something useful
            if (TypeReference.class != genericSuperclass) {
                return getSuperclassTypeParameter(clazz.getSuperclass());
            }

            throw new TypeException("'" + getClass() + "' extends TypeReference but misses the type parameter. "
                    + "Remove the extension or add a type parameter to it.");
        }

        Type rawType = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
        // TODO remove this when Reflector is fixed to return Types
        if (rawType instanceof ParameterizedType) {
            rawType = ((ParameterizedType) rawType).getRawType();
        }

        return rawType;
    }

    /**
     * TypeHandler类上没有使用@MappedTypes指定对应的javaType时，如果当前的TypeHandler继承了TypeReference抽象类，
     * Mybatis会利用TypeReference的getRawType()方法取到当前TypeHandler泛型对应的javaType类型，
     * 然后利用取得的javaType和TypeHandler以2的方式进行注册，同时还包括一个javaType为null以方式2进行的注册。
     * TypeReference是Mybatis中定义的一个抽象类，主要是用来获取对应的泛型类型。
     *
     * @return
     */
    public final Type getRawType() {
        return rawType;
    }

    @Override
    public String toString() {
        return rawType.toString();
    }

}
