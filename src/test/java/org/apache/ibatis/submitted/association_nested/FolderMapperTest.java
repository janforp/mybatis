package org.apache.ibatis.submitted.association_nested;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

/**
 * @author Loïc Guerrin <guerrin@fullsix.com>
 */
public class FolderMapperTest {

    @Test
    public void testFindWithChildren() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:hsqldb:mem:association_nested", "SA", "");
        Statement statement = connection.createStatement();

        //DDL
        statement.execute("create table folder (id int, name varchar(100), parent_id int)");

        //DML
        statement.execute("insert into folder (id, name) values(1, 'Root')");
        statement.execute("insert into folder values(2, 'Folder 1', 1)");
        statement.execute("insert into folder values(3, 'Folder 2', 1)");
        statement.execute("insert into folder values(4, 'Folder 2_1', 3)");
        statement.execute("insert into folder values(5, 'Folder 2_2', 3)");

        /**
         * Root/
         *    Folder 1/
         *    Folder 2/
         *      Folder 2_1
         *      Folder 2_2
         */

        String resource = "org/apache/ibatis/submitted/association_nested/mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);

        SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();
        SqlSessionFactory sqlSessionFactory = sqlSessionFactoryBuilder.build(inputStream);

        //用户使用的接口
        SqlSession session = sqlSessionFactory.openSession();

        //获取映射接口
        FolderMapper postMapper = session.getMapper(FolderMapper.class);

        List<FolderFlatTree> flatTreeList = postMapper.findWithSubFolders("Root");

        Assert.assertEquals(3, flatTreeList.size());

        session.close();
    }
}
