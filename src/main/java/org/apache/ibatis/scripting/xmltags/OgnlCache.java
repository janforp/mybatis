package org.apache.ibatis.scripting.xmltags;

import ognl.Ognl;
import ognl.OgnlException;
import org.apache.ibatis.builder.BuilderException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches OGNL parsed expressions.
 *
 * @author Eduardo Macarron
 * @see http://code.google.com/p/mybatis/issues/detail?id=342
 * OGNL缓存,根据以上链接，大致是说ognl有性能问题，所以加了一个缓存
 */
public final class OgnlCache {

    private static final Map<String, Object> expressionCache = new ConcurrentHashMap<String, Object>();

    private OgnlCache() {
        // Prevent Instantiation of Static Class
    }

    /**
     * 从表达式 expression 中获取 root 中对应的值
     *
     * @param expression 表达式
     * @param root 参数
     * @return 从表达式 expression 中获取 root 中对应的值
     */
    public static Object getValue(String expression, Object root) {
        try {
            Map<Object, OgnlClassResolver> context = Ognl.createDefaultContext(root, new OgnlClassResolver());
            Object parseExpression = parseExpression(expression);
            return Ognl.getValue(parseExpression, context, root);
        } catch (OgnlException e) {
            throw new BuilderException("Error evaluating expression '" + expression + "'. Cause: " + e, e);
        }
    }

    private static Object parseExpression(String expression) throws OgnlException {
        Object node = expressionCache.get(expression);
        if (node == null) {
            //大致意思就是OgnlParser.topLevelExpression很慢，所以加个缓存，放到ConcurrentHashMap里面
            node = Ognl.parseExpression(expression);
            expressionCache.put(expression, node);
        }
        return node;
    }

}
