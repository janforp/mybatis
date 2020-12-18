package org.apache.ibatis.submitted.cachetwotest;

import lombok.Data;

import java.io.Serializable;

/**
 * Teacher
 *
 * @author zhucj
 * @since 20201224
 */
@Data
public class Teacher implements Serializable {

    private int id;

    private String name;
}