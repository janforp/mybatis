package org.apache.ibatis.session.defaults;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.result.DefaultMapResultHandler;
import org.apache.ibatis.executor.result.DefaultResultContext;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认SqlSession实现
 *
 * @author Clinton Begin
 */
public class DefaultSqlSession implements SqlSession {

    private Configuration configuration;

    private Executor executor;

    /**
     * 是否自动提交
     */
    private boolean autoCommit;

    private boolean dirty;

    public DefaultSqlSession(Configuration configuration, Executor executor, boolean autoCommit) {
        this.configuration = configuration;
        this.executor = executor;
        this.dirty = false;
        this.autoCommit = autoCommit;
    }

    @SuppressWarnings("unused")
    public DefaultSqlSession(Configuration configuration, Executor executor) {
        this(configuration, executor, false);
    }

    /**
     * 核心selectList
     *
     * @param statement Unique identifier matching the statement to use.
     * @param parameter A parameter object to pass to the statement.
     * @param rowBounds Bounds to limit object retrieval
     * @param <E> 范型
     * @return 查询列表结果
     */
    @Override
    public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
        try {
            //根据statement id找到对应的MappedStatement
            MappedStatement mappedStatement = configuration.getMappedStatement(statement);

            //处理过的参数
            Object wrapCollection = wrapCollection(parameter);
            //转而用执行器来查询结果,注意这里传入的ResultHandler是null

            //所有的查询（selectOne, selectList, selectMap），都只关心这里即可
            List<E> objectList = executor.query(mappedStatement, wrapCollection, rowBounds, Executor.NO_RESULT_HANDLER);
            System.out.println(objectList);
            return objectList;

        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    /**
     * 核心selectOne
     *
     * @param statement Unique identifier matching the statement to use.
     * @param parameter A parameter object to pass to the statement.
     * @param <T> 范型
     * @return 查询结果
     */
    @Override
    public <T> T selectOne(String statement, Object parameter) {
        // Popular vote was to return null on 0 results and throw exception on too many.
        //转而去调用selectList,很简单的，如果得到0条则返回null，得到1条则返回1条，得到多条报TooManyResultsException错
        // 特别需要主要的是当没有查询到结果的时候就会返回null。因此一般建议在mapper中编写resultType的时候使用包装类型
        //而不是基本类型，比如推荐使用Integer而不是int。这样就可以避免NPE
        List<T> list = this.selectList(statement, parameter);

        //上面就是到统一的查询接口，下面是 one 的逻辑
        if (list.size() == 1) {
            return list.get(0);
        } else if (list.size() > 1) {
            throw new TooManyResultsException("Expected one result (or null) to be returned by selectOne(), but found: " + list.size());
        } else {
            return null;
        }
    }

    /**
     * 核心selectMap
     *
     * @param statement Unique identifier matching the statement to use.
     * @param parameter A parameter object to pass to the statement.
     * @param mapKey The property to use as key for each value in the list.
     * @param rowBounds Bounds to limit object retrieval
     * @param <K> 范型
     * @param <V> 范型
     * @return 查询结果放到 map
     */
    @Override
    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
        //先调用selectList
        final List<?> list = selectList(statement, parameter, rowBounds);

        //查询结果列表出来之后，获取默认的 Map 结果处理器，通过该处理器把列表转换为 map 的形式
        final DefaultMapResultHandler<K, V> mapResultHandler = new DefaultMapResultHandler<K, V>(mapKey, configuration.getObjectFactory(), configuration.getObjectWrapperFactory());
        //得到一个结果上下文实例，因为 mapResultHandler 的入参数必须是该类型
        final DefaultResultContext resultContext = new DefaultResultContext();
        for (Object object : list) {
            //循环用DefaultMapResultHandler处理每条记录
            resultContext.nextResultObject(object);
            //mapResultHandler 处理结果之后会存到该对象的属性，最后通过 get 获取结果
            mapResultHandler.handleResult(resultContext);
        }
        //注意这个DefaultMapResultHandler里面存了所有已处理的记录(内部实现可能就是一个Map)，最后再返回一个Map
        return mapResultHandler.getMappedResults();
    }

