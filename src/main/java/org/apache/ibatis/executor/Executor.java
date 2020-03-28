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
 * @author Clinton Begin
 * @see ExecutorType
 */
public interface Executor {

    //不需要ResultHandler
    ResultHandler NO_RESULT_HANDLER = null;

    //更新
    int update(MappedStatement ms, Object parameter) throws SQLException;

    //查询，带分页，带缓存，BoundSql
    <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException;

    //查询，带分页
    <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException;

    //刷新批处理语句
    List<BatchResult> flushStatements() throws SQLException;

    //提交和回滚，参数是是否要强制
    void commit(boolean required) throws SQLException;

    void rollback(boolean required) throws SQLException;

    //创建CacheKey
    CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql);

    //判断是否缓存了
    boolean isCached(MappedStatement ms, CacheKey key);

    //清理Session缓存
    void clearLocalCache();

    //延迟加载
    void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType);

    Transaction getTransaction();

    void close(boolean forceRollback);

    boolean isClosed();

    void setExecutorWrapper(Executor executor);
}
