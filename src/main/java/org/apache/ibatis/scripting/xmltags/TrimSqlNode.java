package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.session.Configuration;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * <update id="testTrim" parameterType="com.mybatis.pojo.User">
 * update user
 * <trim prefix="set" suffixOverrides=",">
 * <if test="cash!=null and cash!=''">cash= #{cash},</if>
 * <if test="address!=null and address!=''">address= #{address},</if>
 * </trim>
 * <where>id = #{id}</where>
 * </update>
 *
 * <trim prefix="(" suffix=")" suffixOverrides="," >
 * 1.<trim prefix="" suffix="" suffixOverrides="" prefixOverrides=""></trim>
 * prefix:在trim标签内sql语句加上前缀。
 * suffix:在trim标签内sql语句加上后缀。
 * suffixOverrides:指定去除多余的后缀内容，如：suffixOverrides=","，去除trim标签内sql语句多余的后缀","。
 * prefixOverrides:指定去除多余的前缀内容
 *
 * 执行的sql语句也许是这样的：insert into cart (id,user_id,deal_id,) values(1,2,1,);显然是错误的
 * 指定之后语句就会变成insert into cart (id,user_id,deal_id) values(1,2,1);这样就将“，”去掉了。
 * 前缀也是一个道理这里就不说了。
 *
 * @author Clinton Begin
 */
public class TrimSqlNode implements SqlNode {

    private SqlNode contents;

    private String prefix;

    private String suffix;

    private List<String> prefixesToOverride;

    private List<String> suffixesToOverride;

    private Configuration configuration;

    public TrimSqlNode(Configuration configuration, SqlNode contents,
            String prefix, String prefixesToOverride, String suffix, String suffixesToOverride) {

        this(configuration, contents, prefix, parseOverrides(prefixesToOverride), suffix, parseOverrides(suffixesToOverride));
    }

    protected TrimSqlNode(Configuration configuration, SqlNode contents, String prefix,
            List<String> prefixesToOverride, String suffix, List<String> suffixesToOverride) {

        this.contents = contents;
        this.prefix = prefix;
        this.prefixesToOverride = prefixesToOverride;
        this.suffix = suffix;
        this.suffixesToOverride = suffixesToOverride;
        this.configuration = configuration;
    }

    @Override
    public boolean apply(DynamicContext context) {
        FilteredDynamicContext filteredDynamicContext = new FilteredDynamicContext(context);
        boolean result = contents.apply(filteredDynamicContext);
        filteredDynamicContext.applyAll();
        return result;
    }

    public static void main(String[] args) {
        final StringTokenizer parser = new StringTokenizer("a|b|c", "|", false);
        Assert.assertEquals(3, (int) parser.countTokens());
        final List<String> list = new ArrayList<String>(parser.countTokens());
        while (parser.hasMoreTokens()) {
            String token = parser.nextToken().toUpperCase(Locale.ENGLISH);
            list.add(token);
        }
        Assert.assertEquals("A", list.get(0));
        Assert.assertEquals("B", list.get(1));
        Assert.assertEquals("C", list.get(2));
    }

    private static List<String> parseOverrides(String overrides) {
        if (overrides != null) {
            //分隔符
            final StringTokenizer parser = new StringTokenizer(overrides, "|", false);
            final List<String> list = new ArrayList<String>(parser.countTokens());
            while (parser.hasMoreTokens()) {
                list.add(parser.nextToken().toUpperCase(Locale.ENGLISH));
            }
            return list;
        }
        return Collections.emptyList();
    }

    private class FilteredDynamicContext extends DynamicContext {

        private DynamicContext delegate;

        private boolean prefixApplied;

        private boolean suffixApplied;

        private StringBuilder sqlBuffer;

        public FilteredDynamicContext(DynamicContext delegate) {
            super(configuration, null);
            this.delegate = delegate;
            this.prefixApplied = false;
            this.suffixApplied = false;
            this.sqlBuffer = new StringBuilder();
        }

        public void applyAll() {
            sqlBuffer = new StringBuilder(sqlBuffer.toString().trim());
            String trimmedUppercaseSql = sqlBuffer.toString().toUpperCase(Locale.ENGLISH);
            if (trimmedUppercaseSql.length() > 0) {
                applyPrefix(sqlBuffer, trimmedUppercaseSql);
                applySuffix(sqlBuffer, trimmedUppercaseSql);
            }
            delegate.appendSql(sqlBuffer.toString());
        }

        @Override
        public Map<String, Object> getBindings() {
            return delegate.getBindings();
        }

        @Override
        public void bind(String name, Object value) {
            delegate.bind(name, value);
        }

        @Override
        public int getUniqueNumber() {
            return delegate.getUniqueNumber();
        }

        @Override
        public void appendSql(String sql) {
            sqlBuffer.append(sql);
        }

        @Override
        public String getSql() {
            return delegate.getSql();
        }

        /**
         * 拼接前缀
         *
         * @param sql sql语句拼接器
         * @param trimmedUppercaseSql 大写的sql语句
         */
        private void applyPrefix(StringBuilder sql, String trimmedUppercaseSql) {
            if (!prefixApplied) {
                prefixApplied = true;
                if (prefixesToOverride != null) {
                    for (String toRemove : prefixesToOverride) {
                        if (trimmedUppercaseSql.startsWith(toRemove)) {
                            sql.delete(0, toRemove.trim().length());
                            break;
                        }
                    }
                }
                if (prefix != null) {
                    sql.insert(0, " ");
                    sql.insert(0, prefix);
                }
            }
        }

        /**
         * 拼接后缀
         *
         * @param sql sql语句拼接器
         * @param trimmedUppercaseSql 大写的sql语句
         */
        private void applySuffix(StringBuilder sql, String trimmedUppercaseSql) {
            if (!suffixApplied) {
                suffixApplied = true;
                if (suffixesToOverride != null) {
                    for (String toRemove : suffixesToOverride) {
                        if (trimmedUppercaseSql.endsWith(toRemove) || trimmedUppercaseSql.endsWith(toRemove.trim())) {
                            int start = sql.length() - toRemove.trim().length();
                            int end = sql.length();
                            sql.delete(start, end);
                            break;
                        }
                    }
                }
                if (suffix != null) {
                    sql.append(" ");
                    sql.append(suffix);
                }
            }
        }
    }
}
