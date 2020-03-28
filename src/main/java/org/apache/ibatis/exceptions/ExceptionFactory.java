package org.apache.ibatis.exceptions;

import org.apache.ibatis.executor.ErrorContext;

/**
 * @author Clinton Begin
 */

/**
 * 异常工厂
 */
public class ExceptionFactory {

    private ExceptionFactory() {
        // Prevent Instantiation
    }

    //把普通异常包装成mybatis自己的PersistenceException
    public static RuntimeException wrapException(String message, Exception e) {
        //查找错误上下文，得到错误原因，传给PersistenceException
        //每个线程都会有一个ErrorContext，所以可以得到，  .message(message).cause是典型的构建器模式
        return new PersistenceException(ErrorContext.instance().message(message).cause(e).toString(), e);
    }
}
