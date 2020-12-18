package org.apache.ibatis.executor;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.logging.jdbc.ConnectionLogger;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.apache.ibatis.executor.ExecutionPlaceholder.EXECUTION_PLACEHOLDER;

/**
 * @author Clinton Begin
 */

/**
 * 总结
 * MyBatis一级缓存的生命周期和SqlSession一致。
 * MyBatis一级缓存内部设计简单，只是一个没有容量限定的HashMap，在缓存的功能性上有所欠缺。
 * MyBatis的一级缓存最大范围是SqlSession内部，有多个SqlSession或者分布式的环境下，数据库写操作会引起脏数据，建议设定缓存级别为Statement。
 *
 * 执行器基类
 */
public abstract class BaseExecutor implements Executor {

    private static final Log log = LogFactory.getLog(BaseExecutor.class);

    /**
     * 事务
     */
    protected Transaction transaction;

    /**
     * 被代理执行器，其实就是该实例本身，具体见构造器
     */
    protected Executor wrapper;

    /**
     * 延迟加载队列（线程安全）
     */
    protected ConcurrentLinkedQueue<DeferredLoad> deferredLoads;

    /**
     * 本地缓存
     * 本地缓存机制（Local Cache）防止循环引用（circular references）和加速重复嵌套查询(一级缓存)
     */
    protected PerpetualCache localCache;

    /**
     * 本地输出参数缓存，存储过程
     */
    protected PerpetualCache localOutputParameterCache;

    /**
     * 配置
     */
    protected Configuration configuration;

    /**
     * 查询堆栈
     */
    protected int queryStack = 0;

    /**
     * 是否关闭
     */
    private boolean closed;

    protected BaseExecutor(Configuration configuration, Transaction transaction) {
        this.transaction = transaction;
        this.deferredLoads = new ConcurrentLinkedQueue<DeferredLoad>();
        this.localCache = new PerpetualCache("LocalCache");
        this.localOutputParameterCache = new PerpetualCache("LocalOutputParameterCache");
        this.closed = false;
        this.configuration = configuration;
        this.wrapper = this;
    }

    protected abstract int doUpdate(MappedStatement ms, Object parameter) throws SQLException;

    protected abstract List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException;

