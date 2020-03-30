package org.apache.ibatis.domain.blog.mappers;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.session.ResultHandler;

public interface AuthorMapperWithMultipleHandlers {

    /**
     * @see MapperMethod.MethodSignature#getUniqueParamIndex(java.lang.reflect.Method, java.lang.Class)
     */
    @Select("select id, username, password, email, bio, favourite_section from author where id = #{id}")
    void selectAuthor(int id, ResultHandler handler1, ResultHandler handler2);

}
