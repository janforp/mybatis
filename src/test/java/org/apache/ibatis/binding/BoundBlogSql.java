package org.apache.ibatis.binding;

import org.apache.ibatis.jdbc.SQL;

public class BoundBlogSql {

    public String selectBlogsSql() {
        return new SQL() {
            {
                SELECT("*");
                FROM("BLOG");
            }
        }.toString();
    }
}
