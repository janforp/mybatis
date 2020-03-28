package org.apache.ibatis.executor.resultset;

import lombok.Getter;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.ObjectTypeHandler;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.ibatis.type.UnknownTypeHandler;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 包装一下ResultSet
 *
 * @author Iwao AVE!
 */
class ResultSetWrapper {

    /**
     * 查询结果
     */
    @Getter
    private final ResultSet resultSet;

    /**
     * 类型注册器
     */
    private final TypeHandlerRegistry typeHandlerRegistry;

    /**
     * 结果列的 name 或者 label 集合
     */
    @Getter
    private final List<String> columnNames = new ArrayList<String>();

    /**
     * 结果列的 java程序类型 the fully-qualified name of the class in the Java programming language that would be used by the method
     */
    private final List<String> classNames = new ArrayList<String>();

    /**
     * 结果列的 数据库类型
     */
    private final List<JdbcType> jdbcTypes = new ArrayList<JdbcType>();

    /**
     * key：columnName
     * value:class-处理器
     */
    private final Map<String, Map<Class<?>, TypeHandler<?>>> typeHandlerMap = new HashMap<String, Map<Class<?>, TypeHandler<?>>>();

    /**
     * 结果集合中映射的列
     * key:resultMap.getId() + ":" + columnPrefix
     * value:结果集合中映射的列集合
     */
    private Map<String, List<String>> mappedColumnNamesMap = new HashMap<String, List<String>>();

    /**
     * 结果集合中没有映射的列
     * key:resultMap.getId() + ":" + columnPrefix
     * value:结果集合中没有映射的列集合
     */
    private Map<String, List<String>> unMappedColumnNamesMap = new HashMap<String, List<String>>();

