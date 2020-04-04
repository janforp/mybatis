package org.apache.ibatis.scripting.xmltags;

import lombok.Getter;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.scripting.ScriptingException;
import org.apache.ibatis.type.SimpleTypeRegistry;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Clinton Begin
 */

/**
 * 文本SQL节点（CDATA|TEXT）
 */
public class TextSqlNode implements SqlNode {

    private String text;

    private Pattern injectionFilter;

    /**
     * @param text 如：
     * select *
     * from users
     * where id = #{id}
     */
    public TextSqlNode(String text) {
        this(text, null);
    }

    public TextSqlNode(String text, Pattern injectionFilter) {
        this.text = text;
        this.injectionFilter = injectionFilter;
    }

    /**
     * ${} 占位符处理器
     *
     * @param handler 具体处理匹配字符串的处理器
     * @return 占位符处理器
     */
    private GenericTokenParser createParser(TokenHandler handler) {
        return new GenericTokenParser("${", "}", handler);
    }

    @Override
    public boolean apply(DynamicContext context) {
        BindingTokenParser bindingTokenParser = new BindingTokenParser(context, injectionFilter);
        GenericTokenParser parser = createParser(bindingTokenParser);
        String parse = parser.parse(text);
        context.appendSql(parse);
        return true;
    }

    //判断是否是动态sql
    public boolean isDynamic() {
        DynamicCheckerTokenParser checker = new DynamicCheckerTokenParser();
        //占位符 ${} 解析器
        GenericTokenParser parser = createParser(checker);

        //替换 ${占位符}
        parser.parse(text);

        boolean dynamic = checker.isDynamic();
        return dynamic;
    }

    /**
     * 丢进去一个字符串 ， 返回一个字符串
     */
    private static class BindingTokenParser implements TokenHandler {

        private DynamicContext context;

        private Pattern injectionFilter;

        public BindingTokenParser(DynamicContext context, Pattern injectionFilter) {
            this.context = context;
            this.injectionFilter = injectionFilter;
        }

        @Override
        public String handleToken(String content) {
            Map<String, Object> contextBindingMap = context.getBindings();
            Object parameter = contextBindingMap.get("_parameter");
            if (parameter == null) {
                contextBindingMap.put("value", null);
            } else if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
                contextBindingMap.put("value", parameter);
            }
            //从缓存里取得值
            Object value = OgnlCache.getValue(content, contextBindingMap);
            String srtValue = (value == null ? "" : String.valueOf(value)); // issue #274 return "" instead of "null"
            checkInjection(srtValue);
            return srtValue;
        }

        //检查是否匹配正则表达式
        private void checkInjection(String value) {
            if (injectionFilter != null && !injectionFilter.matcher(value).matches()) {
                throw new ScriptingException("Invalid input. Please conform to regex" + injectionFilter.pattern());
            }
        }
    }

    //动态SQL检查器
    private static class DynamicCheckerTokenParser implements TokenHandler {

        @Getter
        private boolean isDynamic;

        public DynamicCheckerTokenParser() {
            // Prevent Synthetic Access
        }

        @Override
        public String handleToken(String content) {
            //灰常简单，设置isDynamic为true，即调用了这个方法就变成了动态的，否则就表示该文本中没有占位符，就是静态的
            this.isDynamic = true;
            return null;
        }
    }
}