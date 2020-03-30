package org.apache.ibatis.executor.loader;

import net.sf.cglib.proxy.Factory;
import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CglibProxyTest extends SerializableProxyTest {

    public CglibProxyTest() {
        proxyFactory = new CglibProxyFactory();
    }

    @Test
    public void shouldCreateAProxyForAPartiallyLoadedBean() throws Exception {
        ResultLoaderMap loader = new ResultLoaderMap();
        loader.addLoader("id", null, null);
        Object proxy = proxyFactory.createProxy(author, loader, new Configuration(), new DefaultObjectFactory(), new ArrayList<Class<?>>(), new ArrayList<Object>());
        Author author2 = (Author) deserialize(serialize((Serializable) proxy));
        assertTrue(author2 instanceof Factory);
    }

    @Test(expected = ExecutorException.class)
    public void shouldFailCallingAnUnloadedProperty() throws Exception {
        // yes, it must go in uppercase
        HashMap<String, ResultLoaderMap.LoadPair> unloadedProperties = new HashMap<String, ResultLoaderMap.LoadPair>();
        unloadedProperties.put("ID", null);
        Author author2 = (Author) ((CglibProxyFactory) proxyFactory).createDeserializationProxy(author, unloadedProperties, new DefaultObjectFactory(), new ArrayList<Class<?>>(), new ArrayList<Object>());
        author2.getId();
    }

    @Test
    public void shouldLetCallALoadedProperty() throws Exception {
        Author author2 = (Author) ((CglibProxyFactory) proxyFactory).createDeserializationProxy(author, new HashMap<String, ResultLoaderMap.LoadPair>(), new DefaultObjectFactory(), new ArrayList<Class<?>>(), new ArrayList<Object>());
        assertEquals(999, author2.getId());
    }

    @Test
    public void shouldSerizalizeADeserlizaliedProxy() throws Exception {
        Object proxy = ((CglibProxyFactory) proxyFactory).createDeserializationProxy(author, new HashMap<String, ResultLoaderMap.LoadPair>(), new DefaultObjectFactory(), new ArrayList<Class<?>>(), new ArrayList<Object>());
        Author author2 = (Author) deserialize(serialize((Serializable) proxy));
        assertEquals(author, author2);
        assertFalse(author.getClass().equals(author2.getClass()));
    }

}
