package org.apache.ibatis.submitted.force_flush;

public interface EmployeeMapper {

    Employee selectByNameFlush(String name);
}
