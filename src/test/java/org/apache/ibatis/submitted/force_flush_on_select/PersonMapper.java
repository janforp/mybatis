package org.apache.ibatis.submitted.force_flush_on_select;

import java.util.List;

public interface PersonMapper {

    Person selectByIdFlush(int id);

    Person selectByIdNoFlush(int id);

    List<Person> selectAllFlush();

    List<Person> selectAllNoFlush();

    int update(Person p);
}
