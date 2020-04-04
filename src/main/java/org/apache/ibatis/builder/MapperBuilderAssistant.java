package org.apache.ibatis.builder;

import lombok.Getter;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.CacheBuilder;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.LanguageDriverRegistry;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * 映射构建器助手，建造者模式,继承BaseBuilder
 *
 * @author Clinton Begin
 */
public class MapperBuilderAssistant extends BaseBuilder {

    //每个助手都有1个namespace,resource,cache
    @Getter
    private String currentNamespace;

    private String resource;

    /**
     * 对应 namespace 的二级缓存，如果该 mapper.xml 配置了 cache-ref,则该实例就是引用的缓存实例，一级缓存也是这个实例
     */
    private Cache currentCache;

    // issue #676
    private boolean unresolvedCacheRef;

    public MapperBuilderAssistant(Configuration configuration, String resource) {
        super(configuration);
        ErrorContext.instance().resource(resource);
        this.resource = resource;
    }

    public void setCurrentNamespace(String currentNamespace) {
        if (currentNamespace == null) {
            throw new BuilderException("The mapper element requires a namespace attribute to be specified.");
        }

        if (this.currentNamespace != null && !this.currentNamespace.equals(currentNamespace)) {
            throw new BuilderException("Wrong namespace. Expected '"
                    + this.currentNamespace + "' but found '" + currentNamespace + "'.");
        }

        this.currentNamespace = currentNamespace;
    }

    /**
     * 为id加上namespace前缀，如selectPerson-->org.a.b.selectPerson
     *
     * base ------>   currentNamespace + "." + base;
     *
     * @param base
     * @param isReference
     * @return
     */
    public String applyCurrentNamespace(String base, boolean isReference) {
        if (base == null) {
            return null;
        }
        if (isReference) {
            // is it qualified with any namespace yet?
            if (base.contains(".")) {
                return base;
            }
        } else {
            // is it qualified with this namespace yet?
            if (base.startsWith(currentNamespace + ".")) {
                return base;
            }
            if (base.contains(".")) {
                throw new BuilderException("Dots are not allowed in element names, please remove it from " + base);
            }
        }
        return currentNamespace + "." + base;
    }

    /**
     * 二级缓存引用
     *
     * @param namespace 命名空间
     * @return 一个缓存实例
     */
    public Cache useCacheRef(String namespace) {
        if (namespace == null) {
            throw new BuilderException("cache-ref element requires a namespace attribute.");
        }
        try {
            unresolvedCacheRef = true;
            Cache cache = configuration.getCache(namespace);
            if (cache == null) {
                throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.");
            }
            currentCache = cache;
            unresolvedCacheRef = false;
            return cache;
        } catch (IllegalArgumentException e) {
            //该异常会被 org.apache.ibatis.builder.xml.XMLMapperBuilder.cacheRefElement 捕获，
            //然后该 namespace 会被 configuration.addIncompleteCacheRef(cacheRefResolver);
            throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.", e);
        }
    }

    /**
     * 生成 namespace 的二级 cache 实例
     *
     * @param typeClass 缓存类型
     * @param evictionClass 回收策略类
     * @param flushInterval 刷新时间
     * @param size 大写
     * @param readWrite 读写
     * @param blocking 足赛
     * @param props 属性配置
     * @return 生成cache
     */
    public Cache useNewCache(Class<? extends Cache> typeClass, Class<? extends Cache> evictionClass,
            Long flushInterval, Integer size, boolean readWrite, boolean blocking, Properties props) {
        //这里面又判断了一下是否为null就用默认值，有点和XMLMapperBuilder.cacheElement逻辑重复了
        typeClass = valueOrDefault(typeClass, PerpetualCache.class);
        evictionClass = valueOrDefault(evictionClass, LruCache.class);
        //调用CacheBuilder构建cache,id=currentNamespace
        //得到一个缓存实例
        Cache cache = new CacheBuilder(currentNamespace)
                .implementation(typeClass)
                .addDecorator(evictionClass)
                .clearInterval(flushInterval)
                .size(size)
                .readWrite(readWrite)
                .blocking(blocking)
                .properties(props)
                .build();
        //加入缓存
        configuration.addCache(cache);
        //当前的缓存
        currentCache = cache;
        return cache;
    }

    public ParameterMap addParameterMap(String id, Class<?> parameterClass, List<ParameterMapping> parameterMappings) {
        id = applyCurrentNamespace(id, false);
        ParameterMap.Builder parameterMapBuilder = new ParameterMap.Builder(configuration, id, parameterClass, parameterMappings);
        ParameterMap parameterMap = parameterMapBuilder.build();
        configuration.addParameterMap(parameterMap);
        return parameterMap;
    }

