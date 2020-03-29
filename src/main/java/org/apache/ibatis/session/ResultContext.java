package org.apache.ibatis.session;

/**
 * @author Clinton Begin
 */

/**
 * 结果上下文
 * 查询的结果都会先放到该对象
 */
public interface ResultContext {

    /**
     * 获取当前处理之后的结果，一般是一条记录
     *
     * @return 获取当前处理之后的结果，一般是一条记录
     */
    Object getResultObject();

    /**
     * 获取记录数，分页的时候有用到
     *
     * @return 获取记录数
     */
    int getResultCount();

    /**
     * 是否停止
     *
     * @return 是否停止
     */
    boolean isStopped();

    /**
     * 使用该值让mybatis停止加载更多的数据
     */
    void stop();
}
