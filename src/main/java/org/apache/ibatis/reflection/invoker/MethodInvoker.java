package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Clinton Begin
 */

/**
 * 方法调用者
 */
public class MethodInvoker implements Invoker {

    /**
     * 该invoker返回的type
     */
    private Class<?> type;

    /**
     * 方法
     */
    private Method method;

    public MethodInvoker(Method method) {
        this.method = method;
        //如果只有一个参数，返回参数类型，否则返回return的类型
        if (method.getParameterTypes().length == 1) {
            type = method.getParameterTypes()[0];
        } else {
            type = method.getReturnType();
        }
    }

    //就是调用Method.invoke
    @Override
    public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
        return method.invoke(target, args);
    }

    @Override
    public Class<?> getType() {
        return type;
    }
}