    public ParameterMapping buildParameterMapping(
            Class<?> parameterType,
            String property,
            Class<?> javaType,
            JdbcType jdbcType,
            String resultMap,
            ParameterMode parameterMode,
            Class<? extends TypeHandler<?>> typeHandler,
            Integer numericScale) {
        resultMap = applyCurrentNamespace(resultMap, true);

        // Class parameterType = parameterMapBuilder.type();
        Class<?> javaTypeClass = resolveParameterJavaType(parameterType, property, javaType, jdbcType);
        TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, typeHandler);

        ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, javaTypeClass);
        builder.jdbcType(jdbcType);
        builder.resultMapId(resultMap);
        builder.mode(parameterMode);
        builder.numericScale(numericScale);
        builder.typeHandler(typeHandlerInstance);
        return builder.build();
    }

    /**
     * 增加ResultMap 传入的 resultMap 的解析结果 ，把 ResultMap 对象存入 configuration
     *
     * @param id resultMap 的 id
     * @param type resultMap 的 type
     * @param extend
     * @param discriminator 鉴别器
     * @param resultMappings resultMap 的具体映射字段列表
     * @param autoMapping resultMap 上配置的是否自动映射
     * @return ResultMap
     */
    public ResultMap addResultMap(String id, Class<?> type, String extend, Discriminator discriminator, List<ResultMapping> resultMappings, Boolean autoMapping) {
        //org.apache.ibatis.submitted.force_flush_on_select.PersonMapper.personMap
        id = applyCurrentNamespace(id, false);
        extend = applyCurrentNamespace(extend, true);

        //建造者模式
        ResultMap.Builder resultMapBuilder = new ResultMap.Builder(configuration, id, type, resultMappings, autoMapping);
        if (extend != null) {
            if (!configuration.hasResultMap(extend)) {
                throw new IncompleteElementException("Could not find a parent resultmap with id '" + extend + "'");
            }
            ResultMap resultMap = configuration.getResultMap(extend);
            List<ResultMapping> extendedResultMappings = new ArrayList<ResultMapping>(resultMap.getResultMappings());
            extendedResultMappings.removeAll(resultMappings);
            // Remove parent constructor if this resultMap declares a constructor.
            boolean declaresConstructor = false;
            for (ResultMapping resultMapping : resultMappings) {
                if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
                    declaresConstructor = true;
                    break;
                }
            }
            if (declaresConstructor) {
                Iterator<ResultMapping> extendedResultMappingsIter = extendedResultMappings.iterator();
                while (extendedResultMappingsIter.hasNext()) {
                    if (extendedResultMappingsIter.next().getFlags().contains(ResultFlag.CONSTRUCTOR)) {
                        extendedResultMappingsIter.remove();
                    }
                }
            }
            resultMappings.addAll(extendedResultMappings);
        }
        resultMapBuilder.discriminator(discriminator);
        ResultMap resultMap = resultMapBuilder.build();
        configuration.addResultMap(resultMap);
        return resultMap;
    }

    public Discriminator buildDiscriminator(
            Class<?> resultType,
            String column,
            Class<?> javaType,
            JdbcType jdbcType,
            Class<? extends TypeHandler<?>> typeHandler,
            Map<String, String> discriminatorMap) {
        ResultMapping resultMapping = buildResultMapping(
                resultType,
                null,
                column,
                javaType,
                jdbcType,
                null,
                null,
                null,
                null,
                typeHandler,
                new ArrayList<ResultFlag>(),
                null,
                null,
                false);
        Map<String, String> namespaceDiscriminatorMap = new HashMap<String, String>();
        for (Map.Entry<String, String> e : discriminatorMap.entrySet()) {
            String resultMap = e.getValue();
            resultMap = applyCurrentNamespace(resultMap, true);
            namespaceDiscriminatorMap.put(e.getKey(), resultMap);
        }
        Discriminator.Builder discriminatorBuilder = new Discriminator.Builder(configuration, resultMapping, namespaceDiscriminatorMap);
        return discriminatorBuilder.build();
    }

    /**
     * 增加映射语句
     *
     * @param id 如果是主键，则 sql的id + "!selectKey"如：insertOne!selectKey
     * @param sqlSource 静态 sqlNode 以及 动态 sqlNode 列表，各自包括标签中的表达式
     * @param statementType
     * @param sqlCommandType
     * @param fetchSize
     * @param timeout
     * @param parameterMap
     * @param parameterType
     * @param resultMap
     * @param resultType
     * @param resultSetType
     * @param flushCache
     * @param useCache
     * @param resultOrdered
     * @param keyGenerator
     * @param keyProperty
     * @param keyColumn
     * @param databaseId
     * @param lang
     * @param resultSets
     * @return MappedStatement
     */
    public MappedStatement addMappedStatement(String id, SqlSource sqlSource, StatementType statementType,
            SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap,
            Class<?> parameterType, String resultMap, Class<?> resultType, ResultSetType resultSetType,
            boolean flushCache, boolean useCache, boolean resultOrdered, KeyGenerator keyGenerator,
            String keyProperty, String keyColumn, String databaseId, LanguageDriver lang, String resultSets) {

        if (unresolvedCacheRef) {
            throw new IncompleteElementException("Cache-ref not yet resolved");
        }

        //为id加上namespace前缀如：org.apache.ibatis.submitted.force_flush_on_select.PersonMapper.selectByIdFlush或者org.apache.ibatis.submitted.selectkey.Table1.insert!selectKey
        id = applyCurrentNamespace(id, false);
        //是否是select语句
        boolean isSelect = (sqlCommandType == SqlCommandType.SELECT);

        //又是建造者模式
        MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType);
        statementBuilder.resource(resource);
        statementBuilder.fetchSize(fetchSize);
        statementBuilder.statementType(statementType);
        //TODO 在new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType); 中不是已经指定 keyGenerator 啦，这里又重复指定？
        statementBuilder.keyGenerator(keyGenerator);
        statementBuilder.keyProperty(keyProperty);
        statementBuilder.keyColumn(keyColumn);
        statementBuilder.databaseId(databaseId);
        //TODO 在new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType); 中不是已经指定 lang 啦，这里又重复指定？
        statementBuilder.lang(lang);
        statementBuilder.resultOrdered(resultOrdered);
        statementBuilder.resultSets(resultSets);

        //设置超时时间，如果该sql没有单独指定超时时间，在是默认的配置
        setStatementTimeout(timeout, statementBuilder);

        //1.参数映射
        setStatementParameterMap(parameterMap, parameterType, statementBuilder);
        //2.结果映射
        setStatementResultMap(resultMap, resultType, resultSetType, statementBuilder);

        //缓存
        setStatementCache(isSelect, flushCache, useCache, currentCache, statementBuilder);

        //构造 MappedStatement
        MappedStatement statement = statementBuilder.build();
        //建造好调用configuration.addMappedStatement
        configuration.addMappedStatement(statement);
        return statement;
    }

    private <T> T valueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 针对某个Sql的缓存
     *
     * @param isSelect 是否查询
     * @param flushCache 是否刷新
     * @param useCache 是否使用缓存
     * @param cache mapper对应的缓存实例，二级缓存实例
     * @param statementBuilder 该sql的构造器
     */
    private void setStatementCache(boolean isSelect, boolean flushCache, boolean useCache, Cache cache, MappedStatement.Builder statementBuilder) {
        //如果没有指定，则查询不刷新
        flushCache = valueOrDefault(flushCache, !isSelect);
        //如果没指定，则查询默认使用缓存，非查询不用缓存
        useCache = valueOrDefault(useCache, isSelect);

        //下面是build模式
        statementBuilder.flushCacheRequired(flushCache);
        statementBuilder.useCache(useCache);
        statementBuilder.cache(cache);
    }

    private void setStatementParameterMap(String parameterMap, Class<?> parameterTypeClass, MappedStatement.Builder statementBuilder) {
        parameterMap = applyCurrentNamespace(parameterMap, true);

        if (parameterMap != null) {
            try {
                statementBuilder.parameterMap(configuration.getParameterMap(parameterMap));
            } catch (IllegalArgumentException e) {
                throw new IncompleteElementException("Could not find parameter map " + parameterMap, e);
            }
        } else if (parameterTypeClass != null) {
            List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
            ParameterMap.Builder inlineParameterMapBuilder = new ParameterMap.Builder(
                    configuration,
                    statementBuilder.id() + "-Inline",
                    parameterTypeClass,
                    parameterMappings);
            ParameterMap buildParameterMap = inlineParameterMapBuilder.build();
            statementBuilder.parameterMap(buildParameterMap);
        }
    }

    //2.result map
    private void setStatementResultMap(String resultMap, Class<?> resultType, ResultSetType resultSetType, MappedStatement.Builder statementBuilder) {
        resultMap = applyCurrentNamespace(resultMap, true);//org.apache.ibatis.submitted.force_flush_on_select.PersonMapper.personMap

        List<ResultMap> resultMaps = new ArrayList<ResultMap>();
        if (resultMap != null) {
            //2.1 resultMap是高级功能
            String[] resultMapNames = resultMap.split(",");
            for (String resultMapName : resultMapNames) {
                try {
                    String trimResultMapName = resultMapName.trim();
                    ResultMap configurationResultMap = configuration.getResultMap(trimResultMapName);
                    resultMaps.add(configurationResultMap);
                } catch (IllegalArgumentException e) {
                    throw new IncompleteElementException("Could not find result map " + resultMapName, e);
                }
            }
        } else if (resultType != null) {
            //2.2 resultType,一般用这个足矣了
            //<select id="selectUsers" resultType="User">
            //这种情况下,MyBatis 会在幕后自动创建一个 ResultMap,基于属性名来映射列到 JavaBean 的属性上。
            //如果列名没有精确匹配,你可以在列名上使用 select 字句的别名来匹配标签。
            //创建一个inline result map, 把resultType设上就OK了，
            //然后后面被DefaultResultSetHandler.createResultObject()使用
            //DefaultResultSetHandler.getRowValue()使用
            ResultMap.Builder inlineResultMapBuilder = new ResultMap.Builder(configuration, statementBuilder.id() + "-Inline",
                    resultType, new ArrayList<ResultMapping>(), null);
            resultMaps.add(inlineResultMapBuilder.build());
        }
        statementBuilder.resultMaps(resultMaps);

        statementBuilder.resultSetType(resultSetType);
    }

    private void setStatementTimeout(Integer timeout, MappedStatement.Builder statementBuilder) {
        if (timeout == null) {
            timeout = configuration.getDefaultStatementTimeout();
        }
        statementBuilder.timeout(timeout);
    }

    /**
     * 构建result map 中的每一列
     * <result column="config_key" property="configKey" jdbcType="VARCHAR"/>
     */
    public ResultMapping buildResultMapping(Class<?> resultMapType, String property, String column, Class<?> javaType,
            JdbcType jdbcType, String nestedSelect, String nestedResultMap, String notNullColumn, String columnPrefix,
            Class<? extends TypeHandler<?>> typeHandler, List<ResultFlag> flags, String resultSet, String foreignColumn, boolean lazy) {

        //根据 resultMap 的 java 类型， 该列的 属性 以及 该列的 java 类型，推断出最终的 列类型
        Class<?> propertyJavaTypeClass = resolveResultJavaType(resultMapType, property, javaType);
        TypeHandler<?> typeHandlerInstance = resolveTypeHandler(propertyJavaTypeClass, typeHandler);
        //解析复合的列名,一般用不到，返回的是空
        List<ResultMapping> composites = parseCompositeColumnName(column);
        if (composites.size() > 0) {
            column = null;
        }
        //构建result map
        ResultMapping.Builder builder = new ResultMapping.Builder(configuration, property, column, propertyJavaTypeClass);
        builder.jdbcType(jdbcType);
        builder.nestedQueryId(applyCurrentNamespace(nestedSelect, true));
        builder.nestedResultMapId(applyCurrentNamespace(nestedResultMap, true));
        builder.resultSet(resultSet);
        builder.typeHandler(typeHandlerInstance);
        builder.flags(flags == null ? new ArrayList<ResultFlag>() : flags);
        builder.composites(composites);
        builder.notNullColumns(parseMultipleColumnNames(notNullColumn));
        builder.columnPrefix(columnPrefix);
        builder.foreignColumn(foreignColumn);
        builder.lazy(lazy);
        return builder.build();
    }

    private Set<String> parseMultipleColumnNames(String columnName) {
        Set<String> columns = new HashSet<String>();
        if (columnName != null) {
            if (columnName.indexOf(',') > -1) {
                StringTokenizer parser = new StringTokenizer(columnName, "{}, ", false);
                while (parser.hasMoreTokens()) {
                    String column = parser.nextToken();
                    columns.add(column);
                }
            } else {
                columns.add(columnName);
            }
        }
        return columns;
    }

    /**
     * 解析复合列名，即列名由多个组成，可以先忽略
     *
     * 解析混合列
     *
     * @param columnName
     * @return
     */
    private List<ResultMapping> parseCompositeColumnName(String columnName) {
        List<ResultMapping> composites = new ArrayList<ResultMapping>();
        //如果columnName不为null 同时colunmnName中含有"=" 或者含有","号
        if (columnName != null && (columnName.indexOf('=') > -1 || columnName.indexOf(',') > -1)) {
            //分割字符串
            StringTokenizer parser = new StringTokenizer(columnName, "{}=, ", false);
            while (parser.hasMoreTokens()) {
                //获取属性
                String property = parser.nextToken();
                //获取列
                String column = parser.nextToken();
                //构建复合的ResultMapping
                ResultMapping.Builder complexBuilder = new ResultMapping.Builder(configuration, property, column, configuration.getTypeHandlerRegistry().getUnknownTypeHandler());
                composites.add(complexBuilder.build());
            }
        }
        return composites;
    }

    /**
     * 根据 resultMap 的 java 类型， 该列的 属性 以及 该列的 java 类型，推断出最终的 列类型
     *
     * <id column="config_id" property="configId" jdbcType="BIGINT"/>
     * <result column="config_value" property="configValue" jdbcType="VARCHAR"/>
     *
     * @param resultType <resultMap id="BaseResultMap" type="com.servyou.hermes.model.FrontConfig"> 该 resultMap 的 java类型
     * @param property configId 该列的属性名称
     * @param javaType BIGINT 用户指定的类型
     * @return 根据 resultMap 的 java 类型， 该列的 属性 以及 该列的 java 类型，推断出最终的 列类型
     */
    private Class<?> resolveResultJavaType(Class<?> resultType, String property, Class<?> javaType) {
        if (javaType == null && property != null) {
            try {
                MetaClass metaResultType = MetaClass.forClass(resultType);
                javaType = metaResultType.getSetterType(property);
            } catch (Exception e) {
                //ignore, following null check statement will deal with the situation
            }
        }
        if (javaType == null) {
            javaType = Object.class;
        }
        return javaType;
    }

    private Class<?> resolveParameterJavaType(Class<?> resultType, String property, Class<?> javaType, JdbcType jdbcType) {
        if (javaType == null) {
            if (JdbcType.CURSOR.equals(jdbcType)) {
                javaType = java.sql.ResultSet.class;
            } else if (Map.class.isAssignableFrom(resultType)) {
                javaType = Object.class;
            } else {
                MetaClass metaResultType = MetaClass.forClass(resultType);
                javaType = metaResultType.getGetterType(property);
            }
        }
        if (javaType == null) {
            javaType = Object.class;
        }
        return javaType;
    }

    /**
     * Backward compatibility signature
     */
    //向后兼容方法
    public ResultMapping buildResultMapping(
            Class<?> resultType,
            String property,
            String column,
            Class<?> javaType,
            JdbcType jdbcType,
            String nestedSelect,
            String nestedResultMap,
            String notNullColumn,
            String columnPrefix,
            Class<? extends TypeHandler<?>> typeHandler,
            List<ResultFlag> flags) {
        return buildResultMapping(
                resultType, property, column, javaType, jdbcType, nestedSelect,
                nestedResultMap, notNullColumn, columnPrefix, typeHandler, flags, null, null, configuration.isLazyLoadingEnabled());
    }

    /**
     * 取得语言驱动
     *
     * @param langClass 如果对应的方法指定了就不为空，否则返回默认的
     * @return 取得语言驱动
     */
    public LanguageDriver getLanguageDriver(Class<?> langClass) {
        LanguageDriverRegistry languageRegistry = configuration.getLanguageRegistry();
        if (langClass != null) {
            //注册语言驱动
            languageRegistry.register(langClass);
        } else {
            //如果为null，则取得默认驱动（mybatis3.2以前大家一直用的方法）
            //XMLLanguageDriver
            langClass = languageRegistry.getDefaultDriverClass();
        }
        //再去调configuration
        return languageRegistry.getDriver(langClass);
    }

    /**
     * Backward compatibility signature
     */
    //向后兼容方法
    public MappedStatement addMappedStatement(
            String id,
            SqlSource sqlSource,
            StatementType statementType,
            SqlCommandType sqlCommandType,
            Integer fetchSize,
            Integer timeout,
            String parameterMap,
            Class<?> parameterType,
            String resultMap,
            Class<?> resultType,
            ResultSetType resultSetType,
            boolean flushCache,
            boolean useCache,
            boolean resultOrdered,
            KeyGenerator keyGenerator,
            String keyProperty,
            String keyColumn,
            String databaseId,
            LanguageDriver lang) {
        return addMappedStatement(
                id, sqlSource, statementType, sqlCommandType, fetchSize, timeout,
                parameterMap, parameterType, resultMap, resultType, resultSetType,
                flushCache, useCache, resultOrdered, keyGenerator, keyProperty,
                keyColumn, databaseId, lang, null);
    }

}
