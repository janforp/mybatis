package org.apache.ibatis.domain.blog;

import lombok.Data;

@Data
public class Comment {

    private int id;

    private Post post;

    private String name;

    private String comment;

    public String toString() {
        return "Comment: " + id + " : " + name + " : " + comment;
    }
}
