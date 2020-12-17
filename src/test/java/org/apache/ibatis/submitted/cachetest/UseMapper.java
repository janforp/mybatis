package org.apache.ibatis.submitted.cachetest;

public interface UseMapper {

    User getUser(Integer id);

    void insertUser(User user);

}
