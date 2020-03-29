package org.apache.ibatis.builder;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class ParameterExpressionTest {

    @Test
    public void simpleProperty() {
        Map<String, String> result = new ParameterExpression("id");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("id", result.get("property"));
    }

    public void propertyWithSpacesInside() {
        Map<String, String> result = new ParameterExpression(" with spaces ");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("with spaces", result.get("property"));
    }

    @Test
    public void simplePropertyWithOldStyleJdbcType() {
        Map<String, String> result = new ParameterExpression("id:VARCHAR");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("id", result.get("property"));
        Assert.assertEquals("VARCHAR", result.get("jdbcType"));
    }

    @Test
    public void oldStyleJdbcTypeWithExtraWhitespaces() {
        Map<String, String> result = new ParameterExpression(" id :  VARCHAR ");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("id", result.get("property"));
        Assert.assertEquals("VARCHAR", result.get("jdbcType"));
    }

    @Test
    public void expressionWithOldStyleJdbcType() {
        Map<String, String> result = new ParameterExpression("(id.toString()):VARCHAR");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("id.toString()", result.get("expression"));
        Assert.assertEquals("VARCHAR", result.get("jdbcType"));
    }

    @Test
    public void simplePropertyWithOneAttribute() {
        Map<String, String> result = new ParameterExpression("id,name=value");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("id", result.get("property"));
        Assert.assertEquals("value", result.get("name"));
    }

    @Test
    public void expressionWithOneAttribute() {
        Map<String, String> result = new ParameterExpression("(id.toString()),name=value");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("id.toString()", result.get("expression"));
        Assert.assertEquals("value", result.get("name"));
    }

    @Test
    public void simplePropertyWithManyAttributes() {
        Map<String, String> result = new ParameterExpression("id, attr1=val1, attr2=val2, attr3=val3");
        Assert.assertEquals(4, result.size());
        Assert.assertEquals("id", result.get("property"));
        Assert.assertEquals("val1", result.get("attr1"));
        Assert.assertEquals("val2", result.get("attr2"));
        Assert.assertEquals("val3", result.get("attr3"));
    }

    @Test
    public void expressionWithManyAttributes() {
        Map<String, String> result = new ParameterExpression("(id.toString()), attr1=val1, attr2=val2, attr3=val3");
        Assert.assertEquals(4, result.size());
        Assert.assertEquals("id.toString()", result.get("expression"));
        Assert.assertEquals("val1", result.get("attr1"));
        Assert.assertEquals("val2", result.get("attr2"));
        Assert.assertEquals("val3", result.get("attr3"));
    }

    @Test
    public void simplePropertyWithOldStyleJdbcTypeAndAttributes() {
        Map<String, String> result = new ParameterExpression("id:VARCHAR, attr1=val1, attr2=val2");
        Assert.assertEquals(4, result.size());
        Assert.assertEquals("id", result.get("property"));
        Assert.assertEquals("VARCHAR", result.get("jdbcType"));
        Assert.assertEquals("val1", result.get("attr1"));
        Assert.assertEquals("val2", result.get("attr2"));
    }

    @Test
    public void simplePropertyWithSpaceAndManyAttributes() {
        Map<String, String> result = new ParameterExpression("user name, attr1=val1, attr2=val2, attr3=val3");
        Assert.assertEquals(4, result.size());
        Assert.assertEquals("user name", result.get("property"));
        Assert.assertEquals("val1", result.get("attr1"));
        Assert.assertEquals("val2", result.get("attr2"));
        Assert.assertEquals("val3", result.get("attr3"));
    }

    @Test
    public void shouldIgnoreLeadingAndTrailingSpaces() {
        Map<String, String> result = new ParameterExpression(" id , jdbcType =  VARCHAR,  attr1 = val1 ,  attr2 = val2 ");
        Assert.assertEquals(4, result.size());
        Assert.assertEquals("id", result.get("property"));
        Assert.assertEquals("VARCHAR", result.get("jdbcType"));
        Assert.assertEquals("val1", result.get("attr1"));
        Assert.assertEquals("val2", result.get("attr2"));
    }
}
