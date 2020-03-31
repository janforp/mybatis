package org.apache.ibatis.parsing;

/**
 * 普通记号解析器，处理#{}和${}参数
 *
 * @author Clinton Begin
 */
public class GenericTokenParser {

    /**
     * 有一个开始和结束记号，如：${
     */
    private final String openToken;

    /**
     * }
     */
    private final String closeToken;

    /**
     * 记号处理器，一个接口，处理逻辑交给实现
     */
    private final TokenHandler handler;

    public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
        this.openToken = openToken;
        this.closeToken = closeToken;
        this.handler = handler;
    }

    /**
     * 处理由标记符enclose的字符，该parse只负责把被标记包围的字符串解析出来，然后具体怎么处理交给 handler 处理
     *
     * @param text 待处理的字符串，如：AND username = ${username};
     * @return 处理标记字符串，如：AND username = Kobe;
     */
    public String parse(String text) {
        StringBuilder builder = new StringBuilder();
        if (text == null || text.length() == 0) {
            return builder.toString();
        }
        char[] src = text.toCharArray();
        int offset = 0;
        //$或者#第一次出现的下标
        int start = text.indexOf(openToken, offset);//从offset处开始找 openToken 的下标
        //这里是循环解析参数，参考GenericTokenParserTest,比如可以解析${first_name} ${initial} ${last_name} reporting.这样的字符串,里面有3个 ${}
        while (start > -1) {
            //判断一下 ${ 前面是否是反斜杠，这个逻辑在老版的mybatis中（如3.1.0）是没有的
            if (start > 0 && src[start - 1] == '\\') {
                // the variable is escaped. remove the backslash.
                //新版已经没有调用substring了，改为调用如下的offset方式，提高了效率
                //issue #760
                builder.append(src, offset, start - offset - 1).append(openToken);
                offset = start + openToken.length();
            } else {
                int end = text.indexOf(closeToken, start);//如果找到 openToken 的下标的位置 start, 则就从 start的下一个开始寻找 closeToken 的下标
                if (end == -1) {
                    builder.append(src, offset, src.length - offset);
                    offset = src.length;
                } else {
                    builder.append(src, offset, start - offset);//如果找到了开始跟结尾的下标，则说明有一个变量匹配到了
                    offset = start + openToken.length();
                    String content = new String(src, offset, end - offset);
                    //得到一对大括号里的字符串后，调用handler.handleToken,比如替换变量这种功能

                    String handledToken = handler.handleToken(content);
                    builder.append(handledToken);
                    offset = end + closeToken.length();//下一次开始找的下标就是结束标志的下一个
                }
            }
            start = text.indexOf(openToken, offset);
        }
        if (offset < src.length) {
            //把后面的字符拼接起来，保证不丢失
            builder.append(src, offset, src.length - offset);
        }
        return builder.toString();
    }
}
