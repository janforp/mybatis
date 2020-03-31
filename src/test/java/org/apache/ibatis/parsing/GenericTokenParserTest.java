package org.apache.ibatis.parsing;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class GenericTokenParserTest {

    @Test
    public void shouldDemonstrateGenericTokenReplacementOne() {

        HashMap<String, String> hashMap = new HashMap<String, String>() {
            {
                put("username", "Kobe");
            }
        };
        //标志处理器实现
        VariableTokenHandler tokenHandler = new VariableTokenHandler(hashMap);
        GenericTokenParser parser = new GenericTokenParser("${", "}", tokenHandler);

        String parsedStr = parser.parse("AND username = ${username};");
        assertEquals("AND username = Kobe;", parsedStr);
    }

    @Test
    public void shouldDemonstrateGenericTokenReplacement() {

        HashMap<String, String> hashMap = new HashMap<String, String>() {
            {
                put("first_name", "James");
                put("initial", "T");
                put("last_name", "Kirk");
                put("", "");
            }
        };
        //标志处理器实现
        VariableTokenHandler tokenHandler = new VariableTokenHandler(hashMap);
        GenericTokenParser parser = new GenericTokenParser("${", "}", tokenHandler);

        String parsedStr = parser.parse("${first_name} ${initial} ${last_name} reporting.");
        assertEquals("James T Kirk reporting.", parsedStr);
        assertEquals("Hello captain James T Kirk", parser.parse("Hello captain ${first_name} ${initial} ${last_name}"));
        assertEquals("James T Kirk", parser.parse("${first_name} ${initial} ${last_name}"));
        assertEquals("JamesTKirk", parser.parse("${first_name}${initial}${last_name}"));
        assertEquals("{}JamesTKirk", parser.parse("{}${first_name}${initial}${last_name}"));
        assertEquals("}JamesTKirk", parser.parse("}${first_name}${initial}${last_name}"));

        assertEquals("}James{{T}}Kirk", parser.parse("}${first_name}{{${initial}}}${last_name}"));
        assertEquals("}James}T{Kirk", parser.parse("}${first_name}}${initial}{${last_name}"));
        assertEquals("}James}T{Kirk", parser.parse("}${first_name}}${initial}{${last_name}"));
        assertEquals("}James}T{Kirk{{}}", parser.parse("}${first_name}}${initial}{${last_name}{{}}"));
        assertEquals("}James}T{Kirk{{}}", parser.parse("}${first_name}}${initial}{${last_name}{{}}${}"));

        assertEquals("{$$something}JamesTKirk", parser.parse("{$$something}${first_name}${initial}${last_name}"));
        assertEquals("${", parser.parse("${"));
        assertEquals("}", parser.parse("}"));
        assertEquals("Hello ${ this is a test.", parser.parse("Hello ${ this is a test."));
        assertEquals("Hello } this is a test.", parser.parse("Hello } this is a test."));
        assertEquals("Hello } ${ this is a test.", parser.parse("Hello } ${ this is a test."));
    }

    @Test
    public void shallNotInterpolateSkippedVaiables() {
        GenericTokenParser parser = new GenericTokenParser("${", "}", new VariableTokenHandler(new HashMap<String, String>()));

        assertEquals("${skipped} variable", parser.parse("\\${skipped} variable"));
        assertEquals("This is a ${skipped} variable", parser.parse("This is a \\${skipped} variable"));
        assertEquals("null ${skipped} variable", parser.parse("${skipped} \\${skipped} variable"));
        assertEquals("The null is ${skipped} variable", parser.parse("The ${skipped} is \\${skipped} variable"));
    }

    @Test(timeout = 1000)
    public void shouldParseFastOnJdk7u6() {
        // issue #760
        GenericTokenParser parser = new GenericTokenParser("${", "}", new VariableTokenHandler(new HashMap<String, String>() {
            {
                put("first_name", "James");
                put("initial", "T");
                put("last_name", "Kirk");
                put("", "");
            }
        }));

        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            input.append("${first_name} ${initial} ${last_name} reporting. ");
        }
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            expected.append("James T Kirk reporting. ");
        }
        assertEquals(expected.toString(), parser.parse(input.toString()));
    }

    /**
     * 记号处理器,一个简单的实现
     */
    public static class VariableTokenHandler implements TokenHandler {

        private Map<String, String> variables;

        public VariableTokenHandler(Map<String, String> variables) {
            this.variables = variables;
        }

        @Override
        public String handleToken(String content) {
            String s = variables.get(content);
            return s;
        }
    }
}
