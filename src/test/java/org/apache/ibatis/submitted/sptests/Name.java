package org.apache.ibatis.submitted.sptests;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class Name {

    @Getter
    @Setter
    private Integer id;

    @Getter
    @Setter
    private String firstName;

    @Getter
    @Setter
    private String lastName;

    @Getter
    @Setter
    private List<Item> items;

    public List<Item> getItems() {
        return items;
    }
}
