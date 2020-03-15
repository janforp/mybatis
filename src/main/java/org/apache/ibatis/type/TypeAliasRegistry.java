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

package org.apache.ibatis.type;

import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.io.Resources;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * https://blog.csdn.net/majinggogogo/article/details/71503263
 *
 * @author Clinton Begin
 */

/**
 * 类型别名注册机
 */
public class TypeAliasRegistry {

    private final Map<String, Class<?>> TYPE_ALIASES_MAP = new HashMap<String, Class<?>>();

    public TypeAliasRegistry() {
        //构造函数里注册系统内置的类型别名
        registerAlias("string", String.class);

        //基本包装类型
        registerAlias("byte", Byte.class);
        registerAlias("long", Long.class);
        registerAlias("short", Short.class);
        registerAlias("int", Integer.class);
        registerAlias("integer", Integer.class);
        registerAlias("double", Double.class);
        registerAlias("float", Float.class);
        registerAlias("boolean", Boolean.class);

        //基本数组包装类型
        registerAlias("byte[]", Byte[].class);
        registerAlias("long[]", Long[].class);
        registerAlias("short[]", Short[].class);
        registerAlias("int[]", Integer[].class);
        registerAlias("integer[]", Integer[].class);
        registerAlias("double[]", Double[].class);
        registerAlias("float[]", Float[].class);
        registerAlias("boolean[]", Boolean[].class);

        //加个下划线，就变成了基本类型
        registerAlias("_byte", byte.class);
        registerAlias("_long", long.class);
        registerAlias("_short", short.class);
        registerAlias("_int", int.class);
        registerAlias("_integer", int.class);
        registerAlias("_double", double.class);
        registerAlias("_float", float.class);
        registerAlias("_boolean", boolean.class);

        //加个下划线，就变成了基本数组类型
        registerAlias("_byte[]", byte[].class);
        registerAlias("_long[]", long[].class);
        registerAlias("_short[]", short[].class);
        registerAlias("_int[]", int[].class);
        registerAlias("_integer[]", int[].class);
        registerAlias("_double[]", double[].class);
        registerAlias("_float[]", float[].class);
        registerAlias("_boolean[]", boolean[].class);

        //日期数字型
        registerAlias("date", Date.class);
        registerAlias("decimal", BigDecimal.class);
        registerAlias("bigdecimal", BigDecimal.class);
        registerAlias("biginteger", BigInteger.class);
        registerAlias("object", Object.class);

        registerAlias("date[]", Date[].class);
        registerAlias("decimal[]", BigDecimal[].class);
        registerAlias("bigdecimal[]", BigDecimal[].class);
        registerAlias("biginteger[]", BigInteger[].class);
        registerAlias("object[]", Object[].class);

        //集合型
        registerAlias("map", Map.class);
        registerAlias("hashmap", HashMap.class);
        registerAlias("list", List.class);
        registerAlias("arraylist", ArrayList.class);
        registerAlias("collection", Collection.class);
        registerAlias("iterator", Iterator.class);

        //还有个ResultSet型
        registerAlias("ResultSet", ResultSet.class);
    }

    @SuppressWarnings("unchecked")
    // throws class cast exception as well if types cannot be assigned
    //解析类型别名
    public <T> Class<T> resolveAlias(String aliasNameOrFullClassName) {
        try {
            if (aliasNameOrFullClassName == null) {
                return null;
            }
            // issue #748
            //先转成小写再解析
            //这里转个小写也有bug？见748号bug(在google code上)
            //https://code.google.com/p/mybatis/issues
            //比如如果本地语言是Turkish，那i转成大写就不是I了，而是另外一个字符（İ）。这样土耳其的机器就用不了mybatis了！这是一个很大的bug，但是基本上每个人都会犯......
            String key = aliasNameOrFullClassName.toLowerCase(Locale.ENGLISH);
            Class<T> value;
            //原理就很简单了，从HashMap里找对应的键值，找到则返回类型别名对应的Class
            if (TYPE_ALIASES_MAP.containsKey(key)) {
                value = (Class<T>) TYPE_ALIASES_MAP.get(key);
            } else {
                //找不到，再试着将String直接转成Class(这样怪不得我们也可以直接用java.lang.Integer的方式定义，也可以就int这么定义)
                value = (Class<T>) Resources.classForName(aliasNameOrFullClassName);
            }
            return value;
        } catch (ClassNotFoundException e) {
            throw new TypeException("Could not resolve type alias '" + aliasNameOrFullClassName + "'.  Cause: " + e, e);
        }
    }

