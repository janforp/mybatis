package org.apache.ibatis.parsing;

import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

/**
 * 类说明：
 *
 * @author zhucj
 * @since 20200423
 */
public class PropertyParserTest {

    @Test
    public void parse() {
        Properties variables = new Properties();
        variables.setProperty("name", "张三");
        variables.setProperty("gender", "2");
        String name = PropertyParser.parse("${name}", variables);
        Assert.assertEquals("张三", name);
    }
}
