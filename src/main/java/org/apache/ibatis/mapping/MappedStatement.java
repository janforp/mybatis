package org.apache.ibatis.mapping;

import lombok.Getter;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 映射的语句，mapper.xml 文件的每个方法都有对应的 MappedStatement 对象
 *
 * @author Clinton Begin
 */
public final class MappedStatement {

    @Getter
    private String resource;

    @Getter
    private Configuration configuration;

    @Getter
    private String id;

    @Getter
    private Integer fetchSize;

    @Getter
    private Integer timeout;

    @Getter
    private StatementType statementType;

    @Getter
    private ResultSetType resultSetType;

    /**
     * 该对象是 sql，其中维护一个 sqlNode 列表，每一个sqlNode 可能是静态跟动态的，其中动态的可能还带着如 test = "id != null" 这样的表达式
     * 而这些表达式只有在用户调用该方法的时候，通过传入参数才能进行计算
     */
    @Getter
    private SqlSource sqlSource;

    /**
     * namespace下的缓存，SqlSession缓存
     *
     * 其实他与二级缓存也就是 mapper.xml 对应的缓存是同一个实例
     */
    @Getter
    private Cache cache;

    @Getter
    private ParameterMap parameterMap;

    @Getter
    private List<ResultMap> resultMaps;

    /**
     * 该条sql执行的时候是否需要清楚缓存
     *
     * 如果没有指定，则查询不刷新，飞查询刷新
     */
    @Getter
    private boolean flushCacheRequired;

    /**
     * 如果sql没指定，则查询默认使用缓存，非查询不用缓存
     */
    @Getter
    private boolean useCache;

    @Getter
    private boolean resultOrdered;

    @Getter
    private SqlCommandType sqlCommandType;

    /**
     * 该sql如果有自动获取主键的sql，则指定一个确定的主键生成器
     *
     * canUseKeyGenerator ? new Jdbc3KeyGenerator() : new NoKeyGenerator();
     */
    @Getter
    private KeyGenerator keyGenerator;

    @Getter
    private String[] keyProperties;

    @Getter
    private String[] keyColumns;

    private boolean hasNestedResultMaps;

    @Getter
    private String databaseId;

    /**
     * 日志
     */
    @Getter
    private Log statementLog;

    @Getter
    private LanguageDriver lang;

    @Getter
    private String[] resultSets;

    MappedStatement() {
        // constructor disabled
    }

    private static String[] delimitedStringtoArray(String in) {
        if (in == null || in.trim().length() == 0) {
            return null;
        } else {
            return in.split(",");
        }
    }

    public boolean hasNestedResultMaps() {
        return hasNestedResultMaps;
    }

    /**
     * 获取 BoundSql
     *
     * @param parameterObject 参数
     * @return BoundSql
     */
    public BoundSql getBoundSql(Object parameterObject) {
        //其实就是调用sqlSource.getBoundSql
        BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
        //剩下的可以暂时忽略
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (parameterMappings == null || parameterMappings.isEmpty()) {
            boundSql = new BoundSql(configuration, boundSql.getSql(), parameterMap.getParameterMappings(), parameterObject);
        }
        // check for nested result maps in parameter mappings (issue #30)
        for (ParameterMapping pm : boundSql.getParameterMappings()) {
            String rmId = pm.getResultMapId();
            if (rmId != null) {
                ResultMap rm = configuration.getResultMap(rmId);
                if (rm != null) {
                    hasNestedResultMaps |= rm.hasNestedResultMaps();
                }
            }
        }
        return boundSql;
    }

    //静态内部类，建造者模式
    public static class Builder {

        private MappedStatement mappedStatement = new MappedStatement();

