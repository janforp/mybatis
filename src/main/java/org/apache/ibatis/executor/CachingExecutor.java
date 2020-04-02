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
     * 被装饰的对象，其中大部分功能由该对象实现
     *
     * CachingExecutor 通过添加一些逻辑实现了比 delegateExecutor 更多的一些功能而已
     */
    private Executor delegateExecutor;

    /**
     * 事务缓存管理器，被CachingExecutor使用
     */
    private TransactionalCacheManager transactionalCacheManager = new TransactionalCacheManager();

    public CachingExecutor(Executor delegateExecutor) {
        this.delegateExecutor = delegateExecutor;
        delegateExecutor.setExecutorWrapper(this);
    }

    @Override
    public Transaction getTransaction() {
        return delegateExecutor.getTransaction();
    }

    /**
     * 通过该 映射sql的配置决定是否需要清除缓存
     * 这是用户在配置sql的地方指定是否需要刷新，是否使用缓存
     *
     * @param mappedStatement 具体的映射语句
     */
    private void flushCacheIfRequired(MappedStatement mappedStatement) {
        Cache cache = mappedStatement.getCache();
        //在对应namespace的mapper文件加：<cache flushInterval="3600000"/>
        //在对应的sql添加 <select id="selectByIdFlush" resultMap="personMap" parameterType="int" flushCache="true">，则刷新，否则该sql不刷新
        boolean flushCacheRequired = mappedStatement.isFlushCacheRequired();
        if (cache != null && flushCacheRequired) {
            //如果该 statement 要要刷新缓存，则一级，二级缓存都会傻笑
            transactionalCacheManager.clear(cache);
        }
    }

    @Override
    public int update(MappedStatement ms, Object parameterObject) throws SQLException {
        //根据配置决定是否刷新缓存完再update
        flushCacheIfRequired(ms);
        //被装饰者去做事情
        return delegateExecutor.update(ms, parameterObject);
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

        //TODO <cache flushInterval="3600000"/> 则true?
        boolean isThisNamespaceUseCache = (cache != null);
        if (isThisNamespaceUseCache) {
            flushCacheIfRequired(mappedStatement);// <select id="selectByIdFlush" resultMap="personMap" parameterType="int" flushCache="true">
            //当该namespace开启了二级缓存，则里面的statement默认使用缓存，除非指定 useCache="false"
            boolean isThisStatementUseCache = mappedStatement.isUseCache();
            if (isThisStatementUseCache && resultHandler == null) {//resultHandler 什么缓存都无法使用
                //Caching stored procedures with OUT params is not supported，所有参数的模式必须是 IN，否则不支持二级缓存
                ensureNoOutParams(mappedStatement, parameterObject, boundSql);
                //先从二级缓存拿
                @SuppressWarnings("unchecked")
                List<E> list = (List<E>) transactionalCacheManager.getObject(cache, cacheKey);
                if (list == null) {//二级缓存没命中
                    //二级缓存没命中，去被代理的执行器，在哪里会进行一级缓存查询
                    list = delegateExecutor.query(mappedStatement, parameterObject, rowBounds, null, cacheKey, boundSql);
                    //查询结果存入二级缓存
                    transactionalCacheManager.putObject(cache, cacheKey, list); // issue #578 and #116
                }
                return list;
            }
        }
        //该sql没有配置使用二级缓存，则直接去数据库查询
        return delegateExecutor.query(mappedStatement, parameterObject, rowBounds, resultHandler, cacheKey, boundSql);
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
            delegateExecutor.close(forceRollback);
        }
    }

    @Override
    public boolean isClosed() {
        return delegateExecutor.isClosed();
    }

    @Override
    public List<BatchResult> flushStatements() throws SQLException {
        return delegateExecutor.flushStatements();
    }

    @Override
    public void commit(boolean required) throws SQLException {
        delegateExecutor.commit(required);
        transactionalCacheManager.commit();
    }

    @Override
    public void rollback(boolean required) throws SQLException {
        try {
            delegateExecutor.rollback(required);
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
        return delegateExecutor.createCacheKey(ms, parameterObject, rowBounds, boundSql);
    }

    @Override
    public boolean isCached(MappedStatement ms, CacheKey key) {
        return delegateExecutor.isCached(ms, key);
    }

    @Override
    public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType) {
        delegateExecutor.deferLoad(ms, resultObject, property, key, targetType);
    }

    @Override
    public void clearLocalCache() {
        delegateExecutor.clearLocalCache();
    }

    @Override
    public void setExecutorWrapper(Executor executor) {
        throw new UnsupportedOperationException("This method should not be called");
    }
}
