package org.apache.ibatis.builder.xml.dynamic;

import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.domain.blog.Section;
import org.apache.ibatis.scripting.xmltags.ExpressionEvaluator;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("all")
public class ExpressionEvaluatorTest {

    private ExpressionEvaluator evaluator = new ExpressionEvaluator();

    @Test
    public void shouldCompareStringsReturnTrue() {
        boolean value = evaluator.evaluateBoolean(
                "username == 'cbegin'",
                new Author(1, "cbegin", "******", "cbegin@apache.org", "N/A", Section.NEWS));

        assertEquals(true, value);
    }

    @Test
    public void shouldCompareStringsReturnFalse() {
        boolean value = evaluator.evaluateBoolean(
                "username == 'norm'",
                new Author(1, "cbegin", "******", "cbegin@apache.org", "N/A", Section.NEWS));
        assertEquals(false, value);
    }

    @Test
    public void shouldReturnTrueIfNotNull() {
        boolean value = evaluator.evaluateBoolean(
                "username",
                new Author(1, "cbegin", "******", "cbegin@apache.org", "N/A", Section.NEWS));
        assertEquals(true, value);
    }

    @Test
    public void shouldReturnFalseIfNull() {
        boolean value = evaluator.evaluateBoolean(
                "password",
                new Author(1, "cbegin", null, "cbegin@apache.org", "N/A", Section.NEWS));
        assertEquals(false, value);
    }

    @Test
    public void shouldReturnTrueIfNotZero() {
        boolean value = evaluator.evaluateBoolean("id", new Author(1, "cbegin", null, "cbegin@apache.org", "N/A", Section.NEWS));
        assertEquals(true, value);
    }

    @Test
    public void shouldReturnFalseIfZero() {
        boolean value = evaluator.evaluateBoolean(
                "id",
                new Author(0, "cbegin", null, "cbegin@apache.org", "N/A", Section.NEWS));

        assertEquals(false, value);
    }

    @Test
    public void shouldIterateOverIterable() {
        final HashMap<String, String[]> parameterObject = new HashMap<String, String[]>() {
            {
                put("array", new String[] { "1", "2", "3" });
            }
        };
        final Iterable<?> iterable = evaluator.evaluateIterable("array", parameterObject);
        int i = 0;
        for (Object o : iterable) {
            assertEquals(String.valueOf(++i), o);
        }
    }
}