    /**
     * 而是使用<package>标签，表示扫描该包名下的所有类（除了接口和匿名内部类），如果类名上有注解，则使用注解指定的名称作为别名，
     * 如果没有则使用类名首字母小写作为别名，如com.majing.learning.mybatis.entity.User这个类如果没有设置@Alias注解，则此时会被关联到user这个别名上。
     * ————————————————
     * 版权声明：本文为CSDN博主「DreamMakers」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
     * 原文链接：https://blog.csdn.net/majinggogogo/article/details/71503263
     *
     * @param packageName
     */
    public void registerAliases(String packageName) {
        registerAliases(packageName, Object.class);
    }

    //扫描并注册包下所有继承于superType的类型别名
    public void registerAliases(String packageName, Class<?> superType) {
        //TODO ResolverUtil
        ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
        resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
        Set<Class<? extends Class<?>>> typeSet = resolverUtil.getClasses();
        for (Class<?> type : typeSet) {
            // Ignore inner classes and interfaces (including package-info.java)
            // Skip also inner classes. See issue #6
            if (!type.isAnonymousClass() && !type.isInterface() && !type.isMemberClass()) {
                registerAlias(type);
            }
        }
    }

    /**
     * 注册类型别名
     * 如果有注解，则以注解为主，否则用简单名称
     *
     * @param aliasClass 别名代替的类型
     */
    public void registerAlias(Class<?> aliasClass) {
        //如果没有类型别名，用Class.getSimpleName来注册
        String alias = aliasClass.getSimpleName();
        //或者通过Alias注解来注册(Class.getAnnotation)
        Alias aliasAnnotation = aliasClass.getAnnotation(Alias.class);
        if (aliasAnnotation != null) {
            //以注解优先
            alias = aliasAnnotation.value();
        }
        registerAlias(alias, aliasClass);
    }

    /**
     * 注册类型别名
     *
     * @param aliasName 代替类的名称
     * @param aliasClass 被代替的类
     */
    public void registerAlias(String aliasName, Class<?> aliasClass) {
        if (aliasName == null) {
            throw new TypeException("The parameter aliasName cannot be null");
        }
        // issue #748
        //真正注册的别名全部小写
        String finalAliasName = aliasName.toLowerCase(Locale.ENGLISH);
        //如果已经存在key了，且value和之前不一致，报错
        if (TYPE_ALIASES_MAP.containsKey(finalAliasName) && TYPE_ALIASES_MAP.get(finalAliasName) != null && !TYPE_ALIASES_MAP.get(finalAliasName).equals(aliasClass)) {
            throw new TypeException("The aliasName '" + aliasName + "' is already mapped to the aliasClass '" + TYPE_ALIASES_MAP.get(finalAliasName).getName() + "'.");
        }
        TYPE_ALIASES_MAP.put(finalAliasName, aliasClass);
    }

    public void registerAlias(String alias, String value) {
        try {
            registerAlias(alias, Resources.classForName(value));
        } catch (ClassNotFoundException e) {
            throw new TypeException("Error registering type alias " + alias + " for " + value + ". Cause: " + e, e);
        }
    }

    /**
     * @since 3.2.2
     */
    public Map<String, Class<?>> getTypeAliases() {
        return Collections.unmodifiableMap(TYPE_ALIASES_MAP);
    }

}
