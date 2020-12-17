package org.apache.ibatis.scripting.xmltags;

import java.util.List;

/**
 * @author Clinton Begin
 */

/**
 * 混合SQL节点
 * 组合模式：MixedSqlNode 扮演了容器构件角色
 *
 * 对于其他SqlNode子类的功能，稍微概括如下：
 *
 * TextSqlNode：表示包含 ${} 占位符的动态SQL节点，其 apply 方法会使用 GenericTokenParser 解析 ${} 占位符，并直接替换成用户给定的实际参数值
 * IfSqlNode：对应的是动态SQL节点 <If> 节点，其 apply 方法首先通过 ExpressionEvaluator.evaluateBoolean() 方法检测其 test 表达式是否为 true，
 * 然后根据 test 表达式的结果，决定是否执行其子节点的 apply() 方法
 *
 * TrimSqlNode ：会根据子节点的解析结果，添加或删除相应的前缀或后缀。
 *
 * WhereSqlNode 和 SetSqlNode 都继承了 TrimSqlNode
 *
 * ForeachSqlNode：对应 <foreach> 标签，对集合进行迭代
 *
 * 动态SQL中的 <choose>、<when>、<otherwise> 分别解析成 ChooseSqlNode、IfSqlNode、MixedSqlNode
 *
 * 综上，SqlNode 接口有多个实现类，每个实现类对应一个动态SQL节点，其中 SqlNode 扮演抽象构件角色，MixedSqlNode 扮演容器构件角色，其它一般是叶子构件角色
 */
public class MixedSqlNode implements SqlNode {

    /**
     * 组合模式，拥有一个SqlNode的List
     */
    private List<SqlNode> sqlNodeList;

    public MixedSqlNode(List<SqlNode> contents) {
        this.sqlNodeList = contents;
    }

    @Override
    public boolean apply(DynamicContext context) {
        //依次调用list里每个元素的apply
        for (SqlNode sqlNode : sqlNodeList) {

            //具体是否要拼接交给具体的类型去判断
            sqlNode.apply(context);
        }
        return true;
    }
}
