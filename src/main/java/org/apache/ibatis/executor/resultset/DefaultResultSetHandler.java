package org.apache.ibatis.executor.resultset;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.loader.ResultLoader;
import org.apache.ibatis.executor.loader.ResultLoaderMap;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.result.DefaultResultContext;
import org.apache.ibatis.executor.result.DefaultResultHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.lang.reflect.Constructor;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */

/**
 * 默认结果处理器
 */
public class DefaultResultSetHandler implements ResultSetHandler {

    private static final Object NO_VALUE = new Object();

    private final Executor executor;

    private final Configuration configuration;

    private final MappedStatement mappedStatement;

    private final RowBounds rowBounds;

    private final ParameterHandler parameterHandler;

    private final ResultHandler resultHandler;

    private final BoundSql boundSql;

    private final TypeHandlerRegistry typeHandlerRegistry;

    private final ObjectFactory objectFactory;

    // nested resultmaps
    private final Map<CacheKey, Object> nestedResultObjects = new HashMap<CacheKey, Object>();

    private final Map<CacheKey, Object> ancestorObjects = new HashMap<CacheKey, Object>();

    private final Map<String, String> ancestorColumnPrefix = new HashMap<String, String>();

    // multiple resultsets
    private final Map<String, ResultMapping> nextResultMaps = new HashMap<String, ResultMapping>();

    private final Map<CacheKey, List<PendingRelation>> pendingRelations = new HashMap<CacheKey, List<PendingRelation>>();

    public DefaultResultSetHandler(Executor executor, MappedStatement mappedStatement, ParameterHandler parameterHandler, ResultHandler resultHandler,
            BoundSql boundSql, RowBounds rowBounds) {

        this.executor = executor;
        this.configuration = mappedStatement.getConfiguration();
        this.mappedStatement = mappedStatement;
        this.rowBounds = rowBounds;
        this.parameterHandler = parameterHandler;
        this.boundSql = boundSql;
        this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        this.objectFactory = configuration.getObjectFactory();
        this.resultHandler = resultHandler;
    }

    @Override
    public void handleOutputParameters(CallableStatement cs) throws SQLException {
        final Object parameterObject = parameterHandler.getParameterObject();
        final MetaObject metaParam = configuration.newMetaObject(parameterObject);
        final List<ParameterMapping> parameterMappingList = boundSql.getParameterMappings();
        //循环处理每个参数
        for (int i = 0; i < parameterMappingList.size(); i++) {
            final ParameterMapping parameterMapping = parameterMappingList.get(i);
            //只处理OUT|INOUT
            if (parameterMapping.getMode() == ParameterMode.OUT || parameterMapping.getMode() == ParameterMode.INOUT) {
                if (ResultSet.class.equals(parameterMapping.getJavaType())) {
                    //如果是ResultSet型(游标)
                    //#{result, jdbcType=CURSOR, mode=OUT, javaType=ResultSet, resultMap=userResultMap}
                    //先用CallableStatement.getObject取得这个游标，作为参数传进去
                    handleRefCursorOutputParameter((ResultSet) cs.getObject(i + 1), parameterMapping, metaParam);
                } else {
                    //否则是普通型，核心就是CallableStatement.getXXX取得值
                    final TypeHandler<?> typeHandler = parameterMapping.getTypeHandler();
                    metaParam.setValue(parameterMapping.getProperty(), typeHandler.getResult(cs, i + 1));
                }
            }
        }
    }

    //
    // HANDLE OUTPUT PARAMETER
    //

    //处理游标(OUT参数)
    private void handleRefCursorOutputParameter(ResultSet rs, ParameterMapping parameterMapping, MetaObject metaParam) throws SQLException {
        try {
            final String resultMapId = parameterMapping.getResultMapId();
            final ResultMap resultMap = configuration.getResultMap(resultMapId);
            final DefaultResultHandler resultHandler = new DefaultResultHandler(objectFactory);
            final ResultSetWrapper rsw = new ResultSetWrapper(rs, configuration);
            //里面就和一般ResultSet处理没两样了
            handleRowValues(rsw, resultMap, resultHandler, new RowBounds(), null);
            metaParam.setValue(parameterMapping.getProperty(), resultHandler.getResultList());
        } finally {
            // issue #228 (close resultsets)
            closeResultSet(rs);
        }
    }

    /**
     * 处理sql执行结果
     *
     * @param statement 已经执行过数据库查询的 statement，通过 ResultSet resultSet = statement.getResultSet();可以拿到结果
     * @return sql执行结果
     * @throws SQLException 异常
     */
    @Override
    public List<Object> handleResultSets(Statement statement) throws SQLException {
        ErrorContext.instance().activity("handling results").object(mappedStatement.getId());

        final List<Object> multipleResults = new ArrayList<Object>();

        ResultSetWrapper resultSetWrapper = getFirstResultSet(statement);
        //结果映射 resultMap
        List<ResultMap> resultMapList = mappedStatement.getResultMaps();
        //一般resultMaps里只有一个元素
        int resultMapCount = resultMapList.size();
        validateResultMapsCount(resultSetWrapper, resultMapCount);

        int resultSetCount = 0;
        while (resultSetWrapper != null && resultMapCount > resultSetCount) {
            ResultMap resultMap = resultMapList.get(resultSetCount);
            handleResultSet(resultSetWrapper, resultMap, multipleResults, null);
            resultSetWrapper = getNextResultSet(statement);
            cleanUpAfterHandlingResultSet();
            resultSetCount++;
        }

        String[] resultSets = mappedStatement.getResultSets();
        if (resultSets != null) {
            while (resultSetWrapper != null && resultSetCount < resultSets.length) {
                ResultMapping parentMapping = nextResultMaps.get(resultSets[resultSetCount]);
                if (parentMapping != null) {
                    String nestedResultMapId = parentMapping.getNestedResultMapId();
                    ResultMap resultMap = configuration.getResultMap(nestedResultMapId);
                    handleResultSet(resultSetWrapper, resultMap, null, parentMapping);
                }
                resultSetWrapper = getNextResultSet(statement);
                cleanUpAfterHandlingResultSet();
                resultSetCount++;
            }
        }

        return collapseSingleResultList(multipleResults);
    }

    private ResultSetWrapper getFirstResultSet(Statement statement) throws SQLException {
        ResultSet resultSet = statement.getResultSet();
        //HSQLDB2.1特殊情况处理,mysql是不存在这样的情况
        while (resultSet == null) {
            // move forward to get the first resultset in case the driver
            // doesn't return the resultset as the first result (HSQLDB 2.1)
            if (statement.getMoreResults()) {
                resultSet = statement.getResultSet();
            } else {
                if (statement.getUpdateCount() == -1) {
                    // no more results. Must be no resultset
                    break;
                }
            }
        }
        return resultSet != null ? new ResultSetWrapper(resultSet, configuration) : null;
    }

