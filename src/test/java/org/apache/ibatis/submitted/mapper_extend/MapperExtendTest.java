

package org.apache.ibatis.submitted.mapper_extend;

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

public class MapperExtendTest {

    private static SqlSessionFactory sqlSessionFactory;

    @BeforeClass
    public static void setUp() throws Exception {
        // create an SqlSessionFactory
        Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/mapper_extend/mybatis-config.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        reader.close();

        // populate in-memory database
        SqlSession session = sqlSessionFactory.openSession();
        Connection conn = session.getConnection();
        reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/mapper_extend/CreateDB.sql");
        ScriptRunner runner = new ScriptRunner(conn);
        runner.setLogWriter(null);
        runner.runScript(reader);
        reader.close();
        session.close();
    }

    @Test
    public void shouldGetAUserWithAnExtendedXMLMethod() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            ParentMapper mapper = sqlSession.getMapper(Mapper.class);
            User user = mapper.getUserXML();
            Assert.assertEquals("User1", user.getName());
        } finally {
            sqlSession.close();
        }
    }

    @Test
    public void shouldGetAUserWithAnExtendedAnnotatedMethod() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            ParentMapper mapper = sqlSession.getMapper(Mapper.class);
            User user = mapper.getUserAnnotated();
            Assert.assertEquals("User1", user.getName());
        } finally {
            sqlSession.close();
        }
    }

    @Test
    public void shouldGetAUserWithAnOverloadedXMLMethod() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            ParentMapper mapper = sqlSession.getMapper(MapperOverload.class);
            User user = mapper.getUserXML();
            Assert.assertEquals("User2", user.getName());
        } finally {
            sqlSession.close();
        }
    }

    @Test
    public void shouldGetAUserWithAnOverloadedAnnotatedMethod() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            ParentMapper mapper = sqlSession.getMapper(MapperOverload.class);
            User user = mapper.getUserAnnotated();
            Assert.assertEquals("User2", user.getName());
        } finally {
            sqlSession.close();
        }
    }

}
