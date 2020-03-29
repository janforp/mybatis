package org.apache.ibatis.executor;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cache.TransactionalCacheManager;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

import java.sql.SQLException;
import java.util.List;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */

/**
 * 二级缓存执行器
 *
 * 一级缓存中，其最大的共享范围就是一个SqlSession内部，如果多个SqlSession之间需要共享缓存，
 * 则需要使用到二级缓存。开启二级缓存后，会使用CachingExecutor装饰Executor，进入一级缓存的查询流程前，先在CachingExecutor进行二级缓存的查询
 *
 * 二级缓存开启后，同一个namespace下的所有操作语句，都影响着同一个Cache，即二级缓存被多个SqlSession共享，是一个全局的变量。
 * 当开启缓存后，数据的查询执行的流程就是 二级缓存 -> 一级缓存 -> 数据库。
 */
public class CachingExecutor implements Executor {

    /**
     * 装饰者模式
     */
    private Executor delegate;

    /**
     * 事务缓存管理器，被CachingExecutor使用
     */
    private TransactionalCacheManager transactionalCacheManager = new TransactionalCacheManager();

    public CachingExecutor(Executor delegate) {
        this.delegate = delegate;
        delegate.setExecutorWrapper(this);
    }

    @Override
    public Transaction getTransaction() {
        return delegate.getTransaction();
    }

    @Override
    public void close(boolean forceRollback) {
        try {
            //issues #499, #524 and #573
            if (forceRollback) {
                transactionalCacheManager.rollback();
            } else {
                transactionalCacheManager.commit();
            }
        } finally {
            delegate.close(forceRollback);
        }
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public int update(MappedStatement ms, Object parameterObject) throws SQLException {
        //根据配置决定是否刷新缓存完再update
        flushCacheIfRequired(ms);
        //被装饰者去做事情
        return delegate.update(ms, parameterObject);
    }

    @Override
    public <E> List<E> query(MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {

        BoundSql boundSql = mappedStatement.getBoundSql(parameterObject);
        //query时传入一个cacheKey参数
        CacheKey cacheKey = createCacheKey(mappedStatement, parameterObject, rowBounds, boundSql);
        return query(mappedStatement, parameterObject, rowBounds, resultHandler, cacheKey, boundSql);
    }

    //被ResultLoader.selectList调用
    @Override
    public <E> List<E> query(MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds,
            ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException {

        Cache cache = mappedStatement.getCache();
        //默认情况下是没有开启缓存的(二级缓存).要开启二级缓存,你需要在你的 SQL 映射文件中添加一行: <cache/>
        //简单的说，就是先查CacheKey，查不到再委托给实际的执行器去查

        //是否在该statement中配置了 useCache = "true"
        boolean isThisStatementUseCache = (cache != null);
        if (isThisStatementUseCache) {
            flushCacheIfRequired(mappedStatement);
            //resultHandler 什么缓存都无法使用
            if (mappedStatement.isUseCache() && resultHandler == null) {
                //Caching stored procedures with OUT params is not supported，所有参数的模式必须是 IN，否则不支持二级缓存
                ensureNoOutParams(mappedStatement, parameterObject, boundSql);
                //先拿缓存
                @SuppressWarnings("unchecked")
                List<E> list = (List<E>) transactionalCacheManager.getObject(cache, cacheKey);
                if (list == null) {
                    //没缓存，去数据库，去数据库查询是交给被代理者
                    list = delegate.query(mappedStatement, parameterObject, rowBounds, resultHandler, cacheKey, boundSql);
                    //缓存结果
                    transactionalCacheManager.putObject(cache, cacheKey, list); // issue #578 and #116
                }
                return list;
            }
        }
        return delegate.query(mappedStatement, parameterObject, rowBounds, resultHandler, cacheKey, boundSql);
    }

    @Override
    public List<BatchResult> flushStatements() throws SQLException {
        return delegate.flushStatements();
    }

    @Override
    public void commit(boolean required) throws SQLException {
        delegate.commit(required);
        transactionalCacheManager.commit();
    }

    @Override
    public void rollback(boolean required) throws SQLException {
        try {
            delegate.rollback(required);
        } finally {
            if (required) {
                transactionalCacheManager.rollback();
            }
        }
    }

    @SuppressWarnings("unused")
    private void ensureNoOutParams(MappedStatement ms, Object parameter, BoundSql boundSql) {
        if (ms.getStatementType() == StatementType.CALLABLE) {
            //#{property,javaType=int,jdbcType=NUMERIC}列表
            List<ParameterMapping> parameterMappingList = boundSql.getParameterMappings();
            for (ParameterMapping parameterMapping : parameterMappingList) {
                if (parameterMapping.getMode() != ParameterMode.IN) {
                    throw new ExecutorException("Caching stored procedures with OUT params is not supported.  Please configure useCache=false in " + ms.getId() + " statement.");
                }
            }
        }
    }

    @Override
    public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
        return delegate.createCacheKey(ms, parameterObject, rowBounds, boundSql);
    }

    @Override
    public boolean isCached(MappedStatement ms, CacheKey key) {
        return delegate.isCached(ms, key);
    }

    @Override
    public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType) {
        delegate.deferLoad(ms, resultObject, property, key, targetType);
    }

    @Override
    public void clearLocalCache() {
        delegate.clearLocalCache();
    }

    /**
     * 通过该 映射sql的配置决定是否需要清除缓存
     *
     * @param mappedStatement 具体的映射语句
     */
    private void flushCacheIfRequired(MappedStatement mappedStatement) {
        Cache cache = mappedStatement.getCache();
        if (cache != null && mappedStatement.isFlushCacheRequired()) {
            transactionalCacheManager.clear(cache);
        }
    }

    @Override
    public void setExecutorWrapper(Executor executor) {
        throw new UnsupportedOperationException("This method should not be called");
    }
}
