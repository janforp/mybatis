package org.apache.ibatis.submitted.force_flush;

import java.util.List;

public interface EmployeeMapper {

    Employee selectByIdFlush(int id);

    Employee selectByIdNoFlush(int id);

    List<Employee> selectAllFlush();

    List<Employee> selectAllNoFlush();

    int update(Employee p);
}
