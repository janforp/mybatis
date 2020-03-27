package org.apache.ibatis.scripting.xmltags;

import java.util.List;

/**
 * @author Clinton Begin
 */

/**
 * <where>
 *       <choose>
 *         <when test="id != null">id = #{id}</when>
 *         <when test="author_id != null">AND author_id = #{author_id}</when>
 *         <otherwise>
 *           <if test="ids != null">
 *             AND id IN
 *             <foreach item="item_id" index="index" open="(" close=")" separator="," collection="ids">#{ids[${index}]}
 *             </foreach>
 *           </if>
 *           <trim prefix="AND">
 *             <include refid="byBlogId"/>
 *           </trim>
 *         </otherwise>
 *       </choose>
 *     </where>
 * choose SQL节点
 */
public class ChooseSqlNode implements SqlNode {

    private SqlNode defaultSqlNode;

    private List<SqlNode> ifSqlNodes;

    public ChooseSqlNode(List<SqlNode> ifSqlNodes, SqlNode defaultSqlNode) {
        this.ifSqlNodes = ifSqlNodes;
        this.defaultSqlNode = defaultSqlNode;
    }

    @Override
    public boolean apply(DynamicContext context) {
        //循环判断if，只要有1个为true了，返回true
        for (SqlNode sqlNode : ifSqlNodes) {
            if (sqlNode.apply(context)) {
                return true;
            }
        }
        //if都不为true，那就看otherwise
        if (defaultSqlNode != null) {
            defaultSqlNode.apply(context);
            return true;
        }
        //如果连otherwise都没有，返回false
        return false;
    }
}
