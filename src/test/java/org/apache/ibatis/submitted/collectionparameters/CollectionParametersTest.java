

package org.apache.ibatis.submitted.collectionparameters;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionParametersTest {

    private static SqlSessionFactory sqlSessionFactory;

    @BeforeClass
    public static void setUp() throws Exception {
        // create an SqlSessionFactory
        Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/collectionparameters/mybatis-config.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        reader.close();

        // populate in-memory database
        SqlSession session = sqlSessionFactory.openSession();
        Connection conn = session.getConnection();
        reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/collectionparameters/CreateDB.sql");
        ScriptRunner runner = new ScriptRunner(conn);
        runner.setLogWriter(null);
        runner.runScript(reader);
        reader.close();
        session.close();
    }

    @Test
    public void shouldGetTwoUsersPassingAList() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            Mapper mapper = sqlSession.getMapper(Mapper.class);
            ArrayList<Integer> list = new ArrayList<Integer>();
            list.add(1);
            list.add(2);
            List<User> users = mapper.getUsersFromList(list);
            Assert.assertEquals(2, users.size());
        } finally {
            sqlSession.close();
        }
    }

    @Test
    public void shouldGetTwoUsersPassingAnArray() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            Mapper mapper = sqlSession.getMapper(Mapper.class);
            Integer[] list = new Integer[2];
            list[0] = 1;
            list[1] = 2;
            List<User> users = mapper.getUsersFromArray(list);
            Assert.assertEquals(2, users.size());
        } finally {
            sqlSession.close();
        }
    }

    @Test
    public void shouldGetTwoUsersPassingACollection() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            Mapper mapper = sqlSession.getMapper(Mapper.class);
            Set<Integer> list = new HashSet<Integer>();
            list.add(1);
            list.add(2);
            List<User> users = mapper.getUsersFromCollection(list);
            Assert.assertEquals(2, users.size());
        } finally {
            sqlSession.close();
        }
    }

}
