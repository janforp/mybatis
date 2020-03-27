package org.apache.ibatis.scripting.xmltags;

/**
 * @author Clinton Begin
 */

import java.util.Map;

/**
 * if SQL节点
 */
public class IfSqlNode implements SqlNode {

    private ExpressionEvaluator evaluator;

    private String test;

    private SqlNode contents;

    public IfSqlNode(SqlNode contents, String test) {
        this.test = test;
        this.contents = contents;
        this.evaluator = new ExpressionEvaluator();
    }

    @Override
    public boolean apply(DynamicContext context) {
        //如果满足条件，则apply，并返回true
        Map<String, Object> bindings = context.getBindings();
        boolean aBoolean = evaluator.evaluateBoolean(test, bindings);
        if (aBoolean) {
            contents.apply(context);
            return true;
        }
        return false;
    }

}
