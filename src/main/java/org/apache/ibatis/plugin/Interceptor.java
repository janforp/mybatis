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

import java.util.Properties;

/**
 * 使用插件是非常简单的，只需实现 Interceptor 接口，并指定想要拦截的方法签名即可。
 *
 * MyBatis 允许你在映射语句执行过程中的某一点进行拦截调用。默认情况下，MyBatis 允许使用插件来拦截的方法调用包括：
 *
 * Executor (update, query, flushStatements, commit, rollback, getTransaction, close, isClosed)
 * ParameterHandler (getParameterObject, setParameters)
 * ResultSetHandler (handleResultSets, handleOutputParameters)
 * StatementHandler (prepare, parameterize, batch, update, query)
 *
 * 拦截器的一个作用就是我们可以拦截某些方法的调用，我们可以选择在这些被拦截的方法执行前后加上某些逻辑，
 * 也可以在执行这些被拦截的方法时执行自己的逻辑而不再执行被拦截的方法。Mybatis 拦截器设计的一个初衷就是为了供用户在某些时候可以实现自己的逻辑而不必去动
 * Mybatis 固有的逻辑。打个比方，对于 Executor，Mybatis 中有几种实现：BatchExecutor、ReuseExecutor、SimpleExecutor
 * 和 CachingExecutor。这个时候如果你觉得这几种实现对于 Executor 接口的 query 方法都不能满足你的要求，那怎么办呢？
 * 是要去改源码吗？当然不。我们可以建立一个Mybatis 拦截器用于拦截 Executor 接口的 query 方法，在拦截之后实现自己的 query 方法逻辑，
 * 之后可以选择是否继续执行原来的 query 方法
 *
 * // ExamplePlugin.java
 *
 * @author Clinton Begin
 * @Intercepts({@Signature( type= Executor.class,
 * method = "update",
 * args = {MappedStatement.class,Object.class})})
 * public class ExamplePlugin implements Interceptor {
 * private Properties properties = new Properties();
 * public Object intercept(Invocation invocation) throws Throwable {
 * // implement pre processing if need
 * Object returnObject = invocation.proceed();
 * // implement post processing if need
 * return returnObject;
 * }
 * public void setProperties(Properties properties) {
 * this.properties = properties;
 * }
 * }
 *
 * 上面的插件将会拦截在 Executor 实例中所有的 “update” 方法调用， 这里的 Executor 是负责执行底层映射语句的内部对象。
 *
 * 定义自己的Interceptor最重要的是要实现 plugin 方法和 intercept 方法，在 plugin
 * 方法中我们可以决定是否要进行拦截进而决定要返回一个什么样的目标对象。
 * 而 intercept 方法就是要进行拦截的时候要执行的方法。
 */
public interface Interceptor {

    /**
     * @param invocation { 代理对象，被监控方法对象，当前被监控方法运行时需要的实参 }
     * @return
     * @throws Throwable
     */
    Object intercept(Invocation invocation) throws Throwable;

    /**
     * @param target 表示被拦截的对象，此处为 Executor 的实例对象
     * 作用：如果被拦截对象所在的类有实现接口，就为当前拦截对象生成一个代理对象
     * 如果被拦截对象所在的类没有指定接口，这个对象之后的行为就不会被代理操作
     * @return
     */
    Object plugin(Object target);

    //MyBatis 允许你在某一点拦截已映射语句执行的调用。默认情况下,MyBatis 允许使用插件来拦截方法调用
    //<plugins>
    //  <plugin interceptor="org.mybatis.example.ExamplePlugin">
    //    <property name="someProperty" value="100"/>
    //  </plugin>
    //</plugins>

    /**
     * 设置属性
     *
     * @param properties <property name="someProperty" value="100"/>
     */
    void setProperties(Properties properties);

}
