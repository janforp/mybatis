package org.apache.ibatis.submitted.cachetwotest;

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
 * 二级缓存测试
 *
 * @author zhucj
 * @since 20201217
 */
public class LevelTwoCacheTest {

    private static SqlSessionFactory sqlSessionFactory;

    @Before
    public void setUp() throws Exception {
        // create an SqlSessionFactory
        Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/cachetwotest/mybatis-config.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        reader.close();

        // populate in-memory database
        SqlSession session = sqlSessionFactory.openSession();
        Connection conn = session.getConnection();
        reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/cachetwotest/initTeacher.sql");
        ScriptRunner runner = new ScriptRunner(conn);
        runner.setLogWriter(null);
        runner.runScript(reader);
        reader.close();
        session.close();
    }

    private void insertATeacher(int id, String name) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setName(name);
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
        TeacherMapper teacherMapper = sqlSession.getMapper(TeacherMapper.class);
        teacherMapper.insert(teacher);

        Teacher teacherFromDb = teacherMapper.get(id);
        Assert.assertEquals(name, teacherFromDb.getName());
    }

    @Test
    public void test() {
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
        TeacherMapper teacherMapper = sqlSession.getMapper(TeacherMapper.class);
        Teacher teacherFromDb = teacherMapper.get(1);
        Assert.assertEquals("邓俊辉", teacherFromDb.getName());

        Teacher teacher = teacherMapper.get(1);
        Assert.assertEquals("邓俊辉", teacher.getName());
    }

    /**
     * <setting name="localCacheScope" value="STATEMENT"/> 一级缓存范围为 STATEMENT
     * <cache/> 开启了二级缓存
     *
     * sqlSessionA 查询之后并么有进行提交
     * sqlSessionB 无法使用耳机缓存
     * 可以看到 sqlSessionA,sqlSessionB都去数据库进行了查询
     */
    @Test
    public void testTwoSqlSessionAllNotCommit() {
        SqlSession sqlSessionA = sqlSessionFactory.openSession(false);
        SqlSession sqlSessionB = sqlSessionFactory.openSession(false);

        TeacherMapper teacherMapperA = sqlSessionA.getMapper(TeacherMapper.class);
        TeacherMapper teacherMapperB = sqlSessionB.getMapper(TeacherMapper.class);

        Teacher teacherA = teacherMapperA.get(1);
        Teacher teacherB = teacherMapperB.get(1);
        Assert.assertEquals("邓俊辉", teacherA.getName());
        Assert.assertEquals("邓俊辉", teacherB.getName());
    }

    /**
     * <setting name="localCacheScope" value="STATEMENT"/> 一级缓存范围为 STATEMENT
     * <cache/> 开启了二级缓存
     *
     * sqlSessionA 查询之后进行了提交
     * sqlSessionB 使用耳机缓存
     * 可以看到
     * 1.sqlSessionA 去数据库查询了
     * 2.sqlSessionB 妹有去数据库查询
     */
    @Test
    public void testTwoSqlSessionFirstCommit() {
        SqlSession sqlSessionA = sqlSessionFactory.openSession(true);
        SqlSession sqlSessionB = sqlSessionFactory.openSession(true);

        TeacherMapper teacherMapperA = sqlSessionA.getMapper(TeacherMapper.class);
        TeacherMapper teacherMapperB = sqlSessionB.getMapper(TeacherMapper.class);

        Teacher teacherA = teacherMapperA.get(1);
        //提交事务，使二级缓存中有数据
        sqlSessionA.commit();
        Teacher teacherB = teacherMapperB.get(1);
        Assert.assertEquals("邓俊辉", teacherA.getName());
        Assert.assertEquals("邓俊辉", teacherB.getName());
        //不同的对象,因为使用了 org.apache.ibatis.cache.decorators.SynchronizedCache 反序列化之后的对象不同的
        Assert.assertNotSame(teacherA, teacherB);
    }

    /**
     * <setting name="localCacheScope" value="STATEMENT"/> 一级缓存范围为 STATEMENT
     * <cache/> 开启了二级缓存
     *
     * sqlSessionA 查询之后进行了提交
     * sqlSessionB 使用二级缓存
     * sqlSessionC 修改了数据，并且进行了提交
     * 可以看到
     * 1.sqlSessionA 去数据库查询了
     * 2.sqlSessionB 第一次没有到db查询
     * 3.sqlSessionC 修改修改 sqlSessionB 第二次查询去了db
     */
    @Test
    public void testThreeSqlSessionFirstCommitAndUpdate() {
        SqlSession sqlSessionA = sqlSessionFactory.openSession();
        SqlSession sqlSessionB = sqlSessionFactory.openSession();
        SqlSession sqlSessionC = sqlSessionFactory.openSession();

        TeacherMapper teacherMapperA = sqlSessionA.getMapper(TeacherMapper.class);
        TeacherMapper teacherMapperB = sqlSessionB.getMapper(TeacherMapper.class);
        TeacherMapper teacherMapperC = sqlSessionC.getMapper(TeacherMapper.class);

        Teacher teacherA = teacherMapperA.get(1);
        //提交事务，使二级缓存中有数据
        sqlSessionA.commit();
        Teacher teacherB = teacherMapperB.get(1);
        Assert.assertEquals("邓俊辉", teacherA.getName());
        Assert.assertEquals("邓俊辉", teacherB.getName());
        //不同的对象,因为使用了 org.apache.ibatis.cache.decorators.SynchronizedCache 反序列化之后的对象不同的
        Assert.assertNotSame(teacherA, teacherB);

        Teacher teacherC = new Teacher();
        teacherC.setId(1);
        teacherC.setName("哈哈哈哈");
        teacherMapperC.update(teacherC);
        //提交并刷新缓存
        sqlSessionC.commit();

        Teacher teacherBB = teacherMapperB.get(1);
        Assert.assertEquals("哈哈哈哈", teacherBB.getName());
    }
}