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

        Teacher teacherFromDb = teacherMapper.getUser(id);
        Assert.assertEquals(name, teacherFromDb.getName());
    }

    @Test
    public void test() {
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
        TeacherMapper teacherMapper = sqlSession.getMapper(TeacherMapper.class);
        Teacher teacherFromDb = teacherMapper.getUser(1);
        Assert.assertEquals("邓俊辉", teacherFromDb.getName());

        Teacher teacher = teacherMapper.getUser(1);
        Assert.assertEquals("邓俊辉", teacher.getName());
    }
}