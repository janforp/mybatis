package org.apache.ibatis.scripting.xmltags;

/**
 * @author Clinton Begin
 */

import java.util.Map;

/**
 * if SQL节点
 */
public class IfSqlNode implements SqlNode {

    /**
     * 表达式计算器
     */
    private ExpressionEvaluator evaluator;

    /**
     * id != null
     */
    private String test;

    /**
     * if标签内的内容
     * and id = #{id}
     */
    private SqlNode contents;

    /**
     * 实例化一个 <if></if> 标签对象
     *
     * @param contents and id = #{id} 对应的静态标签
     * @param test <if test="id != null"> 中的test表达式
     */
    public IfSqlNode(SqlNode contents, String test) {
        this.test = test;
        this.contents = contents;
        this.evaluator = new ExpressionEvaluator();
    }

    @Override
    public boolean apply(DynamicContext context) {
        //如果满足条件，则apply，并返回true
        Map<String, Object> bindings = context.getBindings();

        //通过传入的参数计算 if 标签中的 test 是否为 true/false
        boolean aBoolean = evaluator.evaluateBoolean(test, bindings);
        if (aBoolean) {
            contents.apply(context);
            return true;
        }
        return false;
    }

}
