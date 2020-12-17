package org.apache.ibatis.submitted.cachetest;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Reader;
import java.sql.Connection;

public class LevelOneCacheTest {

    private static SqlSessionFactory sqlSessionFactory;

    @BeforeClass
    public static void setUp() throws Exception {
        // create an SqlSessionFactory
        Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/cachetest/mybatis-config.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        reader.close();

        // populate in-memory database
        SqlSession session = sqlSessionFactory.openSession();
        Connection conn = session.getConnection();
        reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/cachetest/init.sql");
        ScriptRunner runner = new ScriptRunner(conn);
        runner.setLogWriter(null);
        runner.runScript(reader);
        reader.close();
        session.close();
    }

    /**
     * 查询条件不一样,可以看到两条数据库日志
     */
    @Test
    public void findInDbEachWhenDifferentParam() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            UseMapper mapper = sqlSession.getMapper(UseMapper.class);
            User user = mapper.getUser(1);
            Assert.assertEquals("User1", user.getName());

            user = mapper.getUser(2);
            Assert.assertNull(user);
        } finally {
            sqlSession.close();
        }
    }

    /**
     * 因为是同一个SqlSession，并且查询条件是一样的，通过日志看，只去数据库查询了一次
     */
    @Test
    public void findInDbOnceWhenSameParam() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            UseMapper mapper = sqlSession.getMapper(UseMapper.class);
            User user = mapper.getUser(1);
            Assert.assertEquals("User1", user.getName());

            user = mapper.getUser(1);
            Assert.assertEquals("User1", user.getName());
        } finally {
            sqlSession.close();
        }
    }

    @Test
    public void shouldInsertAUser() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            UseMapper mapper = sqlSession.getMapper(UseMapper.class);
            User user = new User();
            user.setId(2);
            user.setName("User2");
            mapper.insertUser(user);
        } finally {
            sqlSession.close();
        }
    }
}
