package org.apache.ibatis.scripting.xmltags;

/**
 * @author Clinton Begin
 */

/**
 * SQL节点（choose|foreach|if|）中的sql语句，包括参数占位符
 */
public interface SqlNode {

    boolean apply(DynamicContext context);
}
