package org.apache.ibatis.submitted.force_flush;

import lombok.Data;

import java.io.Serializable;

@Data
public class Employee implements Serializable {

    private Integer id;

    private String name;

    private String idNo;
}
