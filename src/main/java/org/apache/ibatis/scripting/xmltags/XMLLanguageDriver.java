package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.Configuration;

import java.util.Properties;

/**
 * @author Eduardo Macarron
 */

/**
 * XML语言驱动
 */
public class XMLLanguageDriver implements LanguageDriver {

    @Override
    public ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
        //返回默认的参数处理器
        return new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
    }

    @Override
    public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
        //用XML脚本构建器解析
        XMLScriptBuilder builder = new XMLScriptBuilder(configuration, script, parameterType);
        return builder.parseScriptNode();
    }

    //注解方式构建mapper，一般不用，可以暂时忽略
    @Override
    public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
        // issue #3
        if (script.startsWith("<script>")) {
            XPathParser parser = new XPathParser(script, false,
                    configuration.getVariables(), new XMLMapperEntityResolver());
            return createSqlSource(configuration, parser.evalNode("/script"), parameterType);
        } else {
            // issue #127
            Properties configurationVariables = configuration.getVariables();
            //输入字符串 (name = ${username}),可能会输出(name = 张三)，当然映射中要有 key=username,value=张三
            script = PropertyParser.parse(script, configurationVariables);
            TextSqlNode textSqlNode = new TextSqlNode(script);
            //一种是动态，一种是原始
            if (textSqlNode.isDynamic()) {
                return new DynamicSqlSource(configuration, textSqlNode);
            } else {
                return new RawSqlSource(configuration, script, parameterType);
            }
        }
    }
}
