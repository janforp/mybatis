package org.apache.ibatis.submitted.associationtest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Car {

    private int id;

    private String type;

    private Engine engine;

    private Brakes brakes;
}
