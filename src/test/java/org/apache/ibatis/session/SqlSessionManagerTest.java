package org.apache.ibatis.session;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.domain.blog.mappers.AuthorMapper;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.io.Resources;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Reader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SqlSessionManagerTest extends BaseDataTest {

    private static SqlSessionManager sqlSessionManager;

    @BeforeClass
    public static void setup() throws Exception {
        createBlogDataSource();
        final String resource = "org/apache/ibatis/builder/MapperConfig.xml";
        final Reader reader = Resources.getResourceAsReader(resource);
        sqlSessionManager = SqlSessionManager.newInstance(reader);
    }

    @Test
    public void shouldThrowExceptionIfMappedStatementDoesNotExistAndSqlSessionIsOpen() throws Exception {
        try {
            sqlSessionManager.startManagedSession();
            sqlSessionManager.selectList("ThisStatementDoesNotExist");
            fail("Expected exception to be thrown due to statement that does not exist.");
        } catch (PersistenceException e) {
            assertTrue(e.getMessage().contains("does not contain value for ThisStatementDoesNotExist"));
        } finally {
            sqlSessionManager.close();
        }
    }

    @Test
    public void shouldCommitInsertedAuthor() throws Exception {
        try {
            sqlSessionManager.startManagedSession();
            AuthorMapper mapper = sqlSessionManager.getMapper(AuthorMapper.class);
            Author expected = new Author(500, "cbegin", "******", "cbegin@somewhere.com", "Something...", null);
            mapper.insertAuthor(expected);
            sqlSessionManager.commit();
            Author actual = mapper.selectAuthor(500);
            assertNotNull(actual);
        } finally {
            sqlSessionManager.close();
        }
    }

    @Test
    public void shouldRollbackInsertedAuthor() throws Exception {
        try {
            sqlSessionManager.startManagedSession();
            AuthorMapper mapper = sqlSessionManager.getMapper(AuthorMapper.class);
            Author expected = new Author(501, "lmeadors", "******", "lmeadors@somewhere.com", "Something...", null);
            mapper.insertAuthor(expected);
            sqlSessionManager.rollback();
            Author actual = mapper.selectAuthor(501);
            assertNull(actual);
        } finally {
            sqlSessionManager.close();
        }
    }

    @Test
    public void shouldImplicitlyRollbackInsertedAuthor() throws Exception {
        sqlSessionManager.startManagedSession();
        AuthorMapper mapper = sqlSessionManager.getMapper(AuthorMapper.class);
        Author expected = new Author(502, "emacarron", "******", "emacarron@somewhere.com", "Something...", null);
        mapper.insertAuthor(expected);
        sqlSessionManager.close();
        Author actual = mapper.selectAuthor(502);
        assertNull(actual);
    }

}
