package org.apache.ibatis.domain.blog;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Tag {

    private int id;

    private String name;

    public String toString() {
        return "Tag: " + id + " : " + name;
    }
}