    /**
     * 查询结果包装器
     *
     * @param resultSet 查询结果
     * @param configuration 配置
     * @throws SQLException 异常
     */
    public ResultSetWrapper(ResultSet resultSet, Configuration configuration) throws SQLException {
        super();
        this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        this.resultSet = resultSet;
        //结果元数据
        final ResultSetMetaData metaData = resultSet.getMetaData();
        //结果列数量
        final int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            //决定是 label 还是 name
            columnNames.add(configuration.isUseColumnLabel() ? metaData.getColumnLabel(i) : metaData.getColumnName(i));

            //该列的数据库类型
            int columnType = metaData.getColumnType(i);
            JdbcType jdbcType = JdbcType.forCode(columnType);
            jdbcTypes.add(jdbcType);

            //the fully-qualified name of the class in the Java programming language that would be used by the method
            String columnClassName = metaData.getColumnClassName(i);
            classNames.add(columnClassName);
        }
    }

    public List<String> getClassNames() {
        return Collections.unmodifiableList(classNames);
    }

    /**
     * Gets the type handler to use when reading the result set.
     * Tries to get from the TypeHandlerRegistry by searching for the property type.
     * If not found it gets the column JDBC type and tries to get a handler for it.
     *
     * @param propertyType 属性类型
     * @param columnName 列名称
     * @return 类型处理器
     */
    public TypeHandler<?> getTypeHandler(Class<?> propertyType, String columnName) {
        TypeHandler<?> handler = null;
        Map<Class<?>, TypeHandler<?>> columnHandlers = typeHandlerMap.get(columnName);
        if (columnHandlers == null) {
            columnHandlers = new HashMap<Class<?>, TypeHandler<?>>();
            typeHandlerMap.put(columnName, columnHandlers);
        } else {
            handler = columnHandlers.get(propertyType);
        }
        if (handler == null) {
            //先从注册器拿，如果xml中写列，则此处能拿到
            handler = typeHandlerRegistry.getTypeHandler(propertyType);
            // Replicate logic of UnknownTypeHandler#resolveTypeHandler
            // See issue #59 comment 10
            if (handler == null || handler instanceof UnknownTypeHandler) {
                //结果列的 name 或者 label 集合
                final int index = columnNames.indexOf(columnName);
                //结果列的 数据库类型
                final JdbcType jdbcType = jdbcTypes.get(index);
                //结果列的 java程序类型 the fully-qualified name of the class in the Java programming language that would be used by the method
                String className = classNames.get(index);
                final Class<?> javaType = resolveClass(className);
                if (javaType != null && jdbcType != null) {
                    handler = typeHandlerRegistry.getTypeHandler(javaType, jdbcType);
                } else if (javaType != null) {
                    handler = typeHandlerRegistry.getTypeHandler(javaType);
                } else if (jdbcType != null) {
                    handler = typeHandlerRegistry.getTypeHandler(jdbcType);
                }
            }

            //通过数据库元数据也获取不到该属性的处理器，则没办法，只能设置为 ObjectTypeHandler
            if (handler == null || handler instanceof UnknownTypeHandler) {
                handler = new ObjectTypeHandler();
            }
            columnHandlers.put(propertyType, handler);
        }
        return handler;
    }

    private Class<?> resolveClass(String className) {
        try {
            return Resources.classForName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * 加载映射的和没有映射的 columnName
     *
     * @param resultMap 隐私 resultMap
     * @param columnPrefix 列前缀
     * @throws SQLException 异常
     */
    private void loadMappedAndUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
        //映射成功的columnName集合
        List<String> mappedColumnNames = new ArrayList<String>();
        //没有映射成功的columnName集合
        List<String> unmappedColumnNames = new ArrayList<String>();

        //列名称前缀的大写
        final String upperColumnPrefix = (columnPrefix == null ? null : columnPrefix.toUpperCase(Locale.ENGLISH));

        //ResultMap 中的 columnNameSet
        Set<String> resultMapMappedColumns = resultMap.getMappedColumns();
        //把这些列拼接上前缀，如果前缀没有指定，则原样返回
        final Set<String> mappedColumns = prependPrefixes(resultMapMappedColumns, upperColumnPrefix);

        //结果列的 name 或者 label 集合
        for (String columnName : columnNames) {//便利结果的所有列名称
            final String upperColumnName = columnName.toUpperCase(Locale.ENGLISH);
            if (mappedColumns.contains(upperColumnName)) {
                mappedColumnNames.add(upperColumnName);
            } else {
                unmappedColumnNames.add(columnName);
            }
        }

        //resultMap.getId() + ":" + columnPrefix;
        String mapKey = getMapKey(resultMap, columnPrefix);
        mappedColumnNamesMap.put(mapKey, mappedColumnNames);
        unMappedColumnNamesMap.put(mapKey, unmappedColumnNames);
    }

    /**
     * 获取ResultMap中映射的列名称集合
     *
     * @param resultMap 映射对象
     * @param columnPrefix 前缀
     * @return 获取ResultMap中映射的列名称集合
     * @throws SQLException 异常
     */
    public List<String> getMappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
        String mapKey = getMapKey(resultMap, columnPrefix);
        List<String> mappedColumnNames = mappedColumnNamesMap.get(mapKey);
        if (mappedColumnNames == null) {
            //如果第一次获取不到，则再加载一次
            loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);
            mappedColumnNames = mappedColumnNamesMap.get(mapKey);
        }
        return mappedColumnNames;
    }

    /**
     * 获取ResultMap中没有映射的列名称集合
     *
     * @param resultMap 映射对象
     * @param columnPrefix 前缀
     * @return 获取ResultMap中没有映射的列名称集合
     * @throws SQLException 异常
     */
    public List<String> getUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
        List<String> unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
        if (unMappedColumnNames == null) {
            //如果第一次获取不到，则再加载一次
            loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);
            unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
        }
        return unMappedColumnNames;
    }

    private String getMapKey(ResultMap resultMap, String columnPrefix) {
        return resultMap.getId() + ":" + columnPrefix;
    }

    /**
     * 把这些列拼接上前缀
     *
     * @param columnNameSet 待拼接的列名称集合
     * @param prefix 前缀
     * @return 把这些列拼接上前缀然后返回
     */
    private Set<String> prependPrefixes(Set<String> columnNameSet, String prefix) {
        if (columnNameSet == null || columnNameSet.isEmpty() || prefix == null || prefix.length() == 0) {
            return columnNameSet;
        }

        //拼接前缀之后的集合
        final Set<String> prefixed = new HashSet<String>();
        for (String columnName : columnNameSet) {
            prefixed.add(prefix + columnName);
        }
        return prefixed;
    }
}
