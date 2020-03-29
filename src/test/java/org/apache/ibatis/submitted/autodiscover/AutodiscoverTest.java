package org.apache.ibatis.submitted.autodiscover;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.submitted.autodiscover.mappers.DummyMapper;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Reader;
import java.math.BigInteger;

import static org.junit.Assert.assertTrue;

public class AutodiscoverTest {

    protected static SqlSessionFactory sqlSessionFactory;

    @BeforeClass
    public static void setup() throws Exception {
        Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/autodiscover/MapperConfig.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        reader.close();
    }

    @Test
    public void testTypeAlias() {
        TypeAliasRegistry typeAliasRegistry = sqlSessionFactory.getConfiguration().getTypeAliasRegistry();
        typeAliasRegistry.resolveAlias("testAlias");
    }

    @Test
    public void testTypeHandler() {
        TypeHandlerRegistry typeHandlerRegistry = sqlSessionFactory.getConfiguration().getTypeHandlerRegistry();
        assertTrue(typeHandlerRegistry.hasTypeHandler(BigInteger.class));
    }

    @Test
    public void testMapper() {
        assertTrue(sqlSessionFactory.getConfiguration().hasMapper(DummyMapper.class));
    }
}
