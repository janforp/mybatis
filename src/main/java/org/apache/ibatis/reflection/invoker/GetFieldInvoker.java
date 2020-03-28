package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Clinton Begin
 */

/**
 * getter调用者
 */
public class GetFieldInvoker implements Invoker {

    /**
     * 属性
     */
    private Field field;

    public GetFieldInvoker(Field field) {
        this.field = field;
    }

    /**
     * 就是调用Field.get
     *
     * @param target 实例对象
     * @param args 方法参数
     * @return 调用函数的结果
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @Override
    public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
        return field.get(target);
    }

    @Override
    public Class<?> getType() {
        return field.getType();
    }
}
