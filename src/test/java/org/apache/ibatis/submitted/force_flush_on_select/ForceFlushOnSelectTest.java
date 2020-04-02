package org.apache.ibatis.submitted.force_flush_on_select;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * 查询的时候刷新缓存
 */
public class ForceFlushOnSelectTest {

    private static SqlSessionFactory sqlSessionFactory;

    /**
     * 刷新二级缓存
     */
    @Test
    public void testShouldFlushLocalSessionCacheOnQuery() throws SQLException {
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);
        try {
            PersonMapper personMapper = sqlSession.getMapper(PersonMapper.class);
            //第一次查询
            Person oldPerson = personMapper.selectByIdFlush(1);
            //把id=1的人员的firstName修改为Simone
            updateDatabase(sqlSession.getConnection());
            //修改之后查询
            Person updatedPerson = personMapper.selectByIdFlush(1);
            assertEquals("Simone", updatedPerson.getFirstName());
            sqlSession.commit();
        } finally {
            sqlSession.close();
        }
    }

    /**
     * 不刷新二级缓存
     */
    @Test
    public void testShouldNotFlushLocalSessionCacheOnQuery() throws SQLException {
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);
        try {
            PersonMapper personMapper = sqlSession.getMapper(PersonMapper.class);
            //(1, 'John', 'Smith');
            Person oldPerson = personMapper.selectByIdNoFlush(1);

            //把id=1的人员的firstName修改为Simone
            updateDatabase(sqlSession.getConnection());

            //虽然上面直接去数据库修改了数据，但是下面的查询会在二级缓存中拿到上一次查询的结果，不会去数据库查询
            Person updatedPerson = personMapper.selectByIdNoFlush(1);
            assertEquals("John", updatedPerson.getFirstName());
            sqlSession.commit();
        } finally {
            sqlSession.close();
        }
    }

    @Test
    public void testShouldFlushLocalSessionCacheOnQueryForList() throws SQLException {
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);
        try {
            PersonMapper personMapper = sqlSession.getMapper(PersonMapper.class);
            //查询之前会刷新
            //(1, 'John', 'Smith');
            List<Person> people = personMapper.selectAllFlush();

            //把id=1的人员的firstName修改为Simone
            updateDatabase(sqlSession.getConnection());

            //查询之前会刷新，所以这次会去数据库查询
            people = personMapper.selectAllFlush();

            assertEquals("Simone", people.get(0).getFirstName());
            sqlSession.commit();
        } finally {
            sqlSession.close();
        }
    }

    @Test
    public void testShouldNotFlushLocalSessionCacheOnQueryForList() throws SQLException {
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);
        try {
            PersonMapper personMapper = sqlSession.getMapper(PersonMapper.class);
            List<Person> people = personMapper.selectAllNoFlush();
            updateDatabase(sqlSession.getConnection());
            people = personMapper.selectAllNoFlush();
            assertEquals("John", people.get(0).getFirstName());
            sqlSession.commit();
        } finally {
            sqlSession.close();
        }
    }

    /**
     * 不管怎么样修改的时候都会刷新二级缓存
     */
    @Test
    public void testUpdateShouldFlushLocalCache() throws SQLException {
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);
        try {
            PersonMapper personMapper = sqlSession.getMapper(PersonMapper.class);
            Person person = personMapper.selectByIdNoFlush(1);
            person.setLastName("Perez"); //it is ignored in update
            personMapper.update(person);
            Person updatedPerson = personMapper.selectByIdNoFlush(1);
            assertEquals("Smith", updatedPerson.getLastName());
            assertNotSame(person, updatedPerson);
            sqlSession.commit();
        } finally {
            sqlSession.close();
        }
    }

    @Test
    public void testSelectShouldFlushLocalCacheIfFlushLocalCacheAtferEachStatementIsTrue() throws SQLException {
        Configuration configuration = sqlSessionFactory.getConfiguration();
        LocalCacheScope localCacheScope = LocalCacheScope.STATEMENT;
        configuration.setLocalCacheScope(localCacheScope);
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);
        try {
            PersonMapper personMapper = sqlSession.getMapper(PersonMapper.class);

            //因为现在的一级缓存的范围是 LocalCacheScope.STATEMENT 在返回结果的之前会刷新一级缓存
            List<Person> people = personMapper.selectAllNoFlush();
            updateDatabase(sqlSession.getConnection());
            List<Person> newPeople = personMapper.selectAllFlush();
            assertEquals("Simone", newPeople.get(0).getFirstName());
            sqlSession.commit();
        } finally {
            sqlSession.close();
        }
    }

    /**
     * 初始化的数据：
     * INSERT INTO person (id, firstName, lastName)
     * VALUES (1, 'John', 'Smith');
     *
     * INSERT INTO person (id, firstName, lastName)
     * VALUES (2, 'Christian', 'Poitras');
     *
     * INSERT INTO person (id, firstName, lastName)
     * VALUES (3, 'Clinton', 'Begin');
     *
     * 执行sql脚本
     * 初始化 SqlSessionFactory
     */
    @Before
    public void initDatabase() throws Exception {
        Connection conn = null;
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            conn = DriverManager.getConnection("jdbc:hsqldb:mem:force_flush_on_select", "sa", "");

            Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/force_flush_on_select/CreateDB.sql");
            ScriptRunner runner = new ScriptRunner(conn);
            runner.setLogWriter(null);
            runner.setErrorLogWriter(null);
            runner.runScript(reader);
            conn.commit();
            reader.close();

            reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/force_flush_on_select/ibatisConfig.xml");
            SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();
            sqlSessionFactory = sqlSessionFactoryBuilder.build(reader);
            reader.close();
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    /**
     * 初始化的数据：
     * INSERT INTO person (id, firstName, lastName)
     * VALUES (1, 'John', 'Smith');
     *
     * INSERT INTO person (id, firstName, lastName)
     * VALUES (2, 'Christian', 'Poitras');
     *
     * INSERT INTO person (id, firstName, lastName)
     * VALUES (3, 'Clinton', 'Begin');
     */
    private void updateDatabase(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("UPDATE person SET firstName = 'Simone' WHERE id = 1");
        stmt.close();
    }
}
