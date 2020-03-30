package org.apache.ibatis.domain.blog.mappers;

import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.session.ResultHandler;

import java.util.List;

public interface CopyOfAuthorMapper {

    List selectAllAuthors();

    void selectAllAuthors(ResultHandler handler);

    Author selectAuthor(int id);

    void selectAuthor(int id, ResultHandler handler);

    void insertAuthor(Author author);

    int deleteAuthor(int id);

    int updateAuthor(Author author);
}



