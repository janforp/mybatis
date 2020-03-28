package org.apache.ibatis.executor;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

import java.sql.SQLException;
import java.util.List;

/**
 * 执行器
 * SqlSession向用户提供操作数据库的方法，但和数据库操作有关的职责都会委托给Executor。
 *
 * 有三种类型的执行器：ExecutorType
 *
 * /**
 * * 这个执行器类型不做特殊的事情。它为每个语句的执行创建一个新的预处理语句。
 * SIMPLE,
 *
 * /**
 * * 这个执行器类型会复用预处理语句。
 * REUSE,
 *
 * * 这个执行器会批量执行所有更新语句，如果SELECT在它们中间执行还会标定它们是必须的，来保证一个简单并易于理解的行为。
 * *
 * * 但batch模式也有自己的问题，比如在Insert操作时，在事务没有提交之前，是没有办法获取到自增的id，这在某型情形下是不符合业务要求的；
 *
 * BATCH
 *
 * @author Clinton Begin
 * @see ExecutorType
 */
public interface Executor {

    /**
     * 不需要ResultHandler
     */
    ResultHandler NO_RESULT_HANDLER = null;

    /**
     * 更新
     * SqlSession.update/insert/delete会调用此方法
     *
     * @param ms 映射的查询
     * @param parameter 参数
     * @return 更新影响行
     * @throws SQLException sql异常
     */
    int update(MappedStatement ms, Object parameter) throws SQLException;

    /**
     * 查询，带分页，带缓存，BoundSql
     *
     * @param ms 映射的查询
     * @param parameter 参数
     * @param rowBounds 分页参数
     * @param resultHandler 结果处理器
     * @param cacheKey 缓存key
     * @param boundSql sql
     * @param <E> 结果类型
     * @return 查询结果
     * @throws SQLException sql异常
     */
    <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException;

    /**
     * 查询，带分页
     *
     * @param ms 映射的查询
     * @param parameter 参数
     * @param rowBounds 分页参数
     * @param resultHandler 结果处理器
     * @param <E> 结果类型
     * @return 查询结果
     * @throws SQLException sql异常
     */
    <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException;

    /**
     * 刷新批处理语句
     *
     * @return 批处理结果
     * @throws SQLException sql异常
     */
    List<BatchResult> flushStatements() throws SQLException;

    /**
     * 提交和回滚，参数是是否要强制
     *
     * @param required 是否要强制提交
     * @throws SQLException sql异常
     */
    void commit(boolean required) throws SQLException;

    /**
     * 提交和回滚，参数是是否要强制
     *
     * @param required 是否要强制提交
     * @throws SQLException sql异常
     */
    void rollback(boolean required) throws SQLException;

    /**
     * 创建CacheKey
     *
     * @param ms 映射的查询
     * @param parameterObject 参数
     * @param rowBounds 分页参数
     * @param boundSql sql
     * @return 本次查询的cacheKey
     */
    CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql);

    /**
     * 判断是否缓存了
     *
     * @param ms 映射的查询
     * @param key 本次查询的cacheKey
     * @return 判断是否缓存了
     */
    boolean isCached(MappedStatement ms, CacheKey key);

    /**
     * 清理Session缓存
     */
    void clearLocalCache();

    /**
     * 延迟加载
     *
     * @param ms 映射的查询
     * @param resultObject 加载结果
     * @param property 加载的属性
     * @param key 缓存key
     * @param targetType 目标类型
     */
    void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType);

    Transaction getTransaction();

    void close(boolean forceRollback);

    boolean isClosed();

    /**
     * 包装执行器
     *
     * @param executor 被包装的执行器
     */
    void setExecutorWrapper(Executor executor);
}
