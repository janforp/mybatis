package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.session.Configuration;

import java.util.Map;

/**
 * @author Clinton Begin
 */

/**
 * foreach SQL节点
 * /**
 * *  <foreach item="item" index="index" collection="list" open="(" close=")" separator=",">
 * *       <if test="index != 0">,</if> #{item}
 * *     </foreach>
 */
public class ForEachSqlNode implements SqlNode {

    public static final String ITEM_PREFIX = "__frch_";

    private ExpressionEvaluator evaluator;

    private String collectionExpression;

    private SqlNode contents;

    private String open;

    private String close;

    private String separator;

    private String item;

    private String index;

    private Configuration configuration;

    public ForEachSqlNode(Configuration configuration, SqlNode contents, String collectionExpression, String index, String item, String open, String close, String separator) {
        this.evaluator = new ExpressionEvaluator();
        this.collectionExpression = collectionExpression;
        this.contents = contents;
        this.open = open;
        this.close = close;
        this.separator = separator;
        this.index = index;
        this.item = item;
        this.configuration = configuration;
    }

    private static String itemizeItem(String item, int i) {
        //__frch_ + item + "_" + i
        return ITEM_PREFIX + item + "_" + i;
    }

    @Override
    public boolean apply(DynamicContext context) {
        Map<String, Object> bindings = context.getBindings();
        //解析collectionExpression->iterable,核心用的ognl
        final Iterable<?> iterable = evaluator.evaluateIterable(collectionExpression, bindings);
        if (!iterable.iterator().hasNext()) {
            //没有元素
            return true;
        }
        //是否是第一个元素
        boolean first = true;
        //加上(
        applyOpen(context);
        int i = 0;
        for (Object item : iterable) {
            DynamicContext oldContext = context;
            if (first) {
                //如果第一个元素，则拼接 ""
                context = new PrefixedContext(context, "");
            } else if (separator != null) {
                //如果不是第一个元素，且分隔符不为空，则遍历每一个元素中间拼接分隔符
                context = new PrefixedContext(context, separator);
            } else {
                //分隔符空，则遍历每一个元素中间拼接 ""
                context = new PrefixedContext(context, "");
            }
            int uniqueNumber = context.getUniqueNumber();
            // Issue #709
            if (item instanceof Map.Entry) {
                @SuppressWarnings("unchecked")
                Map.Entry<Object, Object> mapEntry = (Map.Entry<Object, Object>) item;
                applyIndex(context, mapEntry.getKey(), uniqueNumber);
                applyItem(context, mapEntry.getValue(), uniqueNumber);
            } else {
                //索引
                applyIndex(context, i, uniqueNumber);
                //加上一个元素
                applyItem(context, item, uniqueNumber);
            }
            contents.apply(new FilteredDynamicContext(configuration, context, index, this.item, uniqueNumber));
            if (first) {
                first = !((PrefixedContext) context).isPrefixApplied();
            }
            context = oldContext;
            i++;
        }
        //加上)
        applyClose(context);
        return true;
    }

    private void applyIndex(DynamicContext context, Object o, int i) {
        if (index != null) {
            context.bind(index, o);
            context.bind(itemizeItem(index, i), o);
        }
    }

    private void applyItem(DynamicContext context, Object o, int i) {
        if (item != null) {
            context.bind(item, o);
            context.bind(itemizeItem(item, i), o);
        }
    }

    private void applyOpen(DynamicContext context) {
        if (open != null) {
            context.appendSql(open);
        }
    }

    private void applyClose(DynamicContext context) {
        if (close != null) {
            context.appendSql(close);
        }
    }

    //被过滤的动态上下文
    private static class FilteredDynamicContext extends DynamicContext {

        private DynamicContext delegate;

        private int index;

        private String itemIndex;

        private String item;

        public FilteredDynamicContext(Configuration configuration, DynamicContext delegate, String itemIndex, String item, int i) {
            super(configuration, null);
            this.delegate = delegate;
            this.index = i;
            this.itemIndex = itemIndex;
            this.item = item;
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
        public String getSql() {
            return delegate.getSql();
        }

        @Override
        public void appendSql(String sql) {
            GenericTokenParser parser = new GenericTokenParser("#{", "}", new TokenHandler() {
                @Override
                public String handleToken(String content) {
                    String newContent = content.replaceFirst("^\\s*" + item + "(?![^.,:\\s])", itemizeItem(item, index));
                    if (itemIndex != null && newContent.equals(content)) {
                        newContent = content.replaceFirst("^\\s*" + itemIndex + "(?![^.,:\\s])", itemizeItem(itemIndex, index));
                    }
                    return new StringBuilder("#{").append(newContent).append("}").toString();
                }
            });

            delegate.appendSql(parser.parse(sql));
        }

        @Override
        public int getUniqueNumber() {
            return delegate.getUniqueNumber();
        }

    }

    //前缀上下文
    private class PrefixedContext extends DynamicContext {

        private DynamicContext delegate;

        private String prefix;

        private boolean prefixApplied;

        public PrefixedContext(DynamicContext delegate, String prefix) {
            super(configuration, null);
            this.delegate = delegate;
            this.prefix = prefix;
            this.prefixApplied = false;
        }

        public boolean isPrefixApplied() {
            return prefixApplied;
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
        public void appendSql(String sql) {
            if (!prefixApplied && sql != null && sql.trim().length() > 0) {
                delegate.appendSql(prefix);
                prefixApplied = true;
            }
            delegate.appendSql(sql);
        }

        @Override
        public String getSql() {
            return delegate.getSql();
        }

        @Override
        public int getUniqueNumber() {
            return delegate.getUniqueNumber();
        }
    }
}