    private ResultSetWrapper getNextResultSet(Statement stmt) throws SQLException {
        // Making this method tolerant of bad JDBC drivers
        try {
            if (stmt.getConnection().getMetaData().supportsMultipleResultSets()) {
                // Crazy Standard JDBC way of determining if there are more results
                if (!((!stmt.getMoreResults()) && (stmt.getUpdateCount() == -1))) {
                    ResultSet rs = stmt.getResultSet();
                    return rs != null ? new ResultSetWrapper(rs, configuration) : null;
                }
            }
        } catch (Exception e) {
            // Intentionally ignored.
        }
        return null;
    }

    private void closeResultSet(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            // ignore
        }
    }

    private void cleanUpAfterHandlingResultSet() {
        nestedResultObjects.clear();
        ancestorColumnPrefix.clear();
    }

    private void validateResultMapsCount(ResultSetWrapper resultSetWrapper, int resultMapCount) {
        if (resultSetWrapper != null && resultMapCount < 1) {
            throw new ExecutorException("A query was run and no Result Maps were found for the Mapped Statement '" + mappedStatement.getId()
                    + "'.  It's likely that neither a Result Type nor a Result Map was specified.");
        }
    }

    //处理结果集
    private void handleResultSet(ResultSetWrapper resultSetWrapper, ResultMap resultMap, List<Object> multipleResults, ResultMapping parentMapping) throws SQLException {
        try {
            if (parentMapping != null) {
                handleRowValues(resultSetWrapper, resultMap, null, RowBounds.DEFAULT, parentMapping);
            } else {
                if (resultHandler == null) {
                    //如果没有resultHandler
                    //新建DefaultResultHandler
                    DefaultResultHandler defaultResultHandler = new DefaultResultHandler(objectFactory);
                    //调用自己的handleRowValues
                    handleRowValues(resultSetWrapper, resultMap, defaultResultHandler, rowBounds, null);
                    //得到记录的list
                    List<Object> resultList = defaultResultHandler.getResultList();
                    multipleResults.add(resultList);
                } else {
                    //如果有resultHandler
                    handleRowValues(resultSetWrapper, resultMap, resultHandler, rowBounds, null);
                }
            }
        } finally {
            //最后别忘了关闭结果集，这个居然出bug了
            // issue #228 (close resultsets)
            closeResultSet(resultSetWrapper.getResultSet());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> collapseSingleResultList(List<Object> multipleResults) {
        boolean single = multipleResults.size() == 1;
        return single ? (List<Object>) multipleResults.get(0) : multipleResults;
    }

    private void handleRowValues(ResultSetWrapper resultSetWrapper, ResultMap resultMap, ResultHandler resultHandler,
            RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {

        boolean hasNestedResultMaps = resultMap.hasNestedResultMaps();
        //有嵌套映射结果
        if (hasNestedResultMaps) {
            ensureNoRowBounds();
            checkResultHandler();
            handleRowValuesForNestedResultMap(resultSetWrapper, resultMap, resultHandler, rowBounds, parentMapping);
        } else {
            handleRowValuesForSimpleResultMap(resultSetWrapper, resultMap, resultHandler, rowBounds, parentMapping);
        }
    }

    //
    // HANDLE ROWS FOR SIMPLE RESULTMAP
    //

    private void ensureNoRowBounds() {
        if (configuration.isSafeRowBoundsEnabled() && rowBounds != null && (rowBounds.getLimit() < RowBounds.NO_ROW_LIMIT || rowBounds.getOffset() > RowBounds.NO_ROW_OFFSET)) {
            throw new ExecutorException("Mapped Statements with nested result mappings cannot be safely constrained by RowBounds. "
                    + "Use safeRowBoundsEnabled=false setting to bypass this check.");
        }
    }

    protected void checkResultHandler() {
        if (resultHandler != null && configuration.isSafeResultHandlerEnabled() && !mappedStatement.isResultOrdered()) {
            throw new ExecutorException("Mapped Statements with nested result mappings cannot be safely used with a custom ResultHandler. "
                    + "Use safeResultHandlerEnabled=false setting to bypass this check "
                    + "or ensure your statement returns ordered data and set resultOrdered=true on it.");
        }
    }

    private void handleRowValuesForSimpleResultMap(ResultSetWrapper resultSetWrapper, ResultMap resultMap, ResultHandler resultHandler,
            RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {

        DefaultResultContext resultContext = new DefaultResultContext();
        //内存分页
        ResultSet resultSet = resultSetWrapper.getResultSet();
        skipRows(resultSet, rowBounds);
        while (shouldProcessMoreRows(resultContext, rowBounds) && resultSet.next()) {
            //获取鉴别器对应的映射，如果没有鉴别器，则返回当前映射
            ResultMap discriminatedResultMap = resolveDiscriminatedResultMap(resultSet, resultMap, null);
            Object rowValue = getRowValue(resultSetWrapper, discriminatedResultMap);
            storeObject(resultHandler, resultContext, rowValue, parentMapping, resultSet);
        }
    }

    private void storeObject(ResultHandler resultHandler, DefaultResultContext resultContext, Object rowValue, ResultMapping parentMapping, ResultSet resultSet) throws SQLException {
        if (parentMapping != null) {
            linkToParents(resultSet, parentMapping, rowValue);
        } else {
            callResultHandler(resultHandler, resultContext, rowValue);
        }
    }

    private void callResultHandler(ResultHandler resultHandler, DefaultResultContext resultContext, Object rowValue) {
        resultContext.nextResultObject(rowValue);
        resultHandler.handleResult(resultContext);
    }

    private boolean shouldProcessMoreRows(ResultContext context, RowBounds rowBounds) {
        return !context.isStopped() && context.getResultCount() < rowBounds.getLimit();
    }

    /**
     * 通过RowBounds类可以实现Mybatis逻辑分页，原理是首先将所有结果查询出来，然后通过计算offset和limit，
     * 只返回部分结果，操作在内存中进行，所以也叫内存分页。弊端很明显，当数据量比较大的时候，肯定是不行的，
     * 所以一般不会去使用RowBounds进行分页查询，这里仅展示一下RowBounds用法。Mybatis Generator原生支持RowBounds查询，
     * 生成的Mapper接口中存在一个方法selectByExampleWithRowbounds就是通过RowBounds进行分页查询
     *
     * @param resultSet 结果
     * @param rowBounds 分页参数
     * @throws SQLException 异常
     */
    private void skipRows(ResultSet resultSet, RowBounds rowBounds) throws SQLException {
        if (resultSet.getType() != ResultSet.TYPE_FORWARD_ONLY) {
            if (rowBounds.getOffset() != RowBounds.NO_ROW_OFFSET) {
                resultSet.absolute(rowBounds.getOffset());
            }
        } else {
            for (int i = 0; i < rowBounds.getOffset(); i++) {
                //游标移动 offset 次
                resultSet.next();
            }
        }
    }

    /**
     * 核心，取得一行的值
     *
     * @param resultSetWrapper 结果集包装对象
     * @param resultMap 映射
     * @return 一行值
     * @throws SQLException 异常
     */
    private Object getRowValue(ResultSetWrapper resultSetWrapper, ResultMap resultMap) throws SQLException {
        //实例化ResultLoaderMap(延迟加载器)
        final ResultLoaderMap lazyLoader = new ResultLoaderMap();
        //调用自己的createResultObject,内部就是new一个对象(如果是简单类型，new完也把值赋进去)
        //查询到的结果
        Object resultObject = createResultObject(resultSetWrapper, resultMap, lazyLoader, null);
        if (resultObject != null && !typeHandlerRegistry.hasTypeHandler(resultMap.getType())) {
            //一般不是简单类型不会有typeHandler,这个if会进来
            final MetaObject metaObject = configuration.newMetaObject(resultObject);
            boolean foundValues = !resultMap.getConstructorResultMappings().isEmpty();
            if (shouldApplyAutomaticMappings(resultMap, false)) {
                //自动映射咯
                //这里把每个列的值都赋到相应的字段里去了
                foundValues = applyAutomaticMappings(resultSetWrapper, resultMap, metaObject, null) || foundValues;
            }
            foundValues = applyPropertyMappings(resultSetWrapper, resultMap, metaObject, lazyLoader, null) || foundValues;
            foundValues = lazyLoader.size() > 0 || foundValues;
            resultObject = foundValues ? resultObject : null;
            return resultObject;
        }
        return resultObject;
    }

    //
    // GET VALUE FROM ROW FOR SIMPLE RESULT MAP
    //

    private boolean shouldApplyAutomaticMappings(ResultMap resultMap, boolean isNested) {
        if (resultMap.getAutoMapping() != null) {
            return resultMap.getAutoMapping();
        } else {
            if (isNested) {
                return AutoMappingBehavior.FULL == configuration.getAutoMappingBehavior();
            } else {
                return AutoMappingBehavior.NONE != configuration.getAutoMappingBehavior();
            }
        }
    }

    private boolean applyPropertyMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject,
            ResultLoaderMap lazyLoader, String columnPrefix) throws SQLException {

        final List<String> mappedColumnNames = rsw.getMappedColumnNames(resultMap, columnPrefix);
        boolean foundValues = false;
        final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
        for (ResultMapping propertyMapping : propertyMappings) {
            final String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
            if (propertyMapping.isCompositeResult()
                    || (column != null && mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH)))
                    || propertyMapping.getResultSet() != null) {

                Object value = getPropertyMappingValue(rsw.getResultSet(), metaObject, propertyMapping, lazyLoader, columnPrefix);
                // issue #541 make property optional
                final String property = propertyMapping.getProperty();
                // issue #377, call setter on nulls
                if (value != NO_VALUE && property != null && (value != null || configuration.isCallSettersOnNulls())) {
                    if (value != null || !metaObject.getSetterType(property).isPrimitive()) {
                        metaObject.setValue(property, value);
                    }
                    foundValues = true;
                }
            }
        }
        return foundValues;
    }

    //
    // PROPERTY MAPPINGS
    //

    private Object getPropertyMappingValue(ResultSet resultSet, MetaObject metaResultObject, ResultMapping propertyMapping,
            ResultLoaderMap lazyLoader, String columnPrefix) throws SQLException {

        if (propertyMapping.getNestedQueryId() != null) {
            return getNestedQueryMappingValue(resultSet, metaResultObject, propertyMapping, lazyLoader, columnPrefix);
        } else if (propertyMapping.getResultSet() != null) {
            addPendingChildRelation(resultSet, metaResultObject, propertyMapping);
            return NO_VALUE;
        } else if (propertyMapping.getNestedResultMapId() != null) {
            // the user added a column attribute to a nested result map, ignore it
            return NO_VALUE;
        } else {
            final TypeHandler<?> typeHandler = propertyMapping.getTypeHandler();
            final String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
            return typeHandler.getResult(resultSet, column);
        }
    }

    //自动映射咯
    private boolean applyAutomaticMappings(ResultSetWrapper resultSetWrapper, ResultMap resultMap, MetaObject metaObject, String columnPrefix) throws SQLException {
        final List<String> unmappedColumnNames = resultSetWrapper.getUnmappedColumnNames(resultMap, columnPrefix);
        boolean foundValues = false;
        for (String columnName : unmappedColumnNames) {
            String propertyName = columnName;
            if (columnPrefix != null && !columnPrefix.isEmpty()) {
                // When columnPrefix is specified,
                // ignore columns without the prefix.
                if (columnName.toUpperCase(Locale.ENGLISH).startsWith(columnPrefix)) {
                    propertyName = columnName.substring(columnPrefix.length());
                } else {
                    continue;
                }
            }
            final String property = metaObject.findProperty(propertyName, configuration.isMapUnderscoreToCamelCase());
            if (property != null && metaObject.hasSetter(property)) {
                final Class<?> propertyType = metaObject.getSetterType(property);
                if (typeHandlerRegistry.hasTypeHandler(propertyType)) {
                    final TypeHandler<?> typeHandler = resultSetWrapper.getTypeHandler(propertyType, columnName);
                    //巧妙的用TypeHandler取得结果
                    final Object value = typeHandler.getResult(resultSetWrapper.getResultSet(), columnName);
                    // issue #377, call setter on nulls
                    if (value != null || configuration.isCallSettersOnNulls()) {
                        if (value != null || !propertyType.isPrimitive()) {
                            //然后巧妙的用反射来设置到对象
                            metaObject.setValue(property, value);
                        }
                        foundValues = true;
                    }
                }
            }
        }
        return foundValues;
    }

    private void linkToParents(ResultSet rs, ResultMapping parentMapping, Object rowValue) throws SQLException {
        CacheKey parentKey = createKeyForMultipleResults(rs, parentMapping, parentMapping.getColumn(), parentMapping.getForeignColumn());
        List<PendingRelation> parents = pendingRelations.get(parentKey);
        for (PendingRelation parent : parents) {
            if (parent != null) {
                final Object collectionProperty = instantiateCollectionPropertyIfAppropriate(parent.propertyMapping, parent.metaObject);
                if (rowValue != null) {
                    if (collectionProperty != null) {
                        final MetaObject targetMetaObject = configuration.newMetaObject(collectionProperty);
                        targetMetaObject.add(rowValue);
                    } else {
                        parent.metaObject.setValue(parent.propertyMapping.getProperty(), rowValue);
                    }
                }
            }
        }
    }

    // MULTIPLE RESULT SETS

    private Object instantiateCollectionPropertyIfAppropriate(ResultMapping resultMapping, MetaObject metaObject) {
        final String propertyName = resultMapping.getProperty();
        Object propertyValue = metaObject.getValue(propertyName);
        if (propertyValue == null) {
            Class<?> type = resultMapping.getJavaType();
            if (type == null) {
                type = metaObject.getSetterType(propertyName);
            }
            try {
                if (objectFactory.isCollection(type)) {
                    propertyValue = objectFactory.create(type);
                    metaObject.setValue(propertyName, propertyValue);
                    return propertyValue;
                }
            } catch (Exception e) {
                throw new ExecutorException("Error instantiating collection property for result '" + resultMapping.getProperty() + "'.  Cause: " + e, e);
            }
        } else if (objectFactory.isCollection(propertyValue.getClass())) {
            return propertyValue;
        }
        return null;
    }

    private void addPendingChildRelation(ResultSet rs, MetaObject metaResultObject, ResultMapping parentMapping) throws SQLException {
        CacheKey cacheKey = createKeyForMultipleResults(rs, parentMapping, parentMapping.getColumn(), parentMapping.getColumn());
        PendingRelation deferLoad = new PendingRelation();
        deferLoad.metaObject = metaResultObject;
        deferLoad.propertyMapping = parentMapping;
        List<PendingRelation> relations = pendingRelations.get(cacheKey);
        // issue #255
        if (relations == null) {
            relations = new ArrayList<DefaultResultSetHandler.PendingRelation>();
            pendingRelations.put(cacheKey, relations);
        }
        relations.add(deferLoad);
        ResultMapping previous = nextResultMaps.get(parentMapping.getResultSet());
        if (previous == null) {
            nextResultMaps.put(parentMapping.getResultSet(), parentMapping);
        } else {
            if (!previous.equals(parentMapping)) {
                throw new ExecutorException("Two different properties are mapped to the same resultSet");
            }
        }
    }

    private CacheKey createKeyForMultipleResults(ResultSet rs, ResultMapping resultMapping, String names, String columns) throws SQLException {
        CacheKey cacheKey = new CacheKey();
        cacheKey.update(resultMapping);
        if (columns != null && names != null) {
            String[] columnsArray = columns.split(",");
            String[] namesArray = names.split(",");
            for (int i = 0; i < columnsArray.length; i++) {
                Object value = rs.getString(columnsArray[i]);
                if (value != null) {
                    cacheKey.update(namesArray[i]);
                    cacheKey.update(value);
                }
            }
        }
        return cacheKey;
    }

    private Object createResultObject(ResultSetWrapper resultSetWrapper, ResultMap resultMap, ResultLoaderMap lazyLoader, String columnPrefix) throws SQLException {
        final List<Class<?>> constructorArgTypes = new ArrayList<Class<?>>();
        final List<Object> constructorArgs = new ArrayList<Object>();
        //查询结果
        final Object resultObject = createResultObject(resultSetWrapper, resultMap, constructorArgTypes, constructorArgs, columnPrefix);
        //映射类型
        Class<?> resultMapType = resultMap.getType();
        boolean hasTypeHandler = typeHandlerRegistry.hasTypeHandler(resultMapType);

        //如果查询结果不为null，且该查询结果类型没有对应的类型处理器，则进去，否则直接返回结果
        if (resultObject != null && !hasTypeHandler) {
            //所有映射字段
            final List<ResultMapping> propertyMappingList = resultMap.getPropertyResultMappings();
            for (ResultMapping propertyMapping : propertyMappingList) {
                // issue gcode #109 && issue #149
                if (propertyMapping.getNestedQueryId() != null && propertyMapping.isLazy()) {
                    //TODO 使用代理(cglib/javaassist)
                    return configuration.getProxyFactory().createProxy(resultObject, lazyLoader, configuration, objectFactory, constructorArgTypes, constructorArgs);
                }
            }
        }
        return resultObject;
    }

    //
    // INSTANTIATION & CONSTRUCTOR MAPPING
    //

    /**
     * 创建结果对象
     *
     * @param resultSetWrapper 包装了结果的对象
     * @param resultMap 映射
     * @param constructorArgTypes 构造器参数类型
     * @param constructorArgs 构造器参数
     * @param columnPrefix 前缀
     * @return 创建结果对象
     * @throws SQLException 异常
     */
    private Object createResultObject(ResultSetWrapper resultSetWrapper, ResultMap resultMap, List<Class<?>> constructorArgTypes,
            List<Object> constructorArgs, String columnPrefix) throws SQLException {

        //得到result type
        final Class<?> resultType = resultMap.getType();
        final MetaClass metaType = MetaClass.forClass(resultType);
        final List<ResultMapping> constructorMappings = resultMap.getConstructorResultMappings();
        boolean hasTypeHandler = typeHandlerRegistry.hasTypeHandler(resultType);
        //如果这个映射类型有自己的处理器，则说明他只是一个值，不会是一个复杂 对象
        if (hasTypeHandler) {
            //基本型
            return createPrimitiveResultObject(resultSetWrapper, resultMap, columnPrefix);
        } else if (!constructorMappings.isEmpty()) {//通过构造器创建对象的映射
            //有参数的构造函数
            return createParameterizedResultObject(resultSetWrapper, resultType, constructorMappings, constructorArgTypes, constructorArgs, columnPrefix);
        } else if (resultType.isInterface() || metaType.hasDefaultConstructor()) {
            //普通bean类型
            return objectFactory.create(resultType);
        } else if (shouldApplyAutomaticMappings(resultMap, false)) {
            //自动映射
            return createByConstructorSignature(resultSetWrapper, resultType, constructorArgTypes, constructorArgs, columnPrefix);
        }
        //无法确定实例化 映射 对象的方式
        throw new ExecutorException("Do not know how to create an instance of " + resultType);
    }

    /**
     * 通过构造器创建对象的映射
     *
     * <resultMap id="blogUsingConstructor" type="Blog">
     * <constructor>
     * <idArg column="id" javaType="_int"/>
     * <arg column="title" javaType="java.lang.String"/>
     * <arg column="author_id" javaType="org.apache.ibatis.domain.blog.Author"
     * select="org.apache.ibatis.binding.BoundAuthorMapper.selectAuthor"/>
     * <arg column="id" javaType="java.util.List" select="selectPostsForBlog"/>
     * </constructor>
     * </resultMap>
     *
     * @param resultSetWrapper 包装器
     * @param resultType 映射结果类型
     * @param constructorMappingList 构造器映射列表
     * @param constructorArgTypes 构造器参数类型
     * @param constructorArgList 构造器参数
     * @param columnPrefix 前缀
     * @return 结果
     * @throws SQLException 异常
     */
    private Object createParameterizedResultObject(ResultSetWrapper resultSetWrapper, Class<?> resultType, List<ResultMapping> constructorMappingList,
            List<Class<?>> constructorArgTypes, List<Object> constructorArgList, String columnPrefix) throws SQLException {

        boolean foundValues = false;
        for (ResultMapping constructorMapping : constructorMappingList) {
            final Class<?> parameterType = constructorMapping.getJavaType();
            final String column = constructorMapping.getColumn();
            //该构造器参数的值
            final Object value;
            if (constructorMapping.getNestedQueryId() != null) {
                //嵌套查询
                value = getNestedQueryConstructorValue(resultSetWrapper.getResultSet(), constructorMapping, columnPrefix);
            } else if (constructorMapping.getNestedResultMapId() != null) {//构造器还有嵌套的映射？
                String nestedResultMapId = constructorMapping.getNestedResultMapId();
                final ResultMap resultMap = configuration.getResultMap(nestedResultMapId);
                value = getRowValue(resultSetWrapper, resultMap);
            } else {
                final TypeHandler<?> typeHandler = constructorMapping.getTypeHandler();
                value = typeHandler.getResult(resultSetWrapper.getResultSet(), prependPrefix(column, columnPrefix));
            }
            constructorArgTypes.add(parameterType);
            constructorArgList.add(value);
            foundValues = value != null || foundValues;
        }
        return foundValues ? objectFactory.create(resultType, constructorArgTypes, constructorArgList) : null;
    }

    /**
     * 匹配构造器，使用匹配的构造器实例化结果
     *
     * @param resultSetWrapper 包装结果对象
     * @param resultType 结果理想
     * @param constructorArgTypes 构造器类型集合
     * @param constructorArgs 构造器参数集合
     * @param columnPrefix 前缀
     * @return 结果对象
     * @throws SQLException 异常
     */
    private Object createByConstructorSignature(ResultSetWrapper resultSetWrapper, Class<?> resultType, List<Class<?>> constructorArgTypes,
            List<Object> constructorArgs, String columnPrefix) throws SQLException {

        //从返回类型的所有构造器中匹配
        Constructor<?>[] declaredConstructorList = resultType.getDeclaredConstructors();
        for (Constructor<?> constructor : declaredConstructorList) {

            Class<?>[] parameterTypeList = constructor.getParameterTypes();
            List<String> typeNames = typeNames(parameterTypeList);
            //如果参数类型以及顺序吻合，则使用该构造器实例化查询结果
            if (typeNames.equals(resultSetWrapper.getClassNames())) {
                boolean foundValues = false;
                for (int i = 0; i < parameterTypeList.length; i++) {
                    Class<?> parameterType = parameterTypeList[i];
                    String columnName = resultSetWrapper.getColumnNames().get(i);
                    TypeHandler<?> typeHandler = resultSetWrapper.getTypeHandler(parameterType, columnName);
                    Object value = typeHandler.getResult(resultSetWrapper.getResultSet(), prependPrefix(columnName, columnPrefix));
                    constructorArgTypes.add(parameterType);
                    constructorArgs.add(value);
                    foundValues = value != null || foundValues;
                }
                //上面是构造函数创建对象，下面是对象工厂来创建
                return foundValues ? objectFactory.create(resultType, constructorArgTypes, constructorArgs) : null;
            }
        }
        throw new ExecutorException("No constructor found in " + resultType.getName() + " matching " + resultSetWrapper.getClassNames());
    }

    /**
     * 返回class名称集合
     *
     * @param parameterTypes 参数类型集合
     * @return 返回class名称集合
     */
    private List<String> typeNames(Class<?>[] parameterTypes) {
        List<String> names = new ArrayList<String>();
        for (Class<?> type : parameterTypes) {
            names.add(type.getName());
        }
        return names;
    }

    /**
     * 简单类型走这里
     *
     * @param resultSetWrapper 包装了结果的对象
     * @param resultMap 映射规则
     * @param columnPrefix 前缀
     * @return 获取某列的值
     * @throws SQLException 异常
     */
    private Object createPrimitiveResultObject(ResultSetWrapper resultSetWrapper, ResultMap resultMap, String columnPrefix) throws SQLException {
        final Class<?> resultType = resultMap.getType();
        final String columnName;
        if (!resultMap.getResultMappings().isEmpty()) {
            //<result property="password" column="password" /> 列表
            final List<ResultMapping> resultMappingList = resultMap.getResultMappings();
            final ResultMapping mapping = resultMappingList.get(0);
            //全列名称
            columnName = prependPrefix(mapping.getColumn(), columnPrefix);
        } else {
            //因为只有1列，所以取得这一列的名字
            columnName = resultSetWrapper.getColumnNames().get(0);
        }
        //获取该column的类型处理器
        final TypeHandler<?> typeHandler = resultSetWrapper.getTypeHandler(resultType, columnName);
        //获取该列的java类型的值
        ResultSet resultSet = resultSetWrapper.getResultSet();
        return typeHandler.getResult(resultSet, columnName);
    }

    /**
     * 嵌套查询
     *
     * @param resultSet 结果
     * @param constructorMapping 该嵌套查询对应的映射规则
     * @param columnPrefix 前缀
     * @return 嵌套查询的结果
     * @throws SQLException 异常
     */
    private Object getNestedQueryConstructorValue(ResultSet resultSet, ResultMapping constructorMapping, String columnPrefix) throws SQLException {
        final String nestedQueryId = constructorMapping.getNestedQueryId();
        final MappedStatement nestedQuery = configuration.getMappedStatement(nestedQueryId);
        //查询结果类型
        final Class<?> nestedQueryParameterType = nestedQuery.getParameterMap().getType();
        //子查询的参数，这里就必须要查出来，得到具体的值
        final Object nestedQueryParameterObject = prepareParameterForNestedQuery(resultSet, constructorMapping, nestedQueryParameterType, columnPrefix);
        Object value = null;
        if (nestedQueryParameterObject != null) {
            //嵌套查询的sql
            final BoundSql nestedBoundSql = nestedQuery.getBoundSql(nestedQueryParameterObject);
            //获取缓存key
            final CacheKey cacheKey = executor.createCacheKey(nestedQuery, nestedQueryParameterObject, RowBounds.DEFAULT, nestedBoundSql);
            //该构造器参数的java类型
            final Class<?> targetType = constructorMapping.getJavaType();
            final ResultLoader resultLoader = new ResultLoader(configuration, executor, nestedQuery, nestedQueryParameterObject, targetType, cacheKey, nestedBoundSql);
            value = resultLoader.loadResult();
        }
        return value;
    }

    //
    // NESTED QUERY
    //

    //得到嵌套查询值
    private Object getNestedQueryMappingValue(ResultSet rs, MetaObject metaResultObject, ResultMapping propertyMapping, ResultLoaderMap lazyLoader, String columnPrefix)
            throws SQLException {
        final String nestedQueryId = propertyMapping.getNestedQueryId();
        final String property = propertyMapping.getProperty();
        final MappedStatement nestedQuery = configuration.getMappedStatement(nestedQueryId);
        final Class<?> nestedQueryParameterType = nestedQuery.getParameterMap().getType();
        final Object nestedQueryParameterObject = prepareParameterForNestedQuery(rs, propertyMapping, nestedQueryParameterType, columnPrefix);
        Object value = NO_VALUE;
        if (nestedQueryParameterObject != null) {
            final BoundSql nestedBoundSql = nestedQuery.getBoundSql(nestedQueryParameterObject);
            final CacheKey key = executor.createCacheKey(nestedQuery, nestedQueryParameterObject, RowBounds.DEFAULT, nestedBoundSql);
            final Class<?> targetType = propertyMapping.getJavaType();
            if (executor.isCached(nestedQuery, key)) {
                //如果已经有一级缓存了，则延迟加载(实际上deferLoad方法中可以看到则是立即加载)
                executor.deferLoad(nestedQuery, metaResultObject, property, key, targetType);
            } else {
                //否则lazyLoader.addLoader 需要延迟加载则addLoader
                //或者ResultLoader.loadResult 不需要延迟加载则立即加载
                final ResultLoader resultLoader = new ResultLoader(configuration, executor, nestedQuery, nestedQueryParameterObject, targetType, key, nestedBoundSql);
                if (propertyMapping.isLazy()) {
                    lazyLoader.addLoader(property, metaResultObject, resultLoader);
                } else {
                    value = resultLoader.loadResult();
                }
            }
        }
        return value;
    }

    /**
     * 为嵌套查询的参数做准备
     *
     * @param resultSet 结果
     * @param resultMapping 映射规则
     * @param parameterType 参数类型
     * @param columnPrefix 前缀
     * @return 参数
     * @throws SQLException 异常
     */
    private Object prepareParameterForNestedQuery(ResultSet resultSet, ResultMapping resultMapping, Class<?> parameterType, String columnPrefix) throws SQLException {
        boolean isCompositeResult = resultMapping.isCompositeResult();
        if (isCompositeResult) {
            //复合结果，其实就是一个有属性对象
            //准备复杂的参数，其实就是把该符合结果对象组装出并返回
            return prepareCompositeKeyParameter(resultSet, resultMapping, parameterType, columnPrefix);
        } else {
            //单个类型结果
            //准备简单的参数，其实就是从结果集 resultSet 中拿到 resultSet 对应的值
            return prepareSimpleKeyParameter(resultSet, resultMapping, parameterType, columnPrefix);
        }
    }

    /**
     * 准备简单的参数，其实就是从结果集 resultSet 中拿到 resultSet 对应的值
     *
     * @param resultSet 结果
     * @param resultMapping 映射规则
     * @param parameterType 参数类型
     * @param columnPrefix 前缀
     * @return 参数
     * @throws SQLException 异常
     */
    private Object prepareSimpleKeyParameter(ResultSet resultSet, ResultMapping resultMapping, Class<?> parameterType, String columnPrefix) throws SQLException {
        final TypeHandler<?> typeHandler;
        if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
            typeHandler = typeHandlerRegistry.getTypeHandler(parameterType);
        } else {
            typeHandler = typeHandlerRegistry.getUnknownTypeHandler();
        }
        return typeHandler.getResult(resultSet, prependPrefix(resultMapping.getColumn(), columnPrefix));
    }

    /**
     * 准备复杂的参数，其实就是把该符合结果对象组装出并返回
     *
     * @param resultSet 结果
     * @param resultMapping 映射规则
     * @param parameterType 参数类型
     * @param columnPrefix 前缀
     * @return 参数
     * @throws SQLException 异常
     */
    private Object prepareCompositeKeyParameter(ResultSet resultSet, ResultMapping resultMapping, Class<?> parameterType, String columnPrefix) throws SQLException {
        //实例化出一个对象
        final Object parameterObject = instantiateParameterObject(parameterType);
        final MetaObject metaObject = configuration.newMetaObject(parameterObject);
        boolean foundValues = false;
        //符合映射结果列表
        List<ResultMapping> compositeResultMappingList = resultMapping.getComposites();
        for (ResultMapping innerResultMapping : compositeResultMappingList) {
            //该属性的java类型
            final Class<?> propType = metaObject.getSetterType(innerResultMapping.getProperty());
            //该java类型的处理器
            final TypeHandler<?> typeHandler = typeHandlerRegistry.getTypeHandler(propType);
            //该列对应的java类型的实例
            final Object propValue = typeHandler.getResult(resultSet, prependPrefix(innerResultMapping.getColumn(), columnPrefix));
            // issue #353 & #560 do not execute nested query if key is null
            if (propValue != null) {
                //通过 metaObject 给该对象的每一个属性赋值
                metaObject.setValue(innerResultMapping.getProperty(), propValue);
                foundValues = true;
            }
        }
        return foundValues ? parameterObject : null;
    }

    private Object instantiateParameterObject(Class<?> parameterType) {
        if (parameterType == null) {
            return new HashMap<Object, Object>();
        } else {
            return objectFactory.create(parameterType);
        }
    }

    /**
     * 获取鉴别器对应的映射
     *
     * @param resultSet 结果集
     * @param resultMap 鉴别器所在的映射
     * @param columnPrefix 前缀
     * @return 获取鉴别器对应的映射
     * @throws SQLException 异常
     */
    public ResultMap resolveDiscriminatedResultMap(ResultSet resultSet, ResultMap resultMap, String columnPrefix) throws SQLException {
        Set<String> pastDiscriminators = new HashSet<String>();
        Discriminator discriminator = resultMap.getDiscriminator();
        while (discriminator != null) {
            //获取鉴别器的值
            final Object value = getDiscriminatorValue(resultSet, discriminator, columnPrefix);
            final String discriminatedMapId = discriminator.getMapIdFor(String.valueOf(value));
            //鉴别器对应的映射是存在
            boolean hasResultMap = configuration.hasResultMap(discriminatedMapId);
            if (hasResultMap) {
                resultMap = configuration.getResultMap(discriminatedMapId);
                Discriminator lastDiscriminator = discriminator;
                discriminator = resultMap.getDiscriminator();
                if (discriminator == lastDiscriminator || !pastDiscriminators.add(discriminatedMapId)) {
                    break;
                }
            } else {
                break;
            }
        }
        return resultMap;
    }

    //
    // DISCRIMINATOR
    //

    /**
     * 获取鉴别器的值
     *
     * <resultMap id="employeeMap" type="Employee" extends="personMap">
     * <result property="jobTitle" column="jobTitle"/>
     * <discriminator column="employeeType" javaType="String">
     * <case value="DirectorType" resultMap="directorMap"/>
     * </discriminator>
     * </resultMap>
     *
     * @param resultSet 结果集
     * @param discriminator 鉴别器
     * @param columnPrefix 前缀
     * @return 获取鉴别器的值
     * @throws SQLException 异常
     */
    private Object getDiscriminatorValue(ResultSet resultSet, Discriminator discriminator, String columnPrefix) throws SQLException {
        final ResultMapping resultMapping = discriminator.getResultMapping();
        final TypeHandler<?> typeHandler = resultMapping.getTypeHandler();
        //列名称
        String fullColumnName = prependPrefix(resultMapping.getColumn(), columnPrefix);
        return typeHandler.getResult(resultSet, fullColumnName);
    }

    private String prependPrefix(String columnName, String prefix) {
        if (columnName == null || columnName.length() == 0 || prefix == null || prefix.length() == 0) {
            return columnName;
        }
        return prefix + columnName;
    }

    private void handleRowValuesForNestedResultMap(ResultSetWrapper rsw, ResultMap resultMap, ResultHandler resultHandler, RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {
        final DefaultResultContext resultContext = new DefaultResultContext();
        skipRows(rsw.getResultSet(), rowBounds);
        Object rowValue = null;
        while (shouldProcessMoreRows(resultContext, rowBounds) && rsw.getResultSet().next()) {
            final ResultMap discriminatedResultMap = resolveDiscriminatedResultMap(rsw.getResultSet(), resultMap, null);
            final CacheKey rowKey = createRowKey(discriminatedResultMap, rsw, null);
            Object partialObject = nestedResultObjects.get(rowKey);
            // issue #577 && #542
            if (mappedStatement.isResultOrdered()) {
                if (partialObject == null && rowValue != null) {
                    nestedResultObjects.clear();
                    storeObject(resultHandler, resultContext, rowValue, parentMapping, rsw.getResultSet());
                }
                rowValue = getRowValue(rsw, discriminatedResultMap, rowKey, rowKey, null, partialObject);
            } else {
                rowValue = getRowValue(rsw, discriminatedResultMap, rowKey, rowKey, null, partialObject);
                if (partialObject == null) {
                    storeObject(resultHandler, resultContext, rowValue, parentMapping, rsw.getResultSet());
                }
            }
        }
        if (rowValue != null && mappedStatement.isResultOrdered() && shouldProcessMoreRows(resultContext, rowBounds)) {
            storeObject(resultHandler, resultContext, rowValue, parentMapping, rsw.getResultSet());
        }
    }

    //
    // HANDLE NESTED RESULT MAPS
    //

    private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap, CacheKey combinedKey, CacheKey absoluteKey, String columnPrefix, Object partialObject) throws SQLException {
        final String resultMapId = resultMap.getId();
        Object resultObject = partialObject;
        if (resultObject != null) {
            final MetaObject metaObject = configuration.newMetaObject(resultObject);
            putAncestor(absoluteKey, resultObject, resultMapId, columnPrefix);
            applyNestedResultMappings(rsw, resultMap, metaObject, columnPrefix, combinedKey, false);
            ancestorObjects.remove(absoluteKey);
        } else {
            final ResultLoaderMap lazyLoader = new ResultLoaderMap();
            resultObject = createResultObject(rsw, resultMap, lazyLoader, columnPrefix);
            if (resultObject != null && !typeHandlerRegistry.hasTypeHandler(resultMap.getType())) {
                final MetaObject metaObject = configuration.newMetaObject(resultObject);
                boolean foundValues = !resultMap.getConstructorResultMappings().isEmpty();
                if (shouldApplyAutomaticMappings(resultMap, true)) {
                    foundValues = applyAutomaticMappings(rsw, resultMap, metaObject, columnPrefix) || foundValues;
                }
                foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, columnPrefix) || foundValues;
                putAncestor(absoluteKey, resultObject, resultMapId, columnPrefix);
                foundValues = applyNestedResultMappings(rsw, resultMap, metaObject, columnPrefix, combinedKey, true) || foundValues;
                ancestorObjects.remove(absoluteKey);
                foundValues = lazyLoader.size() > 0 || foundValues;
                resultObject = foundValues ? resultObject : null;
            }
            if (combinedKey != CacheKey.NULL_CACHE_KEY) {
                nestedResultObjects.put(combinedKey, resultObject);
            }
        }
        return resultObject;
    }

    //
    // GET VALUE FROM ROW FOR NESTED RESULT MAP
    //

    private void putAncestor(CacheKey rowKey, Object resultObject, String resultMapId, String columnPrefix) {
        if (!ancestorColumnPrefix.containsKey(resultMapId)) {
            ancestorColumnPrefix.put(resultMapId, columnPrefix);
        }
        ancestorObjects.put(rowKey, resultObject);
    }

    private boolean applyNestedResultMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, String parentPrefix, CacheKey parentRowKey, boolean newObject) {
        boolean foundValues = false;
        for (ResultMapping resultMapping : resultMap.getPropertyResultMappings()) {
            final String nestedResultMapId = resultMapping.getNestedResultMapId();
            if (nestedResultMapId != null && resultMapping.getResultSet() == null) {
                try {
                    final String columnPrefix = getColumnPrefix(parentPrefix, resultMapping);
                    final ResultMap nestedResultMap = getNestedResultMap(rsw.getResultSet(), nestedResultMapId, columnPrefix);
                    CacheKey rowKey = null;
                    Object ancestorObject = null;
                    if (ancestorColumnPrefix.containsKey(nestedResultMapId)) {
                        rowKey = createRowKey(nestedResultMap, rsw, ancestorColumnPrefix.get(nestedResultMapId));
                        ancestorObject = ancestorObjects.get(rowKey);
                    }
                    if (ancestorObject != null) {
                        if (newObject) {
                            metaObject.setValue(resultMapping.getProperty(), ancestorObject);
                        }
                    } else {
                        rowKey = createRowKey(nestedResultMap, rsw, columnPrefix);
                        final CacheKey combinedKey = combineKeys(rowKey, parentRowKey);
                        Object rowValue = nestedResultObjects.get(combinedKey);
                        boolean knownValue = (rowValue != null);
                        final Object collectionProperty = instantiateCollectionPropertyIfAppropriate(resultMapping, metaObject);
                        if (anyNotNullColumnHasValue(resultMapping, columnPrefix, rsw.getResultSet())) {
                            rowValue = getRowValue(rsw, nestedResultMap, combinedKey, rowKey, columnPrefix, rowValue);
                            if (rowValue != null && !knownValue) {
                                if (collectionProperty != null) {
                                    final MetaObject targetMetaObject = configuration.newMetaObject(collectionProperty);
                                    targetMetaObject.add(rowValue);
                                } else {
                                    metaObject.setValue(resultMapping.getProperty(), rowValue);
                                }
                                foundValues = true;
                            }
                        }
                    }
                } catch (SQLException e) {
                    throw new ExecutorException("Error getting nested result map values for '" + resultMapping.getProperty() + "'.  Cause: " + e, e);
                }
            }
        }
        return foundValues;
    }

    //
    // NESTED RESULT MAP (JOIN MAPPING)
    //

    private String getColumnPrefix(String parentPrefix, ResultMapping resultMapping) {
        final StringBuilder columnPrefixBuilder = new StringBuilder();
        if (parentPrefix != null) {
            columnPrefixBuilder.append(parentPrefix);
        }
        if (resultMapping.getColumnPrefix() != null) {
            columnPrefixBuilder.append(resultMapping.getColumnPrefix());
        }
        return columnPrefixBuilder.length() == 0 ? null : columnPrefixBuilder.toString().toUpperCase(Locale.ENGLISH);
    }

    private boolean anyNotNullColumnHasValue(ResultMapping resultMapping, String columnPrefix, ResultSet rs) throws SQLException {
        Set<String> notNullColumns = resultMapping.getNotNullColumns();
        boolean anyNotNullColumnHasValue = true;
        if (notNullColumns != null && !notNullColumns.isEmpty()) {
            anyNotNullColumnHasValue = false;
            for (String column : notNullColumns) {
                rs.getObject(prependPrefix(column, columnPrefix));
                if (!rs.wasNull()) {
                    anyNotNullColumnHasValue = true;
                    break;
                }
            }
        }
        return anyNotNullColumnHasValue;
    }

    private ResultMap getNestedResultMap(ResultSet rs, String nestedResultMapId, String columnPrefix) throws SQLException {
        ResultMap nestedResultMap = configuration.getResultMap(nestedResultMapId);
        return resolveDiscriminatedResultMap(rs, nestedResultMap, columnPrefix);
    }

    private CacheKey createRowKey(ResultMap resultMap, ResultSetWrapper rsw, String columnPrefix) throws SQLException {
        final CacheKey cacheKey = new CacheKey();
        cacheKey.update(resultMap.getId());
        List<ResultMapping> resultMappings = getResultMappingsForRowKey(resultMap);
        if (resultMappings.size() == 0) {
            if (Map.class.isAssignableFrom(resultMap.getType())) {
                createRowKeyForMap(rsw, cacheKey);
            } else {
                createRowKeyForUnmappedProperties(resultMap, rsw, cacheKey, columnPrefix);
            }
        } else {
            createRowKeyForMappedProperties(resultMap, rsw, cacheKey, resultMappings, columnPrefix);
        }
        return cacheKey;
    }

    //
    // UNIQUE RESULT KEY
    //

    private CacheKey combineKeys(CacheKey rowKey, CacheKey parentRowKey) {
        if (rowKey.getUpdateCount() > 1 && parentRowKey.getUpdateCount() > 1) {
            CacheKey combinedKey;
            try {
                combinedKey = rowKey.clone();
            } catch (CloneNotSupportedException e) {
                throw new ExecutorException("Error cloning cache key.  Cause: " + e, e);
            }
            combinedKey.update(parentRowKey);
            return combinedKey;
        }
        return CacheKey.NULL_CACHE_KEY;
    }

    private List<ResultMapping> getResultMappingsForRowKey(ResultMap resultMap) {
        List<ResultMapping> resultMappings = resultMap.getIdResultMappings();
        if (resultMappings.size() == 0) {
            resultMappings = resultMap.getPropertyResultMappings();
        }
        return resultMappings;
    }

    private void createRowKeyForMappedProperties(ResultMap resultMap, ResultSetWrapper rsw, CacheKey cacheKey, List<ResultMapping> resultMappings, String columnPrefix) throws SQLException {
        for (ResultMapping resultMapping : resultMappings) {
            if (resultMapping.getNestedResultMapId() != null && resultMapping.getResultSet() == null) {
                // Issue #392
                final ResultMap nestedResultMap = configuration.getResultMap(resultMapping.getNestedResultMapId());
                createRowKeyForMappedProperties(nestedResultMap, rsw, cacheKey, nestedResultMap.getConstructorResultMappings(),
                        prependPrefix(resultMapping.getColumnPrefix(), columnPrefix));
            } else if (resultMapping.getNestedQueryId() == null) {
                final String column = prependPrefix(resultMapping.getColumn(), columnPrefix);
                final TypeHandler<?> th = resultMapping.getTypeHandler();
                List<String> mappedColumnNames = rsw.getMappedColumnNames(resultMap, columnPrefix);
                // Issue #114
                if (column != null && mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH))) {
                    final Object value = th.getResult(rsw.getResultSet(), column);
                    if (value != null) {
                        cacheKey.update(column);
                        cacheKey.update(value);
                    }
                }
            }
        }
    }

    private void createRowKeyForUnmappedProperties(ResultMap resultMap, ResultSetWrapper rsw, CacheKey cacheKey, String columnPrefix) throws SQLException {
        final MetaClass metaType = MetaClass.forClass(resultMap.getType());
        List<String> unmappedColumnNames = rsw.getUnmappedColumnNames(resultMap, columnPrefix);
        for (String column : unmappedColumnNames) {
            String property = column;
            if (columnPrefix != null && !columnPrefix.isEmpty()) {
                // When columnPrefix is specified, ignore columns without the prefix.
                if (column.toUpperCase(Locale.ENGLISH).startsWith(columnPrefix)) {
                    property = column.substring(columnPrefix.length());
                } else {
                    continue;
                }
            }
            if (metaType.findProperty(property, configuration.isMapUnderscoreToCamelCase()) != null) {
                String value = rsw.getResultSet().getString(column);
                if (value != null) {
                    cacheKey.update(column);
                    cacheKey.update(value);
                }
            }
        }
    }

    private void createRowKeyForMap(ResultSetWrapper rsw, CacheKey cacheKey) throws SQLException {
        List<String> columnNames = rsw.getColumnNames();
        for (String columnName : columnNames) {
            final String value = rsw.getResultSet().getString(columnName);
            if (value != null) {
                cacheKey.update(columnName);
                cacheKey.update(value);
            }
        }
    }

    private static class PendingRelation {

        public MetaObject metaObject;

        public ResultMapping propertyMapping;
    }
}
