package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.InvocationTargetException;

/**
 * 调用者
 *
 * @author Clinton Begin
 */
public interface Invoker {

    /**
     * 调用
     *
     * @param target 实例对象
     * @param args 方法参数
     * @return 调用结果
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException;

    /**
     * 取得类型,根据不同的实现返回的类型不一样
     * @return 取得类型
     */
    Class<?> getType();
}