    /**
     * 核心select,带有ResultHandler，和selectList代码差不多的，区别就一个ResultHandler
     *
     * @param statement Unique identifier matching the statement to use.
     * @param parameter 参数
     * @param rowBounds RowBound instance to limit the query results
     * @param handler ResultHandler that will handle each retrieved row
     */
    @Override
    public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
        try {
            MappedStatement ms = configuration.getMappedStatement(statement);
            executor.query(ms, wrapCollection(parameter), rowBounds, handler);
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    /**
     * 核心update
     *
     * @param statement Unique identifier matching the statement to execute.
     * @param parameter A parameter object to pass to the statement.
     * @return 修改影响行
     */
    @Override
    public int update(String statement, Object parameter) {
        try {
            //每次要更新之前，dirty标志设为true
            dirty = true;
            MappedStatement ms = configuration.getMappedStatement(statement);
            //转而用执行器来update结果
            return executor.update(ms, wrapCollection(parameter));
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error updating database.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    /**
     * 核心commit
     *
     * @param force forces connection commit
     */
    @Override
    public void commit(boolean force) {
        try {
            //转而用执行器来commit
            boolean commitOrRollbackRequired = isCommitOrRollbackRequired(force);
            executor.commit(commitOrRollbackRequired);
            //每次commit之后，dirty标志设为false
            dirty = false;
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error committing transaction.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    /**
     * 核心clearCache，清楚执行器的缓存
     */
    @Override
    public void clearCache() {
        //转而用执行器来clearLocalCache
        executor.clearLocalCache();
    }

    @Override
    public <T> T selectOne(String statement) {
        return this.selectOne(statement, null);
    }

    @Override
    public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
        return this.selectMap(statement, null, mapKey, RowBounds.DEFAULT);
    }

    @Override
    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
        return this.selectMap(statement, parameter, mapKey, RowBounds.DEFAULT);
    }

    @Override
    public <E> List<E> selectList(String statement) {
        return this.selectList(statement, null);
    }

    @Override
    public <E> List<E> selectList(String statement, Object parameter) {
        return this.selectList(statement, parameter, RowBounds.DEFAULT);
    }

    @Override
    public void select(String statement, Object parameter, ResultHandler handler) {
        select(statement, parameter, RowBounds.DEFAULT, handler);
    }

    @Override
    public void select(String statement, ResultHandler handler) {
        select(statement, null, RowBounds.DEFAULT, handler);
    }

    @Override
    public int insert(String statement) {
        return insert(statement, null);
    }

    @Override
    public int insert(String statement, Object parameter) {
        //insert也是调用update
        return update(statement, parameter);
    }

    @Override
    public int update(String statement) {
        return update(statement, null);
    }

    @Override
    public int delete(String statement) {
        //delete也是调用update
        return update(statement, null);
    }

    @Override
    public int delete(String statement, Object parameter) {
        return update(statement, parameter);
    }

    @Override
    public void commit() {
        commit(false);
    }

    @Override
    public void rollback() {
        rollback(false);
    }

    //核心rollback
    @Override
    public void rollback(boolean force) {
        try {
            //转而用执行器来rollback
            executor.rollback(isCommitOrRollbackRequired(force));
            //每次rollback之后，dirty标志设为false
            dirty = false;
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error rolling back transaction.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    //核心flushStatements
    @Override
    public List<BatchResult> flushStatements() {
        try {
            //转而用执行器来flushStatements
            return executor.flushStatements();
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error flushing statements.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    //核心close
    @Override
    public void close() {
        try {
            //转而用执行器来close
            executor.close(isCommitOrRollbackRequired(false));
            //每次close之后，dirty标志设为false
            dirty = false;
        } finally {
            ErrorContext.instance().reset();
        }
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public <T> T getMapper(Class<T> type) {
        //最后会去调用MapperRegistry.getMapper
        return configuration.getMapper(type, this);
    }

    @Override
    public Connection getConnection() {
        try {
            return executor.getTransaction().getConnection();
        } catch (SQLException e) {
            throw ExceptionFactory.wrapException("Error getting a new connection.  Cause: " + e, e);
        }
    }

    //检查是否需要强制commit或rollback
    private boolean isCommitOrRollbackRequired(boolean force) {
        return (!autoCommit && dirty) || force;
    }

    //把参数包装成Collection
    private Object wrapCollection(final Object object) {

        //如果是集合类型
        if (object instanceof Collection) {
            //参数若是Collection型，做collection标记
            StrictMap<Object> map = new StrictMap<Object>();
            map.put("collection", object);
            if (object instanceof List) {
                //参数若是List型，做list标记
                map.put("list", object);
            }
            return map;
        }

        //如果是数组
        if (object != null && object.getClass().isArray()) {
            //参数若是数组型，，做array标记
            StrictMap<Object> map = new StrictMap<Object>();
            map.put("array", object);
            return map;
        }
        //参数若不是集合型，也不是数组，直接返回原来值
        return object;
    }

    //严格的Map，如果找不到对应的key，直接抛BindingException例外，而不是返回null
    public static class StrictMap<V> extends HashMap<String, V> {

        private static final long serialVersionUID = -5741767162221585340L;

        @Override
        public V get(Object key) {
            if (!super.containsKey(key)) {
                throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + this.keySet());
            }
            return super.get(key);
        }
    }
}
