package org.apache.ibatis.domain.blog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Blog {

    private int id;

    private String title;

    private Author author;

    private List<Post> posts;

    public String toString() {
        return "Blog: " + id + " : " + title + " (" + author + ")";
    }
}
