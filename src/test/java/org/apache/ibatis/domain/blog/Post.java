package org.apache.ibatis.domain.blog;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class Post {

    private int id;

    private Author author;

    private Blog blog;

    private Date createdOn;

    private Section section;

    private String subject;

    private String body;

    private List<Comment> comments;

    private List<Tag> tags;

    public String toString() {
        return "Post: " + id + " : " + subject + " : " + body + " : " + section + " : " + createdOn + " (" + author + ") (" + blog + ")";
    }
}
