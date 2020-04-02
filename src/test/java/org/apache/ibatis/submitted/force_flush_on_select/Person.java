package org.apache.ibatis.submitted.force_flush_on_select;

import lombok.Data;

import java.io.Serializable;

@Data
public class Person implements Serializable {

    private Integer id;

    private String firstName;

    private String lastName;
}
