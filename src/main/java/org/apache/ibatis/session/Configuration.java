package org.apache.ibatis.session;

import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.builder.annotation.MethodResolver;
import org.apache.ibatis.builder.xml.XMLStatementBuilder;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.FifoCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.SoftCache;
import org.apache.ibatis.cache.decorators.WeakCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.datasource.jndi.JndiDataSourceFactory;
import org.apache.ibatis.datasource.pooled.PooledDataSourceFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;
import org.apache.ibatis.executor.BatchExecutor;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ReuseExecutor;
import org.apache.ibatis.executor.SimpleExecutor;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.executor.loader.cglib.CglibProxyFactory;
import org.apache.ibatis.executor.loader.javassist.JavassistProxyFactory;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl;
import org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.logging.log4j2.Log4j2Impl;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.InterceptorChain;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.LanguageDriverRegistry;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Clinton Begin
 * @see < https://mybatis.org/mybatis-3/zh/configuration.html
 */
public class Configuration {

    protected final InterceptorChain interceptorChain = new InterceptorChain();

    //类型处理器注册机
    @Getter
    protected final TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();

    //类型别名注册机
    @Getter
    protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();

    @Getter
    protected final LanguageDriverRegistry languageRegistry = new LanguageDriverRegistry();

    /**
     * 映射的语句,存在Map里
     * key : mapperInterface.getName() + "." + method.getName()
     * value: 映射的语句
     */
    protected final Map<String, MappedStatement> mappedStatements = new StrictMap<MappedStatement>("Mapped Statements collection");

    /**
     * 二级缓存
     *
     * key:namespace如：org.apache.ibatis.submitted.force_flush_on_select.PersonMapper
     * value:缓存实例
     */
    protected final Map<String, Cache> cacheMap = new StrictMap<Cache>("Caches collection");

    /**
     * 结果映射,存在Map里
     * key：resultMap 的id
     * value：ResultMap
     */
    protected final Map<String, ResultMap> resultMaps = new StrictMap<ResultMap>("Result Maps collection");

    protected final Map<String, ParameterMap> parameterMaps = new StrictMap<ParameterMap>("Parameter Maps collection");

    /**
     * key:org.apache.ibatis.submitted.selectkey.Table1.insert!selectKey
     * value:主键生成器
     */
    protected final Map<String, KeyGenerator> keyGenerators = new StrictMap<KeyGenerator>("Key Generators collection");

    /**
     * A map holds cache-ref relationship. The key is the namespace that
     * references a cache bound to another namespace and the value is the
     * namespace which the actual cache is bound to.
     *
     * cache=ref 缓存
     * key:当前mapper文件的 namespace
     * value:当前mapper文件的 cache-ref的namespace
     */
    protected final Map<String, String> cacheRefMap = new HashMap<String, String>();

    //加载过的资源，避免重复加载
    protected final Set<String> loadedResources = new HashSet<String>();

    @Getter
    protected final Map<String, XNode> sqlFragments = new StrictMap<XNode>("XML fragments parsed from previous mappers");

    //不完整的SQL语句
    @Getter
    protected final Collection<XMLStatementBuilder> incompleteStatements = new LinkedList<XMLStatementBuilder>();

    @Getter
    protected final Collection<CacheRefResolver> incompleteCacheRefs = new LinkedList<CacheRefResolver>();

    //未完成的映射
    @Getter
    protected final Collection<ResultMapResolver> incompleteResultMaps = new LinkedList<ResultMapResolver>();

    @Getter
    protected final Collection<MethodResolver> incompleteMethods = new LinkedList<MethodResolver>();

    //环境
    @Getter
    @Setter
    protected Environment environment;

    //---------以下都是<settings>节点-------

    /**
     * 是否允许在嵌套语句中使用分页（RowBounds）。如果允许使用则设置为 false。
     */
    @Setter
    @Getter
    protected boolean safeRowBoundsEnabled = false;
    //---------以上都是<settings>节点-------

    /**
     * 是否允许在嵌套语句中使用结果处理器（ResultHandler）。如果允许使用则设置为 false
     */
    @Setter
    @Getter
    protected boolean safeResultHandlerEnabled = true;

