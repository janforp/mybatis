package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.session.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * <set>
 * <if test="username != null">username=#{username},</if>
 * <if test="password != null">password=#{password},</if>
 * <if test="email != null">email=#{email},</if>
 * <if test="bio != null">bio=#{bio}</if>
 * </set>
 *
 * @author Clinton Begin
 */
public class SetSqlNode extends TrimSqlNode {

    private static List<String> suffixList = Arrays.asList(",");

    public SetSqlNode(Configuration configuration, SqlNode contents) {
        super(configuration, contents, "SET", null, null, suffixList);
    }
}
