package org.apache.ibatis.parsing;

import java.util.Properties;

/**
 * 属性解析器
 *
 * @author Clinton Begin
 */
public class PropertyParser {

    private PropertyParser() {
        // Prevent Instantiation
    }

    /**
     * Properties variables = new Properties();
     * variables.setProperty("name", "张三");
     * variables.setProperty("gender", "2");
     * String name = PropertyParser.parse("${name}", variables);
     * Assert.assertEquals("张三", name);
     *
     * 如：输入字符串 (name = ${username}),可能会输出(name = 张三)，当然映射中要有 key=username,value=张三
     *
     * @param string 待处理的字符串
     * @param variables 映射
     * @return 处理之后的字符串
     */
    public static String parse(String string, Properties variables) {
        VariableTokenHandler handler = new VariableTokenHandler(variables);
        GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
        return parser.parse(string);
    }

    //就是一个map，用相应的value替换key
    private static class VariableTokenHandler implements TokenHandler {

        /**
         * handleToken函数会从该映射中拿值
         */
        private Properties variables;

        public VariableTokenHandler(Properties variables) {
            this.variables = variables;
        }

        @Override
        public String handleToken(String content) {
            if (variables != null && variables.containsKey(content)) {
                return variables.getProperty(content);
            }
            return "${" + content + "}";
        }
    }
}
