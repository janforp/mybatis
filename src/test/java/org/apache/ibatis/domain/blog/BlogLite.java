package org.apache.ibatis.domain.blog;

import lombok.Data;

import java.util.List;

@Data
public class BlogLite {

    private int id;

    private List<PostLite> posts;
}
