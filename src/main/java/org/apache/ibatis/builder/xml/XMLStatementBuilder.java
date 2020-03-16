/*
 *    Copyright 2009-2014 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.apache.ibatis.builder.xml;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

import java.util.List;
import java.util.Locale;

/**
 * XML语句构建器，建造者模式,继承BaseBuilder
 *
 * @author Clinton Begin
 */
public class XMLStatementBuilder extends BaseBuilder {

    private MapperBuilderAssistant builderAssistant;

    private XNode context;

    private String requiredDatabaseId;

    public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context) {
        this(configuration, builderAssistant, context, null);
    }

    public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context, String databaseId) {
        super(configuration);
        this.builderAssistant = builderAssistant;
        this.context = context;
        this.requiredDatabaseId = databaseId;
    }

    //解析语句(select|insert|update|delete)
    //<select
    //  id="selectPerson"
    //  parameterType="int"
    //  parameterMap="deprecated"
    //  resultType="hashmap"
    //  resultMap="personResultMap"
    //  flushCache="false"
    //  useCache="true"
    //  timeout="10000"
    //  fetchSize="256"
    //  statementType="PREPARED"
    //  resultSetType="FORWARD_ONLY">
    //  SELECT * FROM PERSON WHERE ID = #{id}
    //</select>

    //<insert id="insertAndgetkey" parameterType="com.soft.mybatis.model.User">
    //        <!--selectKey  会将 SELECT LAST_INSERT_ID()的结果放入到传入的model的主键里面，
    //            keyProperty 对应的model中的主键的属性名，这里是 user 中的id，因为它跟数据库的主键对应
    //            order AFTER 表示 SELECT LAST_INSERT_ID() 在insert执行之后执行,多用与自增主键，
    //                  BEFORE 表示 SELECT LAST_INSERT_ID() 在insert执行之前执行，这样的话就拿不到主键了，
    //                        这种适合那种主键不是自增的类型
    //            resultType 主键类型 -->
    //        <selectKey keyProperty="id" order="AFTER" resultType="java.lang.Integer">
    //            SELECT LAST_INSERT_ID()
    //        </selectKey>
    //        insert into t_user (username,password,create_date) values(#{username},#{password},#{createDate})
    //    </insert>
    //————————————————
    //版权声明：本文为CSDN博主「第一小菜鸟」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
    //原文链接：https://blog.csdn.net/xu1916659422/article/details/77921912
    public void parseStatementNode() {
        String id = context.getStringAttribute("id");
        String databaseId = context.getStringAttribute("databaseId");

        //如果databaseId不匹配，退出
        if (!databaseIdMatchesCurrent(id, databaseId, this.requiredDatabaseId)) {
            return;
        }

        //暗示驱动程序每次批量返回的结果行数
        Integer fetchSize = context.getIntAttribute("fetchSize");
        //超时时间
        Integer timeout = context.getIntAttribute("timeout");
        //引用外部 parameterMap,已废弃
        String parameterMap = context.getStringAttribute("parameterMap");
        //参数类型
        String parameterType = context.getStringAttribute("parameterType");
        Class<?> parameterTypeClass = resolveClass(parameterType);
        //引用外部的 resultMap(高级功能)
        String resultMap = context.getStringAttribute("resultMap");
        //结果类型
        String resultType = context.getStringAttribute("resultType");
        //脚本语言,mybatis3.2的新功能
        String lang = context.getStringAttribute("lang");
        //得到语言驱动
        LanguageDriver langDriver = getLanguageDriver(lang);

        Class<?> resultTypeClass = resolveClass(resultType);
        //结果集类型，FORWARD_ONLY|SCROLL_SENSITIVE|SCROLL_INSENSITIVE 中的一种
        String resultSetType = context.getStringAttribute("resultSetType");
        //语句类型, STATEMENT|PREPARED|CALLABLE 的一种
        StatementType statementType = StatementType.valueOf(context.getStringAttribute("statementType", StatementType.PREPARED.toString()));
        ResultSetType resultSetTypeEnum = resolveResultSetType(resultSetType);

        //获取命令类型(select|insert|update|delete)
        String nodeName = context.getNode().getNodeName();
        SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));
        boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
        //如果是查询，默认不刷新缓存
        boolean flushCache = context.getBooleanAttribute("flushCache", !isSelect);
        //是否要缓存select结果,如果是查询，默认使用缓存
        boolean useCache = context.getBooleanAttribute("useCache", isSelect);
        //仅针对嵌套结果 select 语句适用：如果为 true，就是假设包含了嵌套结果集或是分组了，这样的话当返回一个主结果行的时候，就不会发生有对前面结果集的引用的情况。
        //这就使得在获取嵌套的结果集的时候不至于导致内存不够用。默认值：false。
        boolean resultOrdered = context.getBooleanAttribute("resultOrdered", false);

        // Include Fragments before parsing
        //解析之前先解析<include>SQL片段
        XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
        includeParser.applyIncludes(context.getNode());

        // Parse selectKey after includes and remove them.
        //解析之前先解析<selectKey>
        processSelectKeyNodes(id, parameterTypeClass, langDriver);

        // Parse the SQL (pre: <selectKey> and <include> were parsed and removed)
        //解析成SqlSource，一般是DynamicSqlSource
        SqlSource sqlSource = langDriver.createSqlSource(configuration, context, parameterTypeClass);
        String resultSets = context.getStringAttribute("resultSets");
        //(仅对 insert 有用) 标记一个属性, MyBatis 会通过 getGeneratedKeys 或者通过 insert 语句的 selectKey 子元素设置它的值
        String keyProperty = context.getStringAttribute("keyProperty");
        //(仅对 insert 有用) 标记一个属性, MyBatis 会通过 getGeneratedKeys 或者通过 insert 语句的 selectKey 子元素设置它的值
        String keyColumn = context.getStringAttribute("keyColumn");
        KeyGenerator keyGenerator;
        String keyStatementId = id + SelectKeyGenerator.SELECT_KEY_SUFFIX;
        keyStatementId = builderAssistant.applyCurrentNamespace(keyStatementId, true);
        if (configuration.hasKeyGenerator(keyStatementId)) {
            keyGenerator = configuration.getKeyGenerator(keyStatementId);
        } else {
            keyGenerator = context.getBooleanAttribute("useGeneratedKeys",
                    configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType))
                    ? new Jdbc3KeyGenerator() : new NoKeyGenerator();
        }

        //又去调助手类
        builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
                fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass,
                resultSetTypeEnum, flushCache, useCache, resultOrdered,
                keyGenerator, keyProperty, keyColumn, databaseId, langDriver, resultSets);
    }

    /**
     * <selectKey keyProperty="id" order="AFTER" resultType="java.lang.Integer">
     * SELECT LAST_INSERT_ID()
     * </selectKey>
     *
     * @param id <insert id="insertAndgetkey" parameterType="com.soft.mybatis.model.User">
     * @param parameterTypeClass <insert id="insertAndgetkey" parameterType="com.soft.mybatis.model.User">
     * @param langDriver
     */
    private void processSelectKeyNodes(String id, Class<?> parameterTypeClass, LanguageDriver langDriver) {
        List<XNode> selectKeyNodes = context.evalNodes("selectKey");
        if (configuration.getDatabaseId() != null) {
            parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, configuration.getDatabaseId());
        }
        parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, null);
        removeSelectKeyNodes(selectKeyNodes);
    }

    private void parseSelectKeyNodes(String parentId, List<XNode> list, Class<?> parameterTypeClass, LanguageDriver langDriver, String skRequiredDatabaseId) {
        for (XNode nodeToHandle : list) {
            String id = parentId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
            String databaseId = nodeToHandle.getStringAttribute("databaseId");
            if (databaseIdMatchesCurrent(id, databaseId, skRequiredDatabaseId)) {
                parseSelectKeyNode(id, nodeToHandle, parameterTypeClass, langDriver, databaseId);
            }
        }
    }

    private void parseSelectKeyNode(String id, XNode nodeToHandle, Class<?> parameterTypeClass, LanguageDriver langDriver, String databaseId) {
        //返回主键类型
        String resultType = nodeToHandle.getStringAttribute("resultType");
        Class<?> resultTypeClass = resolveClass(resultType);
        StatementType statementType = StatementType.valueOf(nodeToHandle.getStringAttribute("statementType", StatementType.PREPARED.toString()));
        //主键的属性名称
        String keyProperty = nodeToHandle.getStringAttribute("keyProperty");
        //主键列
        String keyColumn = nodeToHandle.getStringAttribute("keyColumn");
        //order="AFTER"，默认为after
        boolean executeBefore = "BEFORE".equals(nodeToHandle.getStringAttribute("order", "AFTER"));
        //defaults
        boolean useCache = false;
        boolean resultOrdered = false;
        KeyGenerator keyGenerator = new NoKeyGenerator();
        Integer fetchSize = null;
        Integer timeout = null;
        boolean flushCache = false;
        String parameterMap = null;
        String resultMap = null;
        ResultSetType resultSetTypeEnum = null;
        //langDriver是一个接口
        SqlSource sqlSource = langDriver.createSqlSource(configuration, nodeToHandle, parameterTypeClass);
        SqlCommandType sqlCommandType = SqlCommandType.SELECT;
        builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
                fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass,
                resultSetTypeEnum, flushCache, useCache, resultOrdered,
                keyGenerator, keyProperty, keyColumn, databaseId, langDriver, null);

        id = builderAssistant.applyCurrentNamespace(id, false);

        MappedStatement keyStatement = configuration.getMappedStatement(id, false);
        configuration.addKeyGenerator(id, new SelectKeyGenerator(keyStatement, executeBefore));
    }

    private void removeSelectKeyNodes(List<XNode> selectKeyNodes) {
        for (XNode nodeToHandle : selectKeyNodes) {
            nodeToHandle.getParent().getNode().removeChild(nodeToHandle.getNode());
        }
    }

    private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
        if (requiredDatabaseId != null) {
            return requiredDatabaseId.equals(databaseId);
        } else {
            if (databaseId != null) {
                return false;
            }
            // skip this statement if there is a previous one with a not null databaseId
            id = builderAssistant.applyCurrentNamespace(id, false);
            if (this.configuration.hasStatement(id, false)) {
                // issue #2
                MappedStatement previous = this.configuration.getMappedStatement(id, false);
                return previous.getDatabaseId() == null;
            }
        }
        return true;
    }

    //取得语言驱动
    private LanguageDriver getLanguageDriver(String lang) {
        Class<?> langClass = null;
        if (lang != null) {
            langClass = resolveClass(lang);
        }
        //调用builderAssistant
        return builderAssistant.getLanguageDriver(langClass);
    }

}
