package org.apache.ibatis.session;

/**
 * 执行器的类型
 *
 * 在测试中使用simple模式提交10000条数据，时间为18248 毫秒，batch模式为5023 ，性能提高70%；
 *
 * @author Clinton Begin
 */
public enum ExecutorType {

    /**
     * 这个执行器类型不做特殊的事情。它为每个语句的执行创建一个新的预处理语句。
     */
    SIMPLE,

    /**
     * 这个执行器类型会复用预处理语句。
     */
    REUSE,

    /**
     * 这个执行器会批量执行所有更新语句，如果SELECT在它们中间执行还会标定它们是必须的，来保证一个简单并易于理解的行为。
     *
     * 但batch模式也有自己的问题，比如在Insert操作时，在事务没有提交之前，是没有办法获取到自增的id，这在某型情形下是不符合业务要求的；
     */
    BATCH
}
