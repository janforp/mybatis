package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

import java.util.Map;

/**
 * @author Clinton Begin
 */

/**
 * 动态SQL源码
 */
public class DynamicSqlSource implements SqlSource {

    private Configuration configuration;

    private SqlNode rootSqlNode;

    /**
     * @param rootSqlNode 由sql语句经过初步解析得到的SqlNodeList
     */
    public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
        this.configuration = configuration;
        this.rootSqlNode = rootSqlNode;
    }

    /**
     * SQL + 参数 + 占位符列表
     *
     * @param parameterObject 参数
     * @return SQL + 参数 + 占位符列表
     */
    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        //生成一个动态上下文
        DynamicContext context = new DynamicContext(configuration, parameterObject);
        //这里SqlNode.apply只是将${}这种参数替换掉，并没有替换#{}这种参数
        rootSqlNode.apply(context);
        //调用SqlSourceBuilder
        SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
        Class<?> parameterType = (parameterObject == null ? Object.class : parameterObject.getClass());
        //SqlSourceBuilder.parse,注意这里返回的是StaticSqlSource,解析完了就把那些参数都替换成?了，也就是最基本的JDBC的SQL写法
        String contextSql = context.getSql();
        Map<String, Object> contextBindings = context.getBindings();
        SqlSource sqlSource = sqlSourceParser.parse(contextSql, parameterType, contextBindings);
        //看似是又去递归调用SqlSource.getBoundSql，其实因为是StaticSqlSource，所以没问题，不是递归调用
        BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
        for (Map.Entry<String, Object> entry : contextBindings.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            boundSql.setAdditionalParameter(key, value);
        }
        return boundSql;
    }
}