    /**
     * query-->queryFromDatabase-->doQuery
     *
     * @param mappedStatement 映射查询
     * @param parameter 参数
     * @param rowBounds 分页参数
     * @param resultHandler 结果处理器
     * @param boundSql sql
     * @param <E> 结果类型
     * @return 查询结果
     * @throws SQLException sql异常
     */
    protected abstract <E> List<E> doQuery(MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException;

    @Override
    public Transaction getTransaction() {
        if (closed) {
            throw new ExecutorException("Executor was closed.");
        }
        return transaction;
    }

    @Override
    public void close(boolean forceRollback) {
        try {
            try {
                rollback(forceRollback);
            } finally {
                if (transaction != null) {
                    transaction.close();
                }
            }
        } catch (SQLException e) {
            // Ignore.  There's nothing that can be done at this point.
            log.warn("Unexpected exception on closing transaction.  Cause: " + e);
        } finally {
            transaction = null;
            deferredLoads = null;
            localCache = null;
            localOutputParameterCache = null;
            closed = true;
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    /**
     * 更新
     * SqlSession.update/insert/delete会调用此方法
     *
     * @param ms 映射的查询
     * @param parameter 参数
     * @return 更新影响行
     * @throws SQLException sql异常
     */
    @Override
    public int update(MappedStatement ms, Object parameter) throws SQLException {
        ErrorContext.instance().resource(ms.getResource()).activity("executing an update").object(ms.getId());
        if (closed) {
            throw new ExecutorException("Executor was closed.");
        }
        //先清局部缓存，再更新，如何更新交由子类，模板方法模式
        clearLocalCache();
        //模版方法设计模式
        return doUpdate(ms, parameter);
    }

    @Override
    public List<BatchResult> flushStatements() throws SQLException {
        return flushStatements(false);
    }

    //刷新语句，Batch用
    public List<BatchResult> flushStatements(boolean isRollBack) throws SQLException {
        if (closed) {
            throw new ExecutorException("Executor was closed.");
        }
        //模版方法
        return doFlushStatements(isRollBack);
    }

    //SqlSession.selectList会调用此方法
    //SqlSession把具体的查询职责委托给了Executor。如果只开启了一级缓存的话，首先会进入BaseExecutor的query方法。代码如下所示：
    @Override
    public <E> List<E> query(MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
        //得到绑定sql
        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        //创建缓存Key
        //只要两条SQL的下列五个值相同，即可以认为是相同的SQL
        //Statement Id + Offset + Limmit + Sql + Params
        CacheKey cacheKey = createCacheKey(mappedStatement, parameter, rowBounds, boundSql);
        //查询
        return query(mappedStatement, parameter, rowBounds, resultHandler, cacheKey, boundSql);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> List<E> query(MappedStatement mappedStatement, Object parameter, RowBounds rowBounds,
            ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException {

        ErrorContext.instance().resource(mappedStatement.getResource()).activity("executing a query").object(mappedStatement.getId());
        //如果已经关闭，报错
        if (closed) {
            throw new ExecutorException("Executor was closed.");
        }
        //先清局部缓存，再查询.但仅查询堆栈为0，才清。为了处理递归调用
        //如果该 statement 要要刷新缓存，则一级，二级缓存都会刷新
        boolean isThisStatementFlushCache = mappedStatement.isFlushCacheRequired();
        if (queryStack == 0 && isThisStatementFlushCache) {
            //刷新二级缓存，开始查询前，并且该statement配置是要刷新缓存，则刷新缓存
            clearLocalCache();
        }

        //用来装查询结果
        List<E> list;
        try {
            //加一,这样递归调用到上面的时候就不会再清局部缓存了
            queryStack++;
            //先根据cacheKey从localCache去查，如果有 resultHandler ，则无法使用缓存
            //如果 resultHandler 为空，则可以是要缓存，但是如果查询前刷新了缓存，则相当于不用缓存
            //一级缓存
            list = (resultHandler == null ? (List<E>) localCache.getObject(cacheKey) : null);
            if (list != null) {
                //缓存命中
                //若查到localCache缓存，处理localOutputParameterCache
                handleLocallyCachedOutputParameters(mappedStatement, cacheKey, parameter, boundSql);
            } else {
                //缓存未命中，从数据库查
                list = queryFromDatabase(mappedStatement, parameter, rowBounds, resultHandler, cacheKey, boundSql);
            }
        } finally {
            //退出一个堆栈
            queryStack--;
        }
        if (queryStack == 0) {
            //延迟加载队列中所有元素
            for (DeferredLoad deferredLoad : deferredLoads) {
                deferredLoad.load();
            }
            // issue #601
            //清空延迟加载队列
            deferredLoads.clear();
            //在query方法执行的最后，会判断一级缓存级别是否是STATEMENT级别，如果是的话，就清空缓存，这也就是STATEMENT级别的一级缓存无法共享localCache的原因。代码如下所示：
            if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
                // issue #482
                //如果是STATEMENT，清除一级缓存
                clearLocalCache();
            }
        }
        return list;
    }

    /**
     * 延迟加载，DefaultResultSetHandler.getNestedQueryMappingValue调用.属于嵌套查询，比较高级.
     *
     * @param ms 映射的查询
     * @param resultObject 加载结果
     * @param property 加载的属性
     * @param key 缓存key
     * @param targetType 目标类型
     */
    @Override
    public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType) {
        if (closed) {
            throw new ExecutorException("Executor was closed.");
        }
        DeferredLoad deferredLoad = new DeferredLoad(resultObject, property, key, localCache, configuration, targetType);
        //如果能加载，则立刻加载，否则加入到延迟加载队列中
        if (deferredLoad.canLoad()) {
            deferredLoad.load();
        } else {
            //这里怎么又new了一个新的，性能有点问题
            deferredLoads.add(new DeferredLoad(resultObject, property, key, localCache, configuration, targetType));
        }
    }

    /**
     * 创建CacheKey
     *
     * @param mappedStatement 映射的查询
     * @param parameterObject 参数
     * @param rowBounds 分页参数
     * @param boundSql sql
     * @return 本次查询的cacheKey
     */
    @Override
    public CacheKey createCacheKey(MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
        if (closed) {
            throw new ExecutorException("Executor was closed.");
        }
        CacheKey cacheKey = new CacheKey();
        //MyBatis 对于其 Key 的生成采取规则为：[mappedStatementId + offset + limit + SQL + queryParams + environment]生成一个哈希码

        String mappedStatementId = mappedStatement.getId();
        cacheKey.update(mappedStatementId);

        int rowBoundsOffset = rowBounds.getOffset();
        cacheKey.update(rowBoundsOffset);

        int rowBoundsLimit = rowBounds.getLimit();
        cacheKey.update(rowBoundsLimit);

        String sql = boundSql.getSql();
        cacheKey.update(sql);

        List<ParameterMapping> parameterMappingList = boundSql.getParameterMappings();
        TypeHandlerRegistry typeHandlerRegistry = mappedStatement.getConfiguration().getTypeHandlerRegistry();
        // mimic DefaultParameterHandler logic
        //模仿DefaultParameterHandler的逻辑,不再重复，请参考DefaultParameterHandler
        for (ParameterMapping parameterMapping : parameterMappingList) {
            ParameterMode parameterMode = parameterMapping.getMode();
            if (parameterMode == ParameterMode.OUT) {
                continue;
            }
            Object value;
            String propertyName = parameterMapping.getProperty();
            if (boundSql.hasAdditionalParameter(propertyName)) {
                value = boundSql.getAdditionalParameter(propertyName);
            } else if (parameterObject == null) {
                value = null;
            } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                value = parameterObject;
            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                value = metaObject.getValue(propertyName);
            }
            cacheKey.update(value);
        }
        if (configuration.getEnvironment() != null) {
            // issue #176
            cacheKey.update(configuration.getEnvironment().getId());
        }
        return cacheKey;
    }

    @Override
    public boolean isCached(MappedStatement mappedStatement, CacheKey cacheKey) {
        return localCache.getObject(cacheKey) != null;
    }

    @Override
    public void commit(boolean required) throws SQLException {
        if (closed) {
            throw new ExecutorException("Cannot commit, transaction is already closed");
        }
        clearLocalCache();
        flushStatements();
        if (required) {
            transaction.commit();
        }
    }

    @Override
    public void rollback(boolean required) throws SQLException {
        if (!closed) {
            try {
                clearLocalCache();
                flushStatements(true);
            } finally {
                if (required) {
                    transaction.rollback();
                }
            }
        }
    }

    @Override
    public void clearLocalCache() {
        if (!closed) {
            localCache.clear();
            localOutputParameterCache.clear();
        }
    }

    protected void closeStatement(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    private void handleLocallyCachedOutputParameters(MappedStatement mappedStatement, CacheKey cacheKey, Object parameter, BoundSql boundSql) {
        //处理存储过程的OUT参数
        StatementType statementType = mappedStatement.getStatementType();
        if (statementType != StatementType.CALLABLE) {
            return;
        }

        //存储过程的出参数也需要缓存
        final Object cachedParameter = localOutputParameterCache.getObject(cacheKey);

        if (cachedParameter != null && parameter != null) {
            final MetaObject metaCachedParameter = configuration.newMetaObject(cachedParameter);
            final MetaObject metaParameter = configuration.newMetaObject(parameter);

            //传入sql的参数列表
            List<ParameterMapping> parameterMappingList = boundSql.getParameterMappings();
            for (ParameterMapping parameterMapping : parameterMappingList) {
                if (parameterMapping.getMode() != ParameterMode.IN) {
                    final String parameterName = parameterMapping.getProperty();
                    final Object cachedValue = metaCachedParameter.getValue(parameterName);
                    //把存储过程的出参数存入 parameter 对象
                    metaParameter.setValue(parameterName, cachedValue);
                }
            }
        }
    }

    /**
     * 从数据库查
     *
     * @param mappedStatement sql相关
     * @param parameter 入参数
     * @param rowBounds 分页
     * @param resultHandler 结果处理器
     * @param cacheKey 缓存Key
     * @param boundSql sql
     * @param <E> 范型
     * @return 查询结果
     * @throws SQLException 异常
     */
    private <E> List<E> queryFromDatabase(MappedStatement mappedStatement, Object parameter, RowBounds rowBounds,
            ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException {

        List<E> list;
        //TODO 先向缓存中放入占位符？？？
        localCache.putObject(cacheKey, EXECUTION_PLACEHOLDER);
        try {
            //模版方法，交给 SimpleExecutor等其他执行器执行
            list = doQuery(mappedStatement, parameter, rowBounds, resultHandler, boundSql);
        } finally {
            //最后删除占位符
            localCache.removeObject(cacheKey);
        }
        //加入以及缓存
        localCache.putObject(cacheKey, list);
        //如果是存储过程，OUT参数也加入缓存
        if (mappedStatement.getStatementType() == StatementType.CALLABLE) {
            localOutputParameterCache.putObject(cacheKey, parameter);
        }
        return list;
    }

    protected Connection getConnection(Log statementLog) throws SQLException {
        Connection connection = transaction.getConnection();
        if (statementLog.isDebugEnabled()) {
            //如果需要打印Connection的日志，返回一个ConnectionLogger(代理模式, AOP思想)
            return ConnectionLogger.newInstance(connection, statementLog, queryStack);
        } else {
            return connection;
        }
    }

    @Override
    public void setExecutorWrapper(Executor wrapper) {
        this.wrapper = wrapper;
    }

    /**
     * 延迟加载
     */
    private static class DeferredLoad {

        /**
         * 被加载数据所属的实例
         */
        private final MetaObject resultObject;

        /**
         * 被加载的属性
         */
        private final String property;

        /**
         * 加载的类型
         */
        private final Class<?> targetType;

        private final CacheKey key;

        /**
         * 永久缓存，使用hashMap
         */
        private final PerpetualCache localCache;

        private final ObjectFactory objectFactory;

        /**
         * 结果提取器
         */
        private final ResultExtractor resultExtractor;

        // issue #781
        public DeferredLoad(MetaObject resultObject, String property, CacheKey key, PerpetualCache localCache, Configuration configuration, Class<?> targetType) {
            this.resultObject = resultObject;
            this.property = property;
            this.key = key;
            this.localCache = localCache;
            this.objectFactory = configuration.getObjectFactory();
            this.resultExtractor = new ResultExtractor(configuration, objectFactory);
            this.targetType = targetType;
        }

        public boolean canLoad() {
            //缓存中找到，且不为占位符，代表可以加载
            return localCache.getObject(key) != null && localCache.getObject(key) != EXECUTION_PLACEHOLDER;
        }

        /**
         * 加载
         */
        public void load() {
            // we suppose we get back a List
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) localCache.getObject(key);
            //调用ResultExtractor.extractObjectFromList
            Object value = resultExtractor.extractObjectFromList(list, targetType);
            resultObject.setValue(property, value);
        }
    }
}
