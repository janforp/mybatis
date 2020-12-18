package org.apache.ibatis.submitted.cachetwotest;

/**
 * @author zhucj
 * @since 20201217
 */
public interface TeacherMapper {

    Teacher getUser(Integer id);

    void insert(Teacher teacher);

    void update(Teacher teacher);
}
