package org.apache.ibatis.submitted.cacheonetest;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.Reader;
import java.sql.Connection;

/**
 * 一级缓存测试
 *
 * @author zhucj
 * @since 20201217
 */
public class LevelOneCacheTest {

    private static SqlSessionFactory sqlSessionFactory;

    @Before
    public void setUp() throws Exception {
        // create an SqlSessionFactory
        Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/cacheonetest/mybatis-config.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        reader.close();

        // populate in-memory database
        SqlSession session = sqlSessionFactory.openSession();
        Connection conn = session.getConnection();
        reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/cacheonetest/init.sql");
        ScriptRunner runner = new ScriptRunner(conn);
        runner.setLogWriter(null);
        runner.runScript(reader);
        reader.close();
        session.close();
    }

    /**
     * 开启一级缓存，范围为会话级别
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
     * 开启一级缓存，范围为会话级别
     * 因为是同一个SqlSession，并且查询条件是一样的，通过日志看，只去数据库查询了一次
     */
    @Test
    public void findInDbOnceWhenSameParam() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            UseMapper mapper = sqlSession.getMapper(UseMapper.class);
            User user1 = mapper.getUser(1);
            Assert.assertEquals("User1", user1.getName());

            User user2 = mapper.getUser(1);
            Assert.assertEquals("User1", user2.getName());

            //并且他们是同一个引用
            Assert.assertEquals(user1, user2);
        } finally {
            sqlSession.close();
        }
    }

    /**
     * 开启一级缓存，范围为会话级别
     * 开启两个SqlSession，在sqlSession1中查询数据，使一级缓存生效，在sqlSession2中更新数据库，验证一级缓存只在数据库会话内部共享。
     * 会去数据库查询2次
     */
    @Test
    public void testFindInTwoSqlSession() {
        SqlSession sqlSession1 = sqlSessionFactory.openSession();
        try {
            UseMapper mapper = sqlSession1.getMapper(UseMapper.class);
            User user = mapper.getUser(1);
            Assert.assertEquals("User1", user.getName());
        } finally {
            sqlSession1.close();
        }

        SqlSession sqlSession2 = sqlSessionFactory.openSession();
        try {
            UseMapper mapper = sqlSession2.getMapper(UseMapper.class);
            User user = mapper.getUser(1);
            Assert.assertEquals("User1", user.getName());
        } finally {
            sqlSession2.close();
        }
    }

    /**
     * 开启一级缓存，范围为会话级别
     * 测试修改数据之后一级缓存会刷新
     */
    @Test
    public void testUpdateThenClearCache() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        UseMapper useMapper = sqlSession.getMapper(UseMapper.class);
        User user = useMapper.getUser(1);
        Assert.assertEquals("User1", user.getName());
        user.setName("updatedName");

        //修改姓名
        useMapper.updateUser(user);
        user = useMapper.getUser(1);
        Assert.assertEquals("updatedName", user.getName());
        sqlSession.close();
    }

    /**
     * 开启一级缓存，范围为会话级别
     * 一级缓存可能导致脏数据
     */
    @Test
    public void testUpdateAndFindInTwoSqlSessionDirtyData() {
        SqlSession sqlSessionA = sqlSessionFactory.openSession();
        UseMapper useMapperA = sqlSessionA.getMapper(UseMapper.class);
        User userA = useMapperA.getUser(1);
        Assert.assertEquals("User1", userA.getName());

        SqlSession sqlSessionB = sqlSessionFactory.openSession();
        UseMapper userMapperB = sqlSessionB.getMapper(UseMapper.class);
        User userB = userMapperB.getUser(1);
        Assert.assertEquals("User1", userB.getName());

        userA.setName("updatedName");

        //修改姓名
        useMapperA.updateUser(userA);
        userA = useMapperA.getUser(1);
        Assert.assertEquals("updatedName", userA.getName());

        sqlSessionA.close();

        //userMapper2再次查询
        userB = userMapperB.getUser(1);
        //sqlSessionB还是在使用它之前存进去的值。导致脏数据
        Assert.assertEquals("User1", userB.getName());
        sqlSessionB.close();
    }
}
