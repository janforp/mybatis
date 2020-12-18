package org.apache.ibatis.submitted.cacheonetest;

public interface UseMapper {

    User getUser(Integer id);

    void insertUser(User user);

    void updateUser(User user);
}
