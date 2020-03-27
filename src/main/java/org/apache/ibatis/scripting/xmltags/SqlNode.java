package org.apache.ibatis.scripting.xmltags;

/**
 * @author Clinton Begin
 */

/**
 * SQL节点（choose|foreach|if|）
 */
public interface SqlNode {

    boolean apply(DynamicContext context);
}
