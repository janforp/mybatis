package org.apache.ibatis.binding;

import org.apache.ibatis.session.Configuration;
import org.junit.Test;

public class WrongMapperTest {

    @Test(expected = RuntimeException.class)
    public void shouldFailForBothOneAndMany() {
        Configuration configuration = new Configuration();
        configuration.addMapper(MapperWithOneAndMany.class);
    }
}