    /**
     * 是否开启驼峰命名自动映射，即从经典数据库列名 A_COLUMN 映射到经典 Java 属性名 aColumn。
     */
    @Getter
    @Setter
    protected boolean mapUnderscoreToCamelCase = false;

    /**
     * 开启时，任一方法的调用都会加载该对象的所有延迟加载属性。 否则，每个延迟加载属性会按需加载（参考 lazyLoadTriggerMethods)。
     */
    @Setter
    @Getter
    protected boolean aggressiveLazyLoading = true;

    /**
     * 是否允许单个语句返回多结果集（需要数据库驱动支持）
     */
    @Setter
    @Getter
    protected boolean multipleResultSetsEnabled = true;

    /**
     * 允许 JDBC 支持自动生成主键，需要数据库驱动支持。如果设置为 true，
     * 将强制使用自动生成主键。尽管一些数据库驱动不支持此特性，但仍可正常工作（如 Derby）
     */
    @Setter
    @Getter
    protected boolean useGeneratedKeys = false;

    /**
     * 使用列标签代替列名。实际表现依赖于数据库驱动，具体可参考数据库驱动的相关文档，或通过对比测试来观察。
     */
    @Setter
    @Getter
    protected boolean useColumnLabel = true;

    /**
     * 全局性地开启或关闭所有映射器配置文件中已配置的任何缓存。
     *
     * <setting name="cacheEnabled" value="true"/>
     */
    @Getter
    @Setter
    protected boolean cacheEnabled = true;

    /**
     * 指定当结果集中值为 null 的时候是否调用映射对象的 setter（map 对象时为 put）方法，
     * 这在依赖于 Map.keySet() 或 null 值进行初始化时比较有用。
     * 注意基本类型（int、boolean 等）是不能设置成 null 的。
     */
    @Getter
    @Setter
    protected boolean callSettersOnNulls = false;

    /**
     * 指定 MyBatis 增加到日志名称的前缀。
     */
    @Getter
    @Setter
    protected String logPrefix;

    /**
     * 指定 MyBatis 所用日志的具体实现，未指定时将自动查找。
     */
    @Getter
    protected Class<? extends Log> logImpl;

    /**
     * MyBatis 利用本地缓存机制（Local Cache）防止循环引用和加速重复的嵌套查询。
     * 默认值为 SESSION，会缓存一个会话中执行的所有查询。
     * 若设置值为 STATEMENT，本地缓存将仅用于执行语句，对相同 SqlSession 的不同查询将不会进行缓存。
     *
     * SESSION | STATEMENT
     */
    @Getter
    @Setter
    protected LocalCacheScope localCacheScope = LocalCacheScope.SESSION;

    /**
     * 当没有为参数指定特定的 JDBC 类型时，空值的默认 JDBC 类型。
     * 某些数据库驱动需要指定列的 JDBC 类型，多数情况直接用一般类型即可，
     * 比如 NULL、VARCHAR 或 OTHER。
     */
    @Getter
    @Setter
    protected JdbcType jdbcTypeForNull = JdbcType.OTHER;

    /**
     * 指定对象的哪些方法触发一次延迟加载。
     */
    @Getter
    @Setter
    protected Set<String> lazyLoadTriggerMethods = new HashSet<String>(Arrays.asList(new String[] { "equals", "clone", "hashCode", "toString" }));

    /**
     * 设置超时时间，它决定数据库驱动等待数据库响应的秒数。
     */
    @Getter
    @Setter
    protected Integer defaultStatementTimeout;

    /**
     * 默认为简单执行器
     *
     * 配置默认的执行器。SIMPLE 就是普通的执行器；
     * REUSE 执行器会重用预处理语句（PreparedStatement）；
     * BATCH 执行器不仅重用语句还会执行批量更新。
     *
     * @see Executor
     */
    @Getter
    @Setter
    protected ExecutorType defaultExecutorType = ExecutorType.SIMPLE;

    @Getter
    @Setter
    protected AutoMappingBehavior autoMappingBehavior = AutoMappingBehavior.PARTIAL;

