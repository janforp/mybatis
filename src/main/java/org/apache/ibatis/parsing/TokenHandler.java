package org.apache.ibatis.parsing;

/**
 * 记号处理器
 */
public interface TokenHandler {

    /**
     * 处理记号，具体怎么处理交给实现
     *
     * @param content 输入字符串
     * @return 输出字符串
     */
    String handleToken(String content);
}

