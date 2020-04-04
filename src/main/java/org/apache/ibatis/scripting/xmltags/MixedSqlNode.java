package org.apache.ibatis.scripting.xmltags;

import java.util.List;

/**
 * @author Clinton Begin
 */

/**
 * 混合SQL节点
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
            sqlNode.apply(context);
        }
        return true;
    }
}