    /**
     * // this property value should be replaced on all mapper files
     * Properties properties = new Properties();
     * properties.put("property", "id");
     *
     * // create a SqlSessionFactory
     * Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/propertiesinmapperfiles/mybatis-config.xml");
     * SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
     * sqlSessionFactory = builder.build(reader, properties);
     */
    @Getter
    @Setter
    protected Properties variables = new Properties();

    //对象工厂和对象包装器工厂
    @Getter
    @Setter
    protected ObjectFactory objectFactory = new DefaultObjectFactory();

    @Getter
    @Setter
    protected ObjectWrapperFactory objectWrapperFactory = new DefaultObjectWrapperFactory();

    //映射注册机
    @Getter
    @Setter
    protected MapperRegistry mapperRegistry = new MapperRegistry(this);

    //默认禁用延迟加载
    @Getter
    @Setter
    protected boolean lazyLoadingEnabled = false;

    @Getter
    protected ProxyFactory proxyFactory = new JavassistProxyFactory(); // #224 Using internal Javassist instead of OGNL

    @Getter
    @Setter
    protected String databaseId;

    /**
     * Configuration factory class.
     * Used to create Configuration for loading deserialized unread properties.
     *
     * @see <a href='https://code.google.com/p/mybatis/issues/detail?id=300'>Issue 300</a> (google code)
     */
    @Getter
    @Setter
    protected Class<?> configurationFactory;

    public Configuration() {
        //注册更多的类型别名，至于为何不直接在TypeAliasRegistry里注册，还需进一步研究
        typeAliasRegistry.registerAlias("JDBC", JdbcTransactionFactory.class);
        typeAliasRegistry.registerAlias("MANAGED", ManagedTransactionFactory.class);

        typeAliasRegistry.registerAlias("JNDI", JndiDataSourceFactory.class);
        typeAliasRegistry.registerAlias("POOLED", PooledDataSourceFactory.class);
        typeAliasRegistry.registerAlias("UNPOOLED", UnpooledDataSourceFactory.class);

        typeAliasRegistry.registerAlias("PERPETUAL", PerpetualCache.class);
        typeAliasRegistry.registerAlias("FIFO", FifoCache.class);
        typeAliasRegistry.registerAlias("LRU", LruCache.class);
        typeAliasRegistry.registerAlias("SOFT", SoftCache.class);
        typeAliasRegistry.registerAlias("WEAK", WeakCache.class);

        typeAliasRegistry.registerAlias("DB_VENDOR", VendorDatabaseIdProvider.class);

        typeAliasRegistry.registerAlias("XML", XMLLanguageDriver.class);
        typeAliasRegistry.registerAlias("RAW", RawLanguageDriver.class);

        typeAliasRegistry.registerAlias("SLF4J", Slf4jImpl.class);
        typeAliasRegistry.registerAlias("COMMONS_LOGGING", JakartaCommonsLoggingImpl.class);
        typeAliasRegistry.registerAlias("LOG4J", Log4jImpl.class);
        typeAliasRegistry.registerAlias("LOG4J2", Log4j2Impl.class);
        typeAliasRegistry.registerAlias("JDK_LOGGING", Jdk14LoggingImpl.class);
        typeAliasRegistry.registerAlias("STDOUT_LOGGING", StdOutImpl.class);
        typeAliasRegistry.registerAlias("NO_LOGGING", NoLoggingImpl.class);

        typeAliasRegistry.registerAlias("CGLIB", CglibProxyFactory.class);
        typeAliasRegistry.registerAlias("JAVASSIST", JavassistProxyFactory.class);

        languageRegistry.setDefaultDriverClass(XMLLanguageDriver.class);
        languageRegistry.register(RawLanguageDriver.class);
    }

    public Configuration(Environment environment) {
        this();
        this.environment = environment;
    }

    /**
     * 创建元对象,可以当做任何对象
     *
     * @param object 对象
     * @return 该对象的元对象
     */
    public MetaObject newMetaObject(Object object) {
        return MetaObject.forObject(object, objectFactory, objectWrapperFactory);
    }

