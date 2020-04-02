package org.apache.ibatis.submitted.force_flush;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.ExecutorType;
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

import static org.junit.Assert.assertEquals;

/**
 * 查询的时候刷新缓存
 */
public class MyEmployeeTest {

    private static SqlSessionFactory sqlSessionFactory;

    /**
     * 初始化的数据：
     * 执行sql脚本
     * 初始化 SqlSessionFactory
     */
    @Before
    public void initDatabase() throws Exception {
        Connection conn = null;
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            conn = DriverManager.getConnection("jdbc:hsqldb:mem:force_flush_on_select", "sa", "");

            //org/apache/ibatis/submitted/force_flush_on_select/CreateDB.sql
            Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/force_flush/employeeDmlDdl.sql");
            ScriptRunner runner = new ScriptRunner(conn);
            runner.setLogWriter(null);
            runner.setErrorLogWriter(null);
            runner.runScript(reader);
            conn.commit();
            reader.close();

            reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/force_flush/EmployeeMybatisConfig.xml");
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
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
        stmt.executeUpdate("UPDATE mt_employee SET name = '呵呵' WHERE id = 1");
        stmt.close();
    }

    /**
     * 刷新二级缓存
     */
    @Test
    public void testShouldFlushLocalSessionCacheOnQuery() throws SQLException {
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);
        try {
            EmployeeMapper personMapper = sqlSession.getMapper(EmployeeMapper.class);
            //第一次查询
            Employee oldPerson = personMapper.selectByIdFlush(1);
            //把id=1的人员的firstName修改为Simone
            updateDatabase(sqlSession.getConnection());
            //修改之后查询
            Employee updatedPerson = personMapper.selectByIdFlush(1);
            assertEquals("呵呵", updatedPerson.getName());
            sqlSession.commit();
        } finally {
            sqlSession.close();
        }
    }

    //    /**
    //     * 不刷新二级缓存
    //     */
    //    @Test
    //    public void testShouldNotFlushLocalSessionCacheOnQuery() throws SQLException {
    //        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);
    //        try {
    //            EmployeeMapper personMapper = sqlSession.getMapper(EmployeeMapper.class);
    //            //(1, 'John', 'Smith');
    //            Employee oldPerson = personMapper.selectByIdNoFlush(1);
    //
    //            //把id=1的人员的firstName修改为Simone
    //            updateDatabase(sqlSession.getConnection());
    //
    //            //虽然上面直接去数据库修改了数据，但是下面的查询会在二级缓存中拿到上一次查询的结果，不会去数据库查询
    //            Employee updatedPerson = personMapper.selectByIdNoFlush(1);
    //            assertEquals("John", updatedPerson.getFirstName());
    //            sqlSession.commit();
    //        } finally {
    //            sqlSession.close();
    //        }
    //    }
    //
    //    @Test
    //    public void testShouldFlushLocalSessionCacheOnQueryForList() throws SQLException {
    //        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);
    //        try {
    //            EmployeeMapper personMapper = sqlSession.getMapper(EmployeeMapper.class);
    //            //查询之前会刷新
    //            //(1, 'John', 'Smith');
    //            List<Employee> people = personMapper.selectAllFlush();
    //
    //            //把id=1的人员的firstName修改为Simone
    //            updateDatabase(sqlSession.getConnection());
    //
    //            //查询之前会刷新，所以这次会去数据库查询
    //            people = personMapper.selectAllFlush();
    //
    //            assertEquals("Simone", people.get(0).getFirstName());
    //            sqlSession.commit();
    //        } finally {
    //            sqlSession.close();
    //        }
    //    }
    //
    //    @Test
    //    public void testShouldNotFlushLocalSessionCacheOnQueryForList() throws SQLException {
    //        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);
    //        try {
    //            EmployeeMapper personMapper = sqlSession.getMapper(EmployeeMapper.class);
    //            List<Employee> people = personMapper.selectAllNoFlush();
    //            updateDatabase(sqlSession.getConnection());
    //            people = personMapper.selectAllNoFlush();
    //            assertEquals("John", people.get(0).getFirstName());
    //            sqlSession.commit();
    //        } finally {
    //            sqlSession.close();
    //        }
    //    }
    //
    //    /**
    //     * 不管怎么样修改的时候都会刷新二级缓存
    //     */
    //    @Test
    //    public void testUpdateShouldFlushLocalCache() throws SQLException {
    //        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);
    //        try {
    //            EmployeeMapper personMapper = sqlSession.getMapper(EmployeeMapper.class);
    //            Employee person = personMapper.selectByIdNoFlush(1);
    //            person.setLastName("Perez"); //it is ignored in update
    //            personMapper.update(person);
    //            Employee updatedPerson = personMapper.selectByIdNoFlush(1);
    //            assertEquals("Smith", updatedPerson.getLastName());
    //            assertNotSame(person, updatedPerson);
    //            sqlSession.commit();
    //        } finally {
    //            sqlSession.close();
    //        }
    //    }
    //
    //    @Test
    //    public void testSelectShouldFlushLocalCacheIfFlushLocalCacheAtferEachStatementIsTrue() throws SQLException {
    //        Configuration configuration = sqlSessionFactory.getConfiguration();
    //        LocalCacheScope localCacheScope = LocalCacheScope.STATEMENT;
    //        configuration.setLocalCacheScope(localCacheScope);
    //        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.SIMPLE);
    //        try {
    //            EmployeeMapper personMapper = sqlSession.getMapper(EmployeeMapper.class);
    //
    //            //因为现在的一级缓存的范围是 LocalCacheScope.STATEMENT 在返回结果的之前会刷新一级缓存
    //            List<Employee> people = personMapper.selectAllNoFlush();
    //            updateDatabase(sqlSession.getConnection());
    //            List<Employee> newPeople = personMapper.selectAllFlush();
    //            assertEquals("Simone", newPeople.get(0).getFirstName());
    //            sqlSession.commit();
    //        } finally {
    //            sqlSession.close();
    //        }
    //    }
}
