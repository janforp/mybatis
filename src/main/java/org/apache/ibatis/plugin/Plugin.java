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

package org.apache.ibatis.plugin;

import org.apache.ibatis.reflection.ExceptionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Clinton Begin
 */

/**
 * 插件,用的代理模式
 */
public class Plugin implements InvocationHandler {

    private Object target;

    private Interceptor interceptor;

    private Map<Class<?>, Set<Method>> signatureMap;

    private Plugin(Object target, Interceptor interceptor, Map<Class<?>, Set<Method>> signatureMap) {
        this.target = target;
        this.interceptor = interceptor;
        this.signatureMap = signatureMap;
    }

    public static Object wrap(Object target, Interceptor interceptor) {
        //把该interceptor的所有拦截的方法按class归类
        Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
        //取得要改变行为的类(ParameterHandler|ResultSetHandler|StatementHandler|Executor)
        Class<?> targetClass = target.getClass();
        //取得接口
        Class<?>[] interfaces = getAllInterfaces(targetClass, signatureMap);
        //产生代理
        //返回代理对象
        if (interfaces.length > 0) {
            return Proxy.newProxyInstance(
                    targetClass.getClassLoader(),
                    interfaces,
                    //当执行被拦截当方法时，真正执行当是：org.apache.ibatis.plugin.Plugin.invoke
                    new Plugin(target, interceptor, signatureMap));
        }
        //否则返回原对象
        return target;
    }

    /**
     * 把该interceptor的所有拦截的方法按class归类
     *
     * @param interceptor
     * @return
     */
    private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {
        //取Intercepts注解，例子可参见ExamplePlugin.java
        //获取拦截器上的注解
        Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);
        // issue #251
        //必须得有Intercepts注解，没有报错
        if (interceptsAnnotation == null) {
            throw new PluginException("No @Intercepts annotation was found in interceptor " + interceptor.getClass().getName());
        }
        //value是数组型，Signature的数组
        //获取所以方法签名
        Signature[] signatures = interceptsAnnotation.value();
        //每个class里有多个Method需要被拦截,所以这么定义
        Map<Class<?>, Set<Method>> signatureMap = new HashMap<Class<?>, Set<Method>>();
        for (Signature signature : signatures) {
            Class<?> intercepteredClass = signature.type();
            Set<Method> intercepteredMethodsInOneClass = signatureMap.get(intercepteredClass);
            if (intercepteredMethodsInOneClass == null) {
                intercepteredMethodsInOneClass = new HashSet<Method>();
                signatureMap.put(intercepteredClass, intercepteredMethodsInOneClass);
            }
            String intercepteredMethod = signature.method();
            try {
                Class<?>[] methodArgs = signature.args();
                Method method = intercepteredClass.getMethod(intercepteredMethod, methodArgs);
                intercepteredMethodsInOneClass.add(method);
            } catch (NoSuchMethodException e) {
                throw new PluginException("Could not find method on " + intercepteredClass + " named " + intercepteredMethod + ". Cause: " + e, e);
            }
        }
        return signatureMap;
    }

    /**
     * 取得接口
     *
     * @param targetClass 代理类型
     * @param signatureMap 需要拦截的方法
     * @return
     */
    private static Class<?>[] getAllInterfaces(Class<?> targetClass, Map<Class<?>, Set<Method>> signatureMap) {
        Set<Class<?>> interfaces = new HashSet<Class<?>>();
        while (targetClass != null) {
            Class<?>[] targetClassInterfaces = targetClass.getInterfaces();
            for (Class<?> c : targetClassInterfaces) {
                //貌似只能拦截ParameterHandler|ResultSetHandler|StatementHandler|Executor
                //拦截其他的无效
                //当然我们可以覆盖Plugin.wrap方法，达到拦截其他类的功能
                if (signatureMap.containsKey(c)) {
                    interfaces.add(c);
                }
            }
            targetClass = targetClass.getSuperclass();
        }
        return interfaces.toArray(new Class<?>[0]);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            //看看如何拦截
            Set<Method> methods = signatureMap.get(method.getDeclaringClass());
            //看哪些方法需要拦截
            if (methods != null && methods.contains(method)) {
                //调用Interceptor.intercept，也即插入了我们自己的逻辑
                return interceptor.intercept(new Invocation(target, method, args));
            }
            //最后还是执行原来逻辑
            return method.invoke(target, args);
        } catch (Exception e) {
            throw ExceptionUtil.unwrapThrowable(e);
        }
    }

}
