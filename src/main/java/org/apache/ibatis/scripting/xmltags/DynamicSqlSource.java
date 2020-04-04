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

    private SqlNode rootSqlNode;//被解析出来的所有sql片段，并且带上各种动态标签的计算表达式

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
        //生成一个动态上下文:ognl

        //这里对象很关键，有参数，并且拼接动态sql的结果也在该对象
        DynamicContext context = new DynamicContext(configuration, parameterObject);

        //这里SqlNode.apply只是将${}这种参数替换掉，并没有替换#{}这种参数
        //在动态sql的情况就是进行各标签的表达式计算
        //该行执行之后在 context 对象中的 sqlBuild 属性就有了拼接之后的带占位符的sql啦
        rootSqlNode.apply(context);

        //调用SqlSourceBuilder
        SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
        //SqlSourceBuilder.parse，拼接好的，但是还有占位符 #{} 的sql
        String fullSqlWithPlaceholder = context.getSql();

        Map<String, Object> contextBindings = context.getBindings();

        Class<?> parameterType = (parameterObject == null ? Object.class : parameterObject.getClass());
        //动态sql经过传入的参数，计算每个动态标签中的表达式之后拼接之后的静态sql（带？占位符）以及使用到的参数对象
        SqlSource sqlSource = sqlSourceParser.parse(fullSqlWithPlaceholder, parameterType, contextBindings);

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
