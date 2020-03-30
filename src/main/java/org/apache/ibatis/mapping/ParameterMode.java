package org.apache.ibatis.mapping;

/**
 * 参数模式（给SP用）
 *
 * SP:stored procedure
 *
 * @author Clinton Begin
 * @see https://my.oschina.net/u/1991646/blog/731250
 *
 * <select id="getCount" resultType="hashmap" statementType="CALLABLE">
 * {call
 * ges_user_count(#{sex_id,mode=IN,jdbcType=INTEGER},#{result,mode=OUT,jdbcType=INTEGER})
 * }
 * </select>
 */
public enum ParameterMode {

    /**
     * 输入参数
     */
    IN,

    /**
     * 存储过程的out参数
     */
    OUT,

    /**
     * 输入输出参数
     */
    INOUT
}