        public Builder(Configuration configuration, String id, SqlSource sqlSource, SqlCommandType sqlCommandType) {
            mappedStatement.configuration = configuration;
            mappedStatement.id = id;
            mappedStatement.sqlSource = sqlSource;
            //TODO  此处为何直接指定？
            mappedStatement.statementType = StatementType.PREPARED;
            mappedStatement.parameterMap = new ParameterMap.Builder(configuration, "defaultParameterMap", null, new ArrayList<ParameterMapping>()).build();
            mappedStatement.resultMaps = new ArrayList<ResultMap>();
            mappedStatement.timeout = configuration.getDefaultStatementTimeout();
            mappedStatement.sqlCommandType = sqlCommandType;

            //配置
            boolean useGeneratedKeys = configuration.isUseGeneratedKeys();
            boolean isInsert = SqlCommandType.INSERT.equals(sqlCommandType);
            boolean canUseKeyGenerator = useGeneratedKeys && isInsert;
            //TODO 只能使用 Jdbc3KeyGenerator 生成主键吗？
            mappedStatement.keyGenerator = canUseKeyGenerator ? new Jdbc3KeyGenerator() : new NoKeyGenerator();

            String logId = id;
            if (configuration.getLogPrefix() != null) {
                logId = configuration.getLogPrefix() + id;
            }
            mappedStatement.statementLog = LogFactory.getLog(logId);
            LanguageDriver defaultScriptingLanguageInstance = configuration.getDefaultScriptingLanuageInstance();
            mappedStatement.lang = defaultScriptingLanguageInstance;
        }

        public Builder resource(String resource) {
            mappedStatement.resource = resource;
            return this;
        }

        public String id() {
            return mappedStatement.id;
        }

        public Builder parameterMap(ParameterMap parameterMap) {
            mappedStatement.parameterMap = parameterMap;
            return this;
        }

        public Builder resultMaps(List<ResultMap> resultMaps) {
            mappedStatement.resultMaps = resultMaps;
            for (ResultMap resultMap : resultMaps) {
                mappedStatement.hasNestedResultMaps = mappedStatement.hasNestedResultMaps || resultMap.hasNestedResultMaps();
            }
            return this;
        }

        public Builder fetchSize(Integer fetchSize) {
            mappedStatement.fetchSize = fetchSize;
            return this;
        }

        public Builder timeout(Integer timeout) {
            mappedStatement.timeout = timeout;
            return this;
        }

        public Builder statementType(StatementType statementType) {
            mappedStatement.statementType = statementType;
            return this;
        }

        public Builder resultSetType(ResultSetType resultSetType) {
            mappedStatement.resultSetType = resultSetType;
            return this;
        }

        public Builder cache(Cache cache) {
            mappedStatement.cache = cache;
            return this;
        }

        public Builder flushCacheRequired(boolean flushCacheRequired) {
            mappedStatement.flushCacheRequired = flushCacheRequired;
            return this;
        }

        public Builder useCache(boolean useCache) {
            mappedStatement.useCache = useCache;
            return this;
        }

        public Builder resultOrdered(boolean resultOrdered) {
            mappedStatement.resultOrdered = resultOrdered;
            return this;
        }

        public Builder keyGenerator(KeyGenerator keyGenerator) {
            mappedStatement.keyGenerator = keyGenerator;
            return this;
        }

        public Builder keyProperty(String keyProperty) {
            mappedStatement.keyProperties = delimitedStringtoArray(keyProperty);
            return this;
        }

        public Builder keyColumn(String keyColumn) {
            mappedStatement.keyColumns = delimitedStringtoArray(keyColumn);
            return this;
        }

        public Builder databaseId(String databaseId) {
            mappedStatement.databaseId = databaseId;
            return this;
        }

        public Builder lang(LanguageDriver driver) {
            mappedStatement.lang = driver;
            return this;
        }

        public Builder resultSets(String resultSet) {
            mappedStatement.resultSets = delimitedStringtoArray(resultSet);
            return this;
        }

        public MappedStatement build() {
            assert mappedStatement.configuration != null;
            assert mappedStatement.id != null;
            assert mappedStatement.sqlSource != null;
            assert mappedStatement.lang != null;
            mappedStatement.resultMaps = Collections.unmodifiableList(mappedStatement.resultMaps);
            return mappedStatement;
        }
    }
}