    /**
     * 创建参数处理器，返回一个 org.apache.ibatis.scripting.defaults.DefaultParameterHandler 默认的参数处理器
     *
     * 可以通过 typeHandler.setParameter(preparedStatement, parameterIndex, value, jdbcType);设置参数
     *
     * @param mappedStatement 映射语句对象
     * @param parameterObject 参数
     * @param boundSql 绑定的sql
     * @return 参数处理器
     */
    public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
        LanguageDriver languageDriver = mappedStatement.getLang();
        //创建ParameterHandler
        ParameterHandler parameterHandler = languageDriver.createParameterHandler(mappedStatement, parameterObject, boundSql);
        //插件在这里插入
        parameterHandler = (ParameterHandler) interceptorChain.pluginAll(parameterHandler);
        return parameterHandler;
    }

    /**
     * 创建结果集处理器，返回一个 org.apache.ibatis.executor.resultset.DefaultResultSetHandler
     *
     * <E> List<E> handleResultSets(Statement statement) throws SQLException;
     * void handleOutputParameters(CallableStatement callableStatement) throws SQLException;
     *
     * @param executor 执行器
     * @param mappedStatement 映射语句对象
     * @param rowBounds 分页参数
     * @param parameterHandler 参数处理器
     * @param resultHandler 结果处理器
     * @param boundSql 绑定的sql
     * @return 创建结果集处理器
     */
    public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds,
            ParameterHandler parameterHandler, ResultHandler resultHandler, BoundSql boundSql) {

        //创建DefaultResultSetHandler(稍老一点的版本3.1是创建NestedResultSetHandler或者FastResultSetHandler)
        ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, parameterHandler, resultHandler, boundSql, rowBounds);
        //插件在这里插入
        resultSetHandler = (ResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
        return resultSetHandler;
    }

    /**
     * 创建语句处理器，一共有3种类型的处理器，通过
     * RoutingStatementHandler ，根据 mappedStatement.getStatementType() 的类型，决定使用具体的statement处理器，共
     * * 普通的 statement
     * STATEMENT,
     *
     * * 预处理，可以防止 sql 注入
     * PREPARED,
     *
     * * 存储过程
     * CALLABLE
     *
     * public RoutingStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
     *
     * //根据语句类型，委派到不同的语句处理器(STATEMENT|PREPARED|CALLABLE)
     * StatementType statementType = mappedStatement.getStatementType();
     * switch (statementType) {
     * case STATEMENT:
     * delegate = new SimpleStatementHandler(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
     * break;
     * case PREPARED:
     * delegate = new PreparedStatementHandler(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
     * break;
     * case CALLABLE:
     * delegate = new CallableStatementHandler(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
     * break;
     * default:
     * throw new ExecutorException("Unknown statement type: " + statementType);
     * }
     *
     * }
     *
     * @param executor 执行器
     * @param mappedStatement 映射语句
     * @param parameterObject 参数
     * @param rowBounds 分页传输
     * @param resultHandler 结果处理器
     * @param boundSql 绑定的sql
     * @return 创建语句处理器
     */
    public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject,
            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {

        //创建路由选择语句处理器，这个 RoutingStatementHandler 是具体三中类型的代理
        StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
        //插件在这里插入
        //TODO ?
        statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
        return statementHandler;
    }

    /**
     * 在这是二级缓存的入口
     *
     * 产生执行器，执行器有3种类型：
     *
     * * 这个执行器类型不做特殊的事情。它为每个语句的执行创建一个新的预处理语句。
     * SIMPLE,
     * * 这个执行器类型会复用预处理语句。
     * REUSE,
     *
     * * 这个执行器会批量执行所有更新语句，如果SELECT在它们中间执行还会标定它们是必须的，来保证一个简单并易于理解的行为。
     * *
     * * 但batch模式也有自己的问题，比如在Insert操作时，在事务没有提交之前，是没有办法获取到自增的id，这在某型情形下是不符合业务要求的；
     * BATCH
     *
     * 但是如果开启了二级缓存，则生成 CachingExecutor， 该执行器只是统一管理缓存，具体的操作数据库还是通过代理交给具体的上面3个执行器中的一个
     *
     * @param transaction 事务
     * @param executorType 执行器类型，可以为空，如果空，则选择默认的类型
     * @return 一个执行器
     */
    public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
        executorType = (executorType == null ? defaultExecutorType : executorType);
        //这句再做一下保护,囧,防止粗心大意的人将defaultExecutorType设成null?
        executorType = (executorType == null ? ExecutorType.SIMPLE : executorType);
        //上面的目的保证 executorType 不能空，如果用户没指定就用 ExecutorType.SIMPLE

        Executor executor;
        //然后就是简单的3个分支，产生3种执行器BatchExecutor/ReuseExecutor/SimpleExecutor
        if (ExecutorType.BATCH == executorType) {
            executor = new BatchExecutor(this, transaction);
        } else if (ExecutorType.REUSE == executorType) {
            executor = new ReuseExecutor(this, transaction);
        } else {
            executor = new SimpleExecutor(this, transaction);
        }
        //如果要使用二级缓存，生成另一种CachingExecutor,装饰者模式
        if (cacheEnabled) {
            //一级缓存中，其最大的共享范围就是一个SqlSession内部，如果多个SqlSession之间需要共享缓存，则需要使用到二级缓存。
            // 开启二级缓存后，会使用CachingExecutor装饰Executor，进入一级缓存的查询流程前，先在CachingExecutor进行二级缓存的查询
            executor = new CachingExecutor(executor);
        }
        //此处调用插件,通过插件可以改变Executor行为
        executor = (Executor) interceptorChain.pluginAll(executor);
        return executor;
    }

    public Executor newExecutor(Transaction transaction) {
        return newExecutor(transaction, defaultExecutorType);
    }

    @SuppressWarnings("unchecked")
    public void setLogImpl(Class<?> logImpl) {
        if (logImpl != null) {
            this.logImpl = (Class<? extends Log>) logImpl;
            LogFactory.useCustomLogging(this.logImpl);
        }
    }

    public void addLoadedResource(String resource) {
        loadedResources.add(resource);
    }

    public boolean isResourceLoaded(String resource) {
        return loadedResources.contains(resource);
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        if (proxyFactory == null) {
            proxyFactory = new JavassistProxyFactory();
        }
        this.proxyFactory = proxyFactory;
    }

    /**
     * @since 3.2.2
     */
    public List<Interceptor> getInterceptors() {
        return interceptorChain.getInterceptors();
    }

    public void setDefaultScriptingLanguage(Class<?> driver) {
        if (driver == null) {
            driver = XMLLanguageDriver.class;
        }
        getLanguageRegistry().setDefaultDriverClass(driver);
    }

    public LanguageDriver getDefaultScriptingLanuageInstance() {
        return languageRegistry.getDefaultDriver();
    }

    public void addKeyGenerator(String id, KeyGenerator keyGenerator) {
        keyGenerators.put(id, keyGenerator);
    }

    public Collection<String> getKeyGeneratorNames() {
        return keyGenerators.keySet();
    }

    public Collection<KeyGenerator> getKeyGenerators() {
        return keyGenerators.values();
    }

    public KeyGenerator getKeyGenerator(String id) {
        return keyGenerators.get(id);
    }

    public boolean hasKeyGenerator(String id) {
        return keyGenerators.containsKey(id);
    }

    public void addCache(Cache cache) {
        //org.apache.ibatis.submitted.force_flush_on_select.PersonMapper
        String id = cache.getId();
        cacheMap.put(id, cache);
    }

    public Collection<String> getCacheNames() {
        return cacheMap.keySet();
    }

    public Collection<Cache> getCacheMap() {
        return cacheMap.values();
    }

    public Cache getCache(String id) {
        return cacheMap.get(id);
    }

    public boolean hasCache(String id) {
        return cacheMap.containsKey(id);
    }

    public void addResultMap(ResultMap rm) {
        //org.apache.ibatis.submitted.force_flush_on_select.PersonMapper.personMap
        String id = rm.getId();
        resultMaps.put(id, rm);
        checkLocallyForDiscriminatedNestedResultMaps(rm);
        checkGloballyForDiscriminatedNestedResultMaps(rm);
    }

    public Collection<String> getResultMapNames() {
        return resultMaps.keySet();
    }

    public Collection<ResultMap> getResultMaps() {
        return resultMaps.values();
    }

    public ResultMap getResultMap(String id) {
        return resultMaps.get(id);
    }

    public boolean hasResultMap(String id) {
        return resultMaps.containsKey(id);
    }

    public void addParameterMap(ParameterMap pm) {
        parameterMaps.put(pm.getId(), pm);
    }

    public Collection<String> getParameterMapNames() {
        return parameterMaps.keySet();
    }

    public Collection<ParameterMap> getParameterMaps() {
        return parameterMaps.values();
    }

    public ParameterMap getParameterMap(String id) {
        return parameterMaps.get(id);
    }

    public boolean hasParameterMap(String id) {
        return parameterMaps.containsKey(id);
    }

    public void addMappedStatement(MappedStatement ms) {
        mappedStatements.put(ms.getId(), ms);
    }

    public Collection<String> getMappedStatementNames() {
        buildAllStatements();
        return mappedStatements.keySet();
    }

    public Collection<MappedStatement> getMappedStatements() {
        buildAllStatements();
        return mappedStatements.values();
    }

    public void addIncompleteStatement(XMLStatementBuilder incompleteStatement) {
        incompleteStatements.add(incompleteStatement);
    }

    public void addIncompleteCacheRef(CacheRefResolver incompleteCacheRef) {
        incompleteCacheRefs.add(incompleteCacheRef);
    }

    public void addIncompleteResultMap(ResultMapResolver resultMapResolver) {
        incompleteResultMaps.add(resultMapResolver);
    }

    public void addIncompleteMethod(MethodResolver builder) {
        incompleteMethods.add(builder);
    }

    //由DefaultSqlSession.selectList调用过来
    public MappedStatement getMappedStatement(String id) {
        return this.getMappedStatement(id, true);
    }

    public MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements) {
        //先构建所有语句，再返回语句
        if (validateIncompleteStatements) {
            buildAllStatements();
        }
        return mappedStatements.get(id);
    }

    public void addInterceptor(Interceptor interceptor) {
        interceptorChain.addInterceptor(interceptor);
    }

    //将包下所有类加入到mapper
    public void addMappers(String packageName, Class<?> superType) {
        mapperRegistry.addMappers(packageName, superType);
    }

    public void addMappers(String packageName) {
        mapperRegistry.addMappers(packageName);
    }

    public <T> void addMapper(Class<T> type) {
        mapperRegistry.addMapper(type);
    }

    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
        return mapperRegistry.getMapper(type, sqlSession);
    }

    public boolean hasMapper(Class<?> type) {
        return mapperRegistry.hasMapper(type);
    }

    public boolean hasStatement(String statementName) {
        return hasStatement(statementName, true);
    }

    public boolean hasStatement(String statementName, boolean validateIncompleteStatements) {
        if (validateIncompleteStatements) {
            buildAllStatements();
        }
        return mappedStatements.containsKey(statementName);
    }

    public void addCacheRef(String namespace, String referencedNamespace) {
        cacheRefMap.put(namespace, referencedNamespace);
    }

    /*
     * Parses all the unprocessed statement nodes in the cache. It is recommended
     * to call this method once all the mappers are added as it provides fail-fast
     * statement validation.
     */
    protected void buildAllStatements() {
        if (!incompleteResultMaps.isEmpty()) {
            synchronized (incompleteResultMaps) {
                // This always throws a BuilderException.
                incompleteResultMaps.iterator().next().resolve();
            }
        }
        if (!incompleteCacheRefs.isEmpty()) {
            synchronized (incompleteCacheRefs) {
                // This always throws a BuilderException.
                incompleteCacheRefs.iterator().next().resolveCacheRef();
            }
        }
        if (!incompleteStatements.isEmpty()) {
            synchronized (incompleteStatements) {
                // This always throws a BuilderException.
                incompleteStatements.iterator().next().parseStatementNode();
            }
        }
        if (!incompleteMethods.isEmpty()) {
            synchronized (incompleteMethods) {
                // This always throws a BuilderException.
                incompleteMethods.iterator().next().resolve();
            }
        }
    }

    /*
     * Extracts namespace from fully qualified statement id.
     *
     * @param statementId
     * @return namespace or null when id does not contain period.
     */
    protected String extractNamespace(String statementId) {
        int lastPeriod = statementId.lastIndexOf('.');
        return lastPeriod > 0 ? statementId.substring(0, lastPeriod) : null;
    }

    // Slow but a one time cost. A better solution is welcome.
    protected void checkGloballyForDiscriminatedNestedResultMaps(ResultMap rm) {
        if (rm.hasNestedResultMaps()) {
            for (Map.Entry<String, ResultMap> entry : resultMaps.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof ResultMap) {
                    ResultMap entryResultMap = (ResultMap) value;
                    if (!entryResultMap.hasNestedResultMaps() && entryResultMap.getDiscriminator() != null) {
                        Collection<String> discriminatedResultMapNames = entryResultMap.getDiscriminator().getDiscriminatorMap().values();
                        if (discriminatedResultMapNames.contains(rm.getId())) {
                            entryResultMap.forceNestedResultMaps();
                        }
                    }
                }
            }
        }
    }

    // Slow but a one time cost. A better solution is welcome.
    protected void checkLocallyForDiscriminatedNestedResultMaps(ResultMap rm) {
        if (!rm.hasNestedResultMaps() && rm.getDiscriminator() != null) {
            for (Map.Entry<String, String> entry : rm.getDiscriminator().getDiscriminatorMap().entrySet()) {
                String discriminatedResultMapName = entry.getValue();
                if (hasResultMap(discriminatedResultMapName)) {
                    ResultMap discriminatedResultMap = resultMaps.get(discriminatedResultMapName);
                    if (discriminatedResultMap.hasNestedResultMaps()) {
                        rm.forceNestedResultMaps();
                        break;
                    }
                }
            }
        }
    }

    /**
     * 静态内部类,严格的Map，不允许多次覆盖key所对应的value
     *
     * @param <V> 值类型
     */
    protected static class StrictMap<V> extends HashMap<String, V> {

        private static final long serialVersionUID = -4950446264854982944L;

        /**
         * 给每一个实例指定一个名称，在抛出异常的地方，可以指明是什么场景
         */
        private String name;

        public StrictMap(String name, int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
            this.name = name;
        }

        public StrictMap(String name, int initialCapacity) {
            super(initialCapacity);
            this.name = name;
        }

        public StrictMap(String name) {
            super();
            this.name = name;
        }

        public StrictMap(String name, Map<String, ? extends V> m) {
            super(m);
            this.name = name;
        }

        @SuppressWarnings("unchecked")
        @Override
        public V put(String key, V value) {
            if (super.containsKey(key)) {
                //如果已经存在此key了，直接报错
                throw new IllegalArgumentException(name + " already contains value for " + key);
            }
            if (key.contains(".")) {
                //如果有.符号，取得短名称，大致用意就是包名不同，类名相同，提供模糊查询的功能
                final String shortKey = getShortName(key);
                if (super.get(shortKey) == null) {
                    super.put(shortKey, value);
                } else {
                    //如果已经有此缩略，表示模糊，放一个Ambiguity型的
                    super.put(shortKey, (V) new Ambiguity(shortKey));
                }
            }
            //再放一个全名
            return super.put(key, value);
            //可以看到，如果有包名，会放2个key到这个map，一个缩略，一个全名
        }

        @Override
        public V get(Object key) {
            V value = super.get(key);
            //如果找不到相应的key，直接报错
            if (value == null) {
                throw new IllegalArgumentException(name + " does not contain value for " + key);
            }
            //如果是模糊型的，也报错，提示用户
            //原来这个模糊型就是为了提示用户啊
            if (value instanceof Ambiguity) {
                throw new IllegalArgumentException(((Ambiguity) value).getSubject() + " is ambiguous in " + name
                        + " (try using the full name including the namespace, or rename one of the entries)");
            }
            return value;
        }

        /**
         * com.janita.service ----->  servic
         *
         * @param key 键
         * @return 获取最后 . 后面的值
         */
        private String getShortName(String key) {
            //按 . 分隔字符串到数组中
            final String[] keyParts = key.split("\\.");
            //获取数组最后一个值
            return keyParts[keyParts.length - 1];
        }

        //模糊，居然放在Map里面的一个静态内部类，
        protected static class Ambiguity {

            //提供一个主题
            private String subject;

            public Ambiguity(String subject) {
                this.subject = subject;
            }

            public String getSubject() {
                return subject;
            }
        }
    }
}
