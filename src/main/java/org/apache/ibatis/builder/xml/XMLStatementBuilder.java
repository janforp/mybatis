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
import org.w3c.dom.Node;

import java.util.List;
import java.util.Locale;

/**
 * XML语句构建器，建造者模式,继承BaseBuilder
 *
 * 解析mapper文件的每一个方法 配置select|insert|update|delete，每一个方法sql都是一个statement
 *
 * @author Clinton Begin
 */
public class XMLStatementBuilder extends BaseBuilder {

    private MapperBuilderAssistant builderAssistant;

    /**
     * 一个 insert/delete/update/select 方法sql中的所有内容，包括动态标签
     */
    private XNode methodSqlNode;

    private String requiredDatabaseId;

    public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode methodSqlNode) {
        this(configuration, builderAssistant, methodSqlNode, null);
    }

    /**
     * sql语句构造器
     *
     * @param configuration 配置
     * @param builderAssistant 辅助
     * @param methodSqlNode 一个 insert/delete/update/select 方法sql中的所有内容，包括动态标签
     * @param databaseId 数据库id
     */
    public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode methodSqlNode, String databaseId) {
        super(configuration);
        this.builderAssistant = builderAssistant;
        this.methodSqlNode = methodSqlNode;
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
    public void parseStatementNode() {
        //<delete id="deleteAuthor" parameterType="int">
        String id = methodSqlNode.getStringAttribute("id");
        String databaseId = methodSqlNode.getStringAttribute("databaseId");

        //如果databaseId不匹配，退出
        if (!databaseIdMatchesCurrent(id, databaseId, this.requiredDatabaseId)) {
            return;
        }

        //暗示驱动程序每次批量返回的结果行数
        Integer fetchSize = methodSqlNode.getIntAttribute("fetchSize");
        //超时时间
        Integer timeout = methodSqlNode.getIntAttribute("timeout");
        //引用外部 parameterMap,已废弃
        String parameterMap = methodSqlNode.getStringAttribute("parameterMap");
        //参数类型
        //<update id="updateAuthor" parameterType="org.apache.ibatis.domain.blog.Author">
        String parameterType = methodSqlNode.getStringAttribute("parameterType");
        //参数类型
        Class<?> parameterTypeClass = resolveClass(parameterType);
        //引用外部的 resultMap(高级功能)
        String resultMap = methodSqlNode.getStringAttribute("resultMap");
        //结果类型
        String resultType = methodSqlNode.getStringAttribute("resultType");
        //脚本语言,mybatis3.2的新功能
        String lang = methodSqlNode.getStringAttribute("lang");
        //得到语言驱动
        LanguageDriver langDriver = getLanguageDriver(lang);

        Class<?> resultTypeClass = resolveClass(resultType);
        //结果集类型，FORWARD_ONLY|SCROLL_SENSITIVE|SCROLL_INSENSITIVE 中的一种
        String resultSetType = methodSqlNode.getStringAttribute("resultSetType");
        ResultSetType resultSetTypeEnum = resolveResultSetType(resultSetType);
        //语句类型, STATEMENT|PREPARED|CALLABLE 的一种
        //默认 PREPARED
        String statementTypeConfig = methodSqlNode.getStringAttribute("statementType", StatementType.PREPARED.toString());
        StatementType statementType = StatementType.valueOf(statementTypeConfig);

        //获取命令类型(select|insert|update|delete)
        Node contextNode = methodSqlNode.getNode();
        String nodeName = contextNode.getNodeName();
        SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));
        //是否是查询函数
        boolean isSelect = (sqlCommandType == SqlCommandType.SELECT);
        //如果是查询，默认不刷新缓存，当然也可以指定刷新
        boolean flushCache = methodSqlNode.getBooleanAttribute("flushCache", !isSelect);
        //是否要缓存select结果,如果是查询，默认使用缓存，当然可以指定不用缓存
        boolean useCache = methodSqlNode.getBooleanAttribute("useCache", isSelect);
        //仅针对嵌套结果 select 语句适用：如果为 true，就是假设包含了嵌套结果集或是分组了，这样的话当返回一个主结果行的时候，就不会发生有对前面结果集的引用的情况。
        //这就使得在获取嵌套的结果集的时候不至于导致内存不够用。默认值：false。
        boolean resultOrdered = methodSqlNode.getBooleanAttribute("resultOrdered", false);

        // Include Fragments before parsing
        //解析之前先解析<include>SQL片段
        XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
        includeParser.applyIncludes(contextNode);

        // Parse selectKey after includes and remove them.
        //解析之前先解析<selectKey>
        processSelectKeyNodes(id, parameterTypeClass, langDriver);

        // Parse the SQL (pre: <selectKey> and <include> were parsed and removed)
        //解析成SqlSource，一般是DynamicSqlSource，解析出所有的sql片段，带上动态标签中的表达式
        SqlSource sqlSource = langDriver.createSqlSource(configuration, methodSqlNode, parameterTypeClass);
        String resultSets = methodSqlNode.getStringAttribute("resultSets");
        //(仅对 insert 有用) 标记一个属性, MyBatis 会通过 getGeneratedKeys 或者通过 insert 语句的 selectKey 子元素设置它的值
        String keyProperty = methodSqlNode.getStringAttribute("keyProperty");
        //(仅对 insert 有用) 标记一个属性, MyBatis 会通过 getGeneratedKeys 或者通过 insert 语句的 selectKey 子元素设置它的值
        String keyColumn = methodSqlNode.getStringAttribute("keyColumn");
        KeyGenerator keyGenerator;
        String keyStatementId = id + SelectKeyGenerator.SELECT_KEY_SUFFIX;
        //org.apache.ibatis.submitted.selectkey.Table1.insert!selectKey
        keyStatementId = builderAssistant.applyCurrentNamespace(keyStatementId, true);
        boolean hasKeyGenerator = configuration.hasKeyGenerator(keyStatementId);
        if (hasKeyGenerator) {
            //通过 <selectKey>
            keyGenerator = configuration.getKeyGenerator(keyStatementId);
        } else {
            //直接在sql上指定 <insert id="insertTable2WithGeneratedKeyXml" useGeneratedKeys="true" keyProperty="nameId,generatedName" keyColumn="ID,NAME_FRED">
            //则使用 Jdbc3KeyGenerator 自增

            //setting中配置了能够使用自增主键，并且该sql是插入类型，则默认使用自增主键，否则还是以该sql的具体配置为准
            boolean defaultUseKeyGenerator = configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType);
            //该sql的具体配置，如果没配置，则取 defaultUseKeyGenerator
            Boolean useGeneratedKeys = methodSqlNode.getBooleanAttribute("useGeneratedKeys", defaultUseKeyGenerator);
            //如果要使用主键，则用 Jdbc3KeyGenerator，否则传一个自增主键的空实现实例
            keyGenerator = useGeneratedKeys ? new Jdbc3KeyGenerator() : new NoKeyGenerator();
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
     * @param id sql方法的id <insert id="insertAndgetkey" parameterType="com.soft.mybatis.model.User">
     * @param parameterTypeClass <insert id="insertAndgetkey" parameterType="com.soft.mybatis.model.User">
     * @param langDriver
     */
    private void processSelectKeyNodes(String id, Class<?> parameterTypeClass, LanguageDriver langDriver) {
        List<XNode> selectKeyNodes = methodSqlNode.evalNodes("selectKey");
        if (configuration.getDatabaseId() != null) {
            parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, configuration.getDatabaseId());
        }
        parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver, null);
        removeSelectKeyNodes(selectKeyNodes);
    }

    private void parseSelectKeyNodes(String parentId, List<XNode> list, Class<?> parameterTypeClass, LanguageDriver langDriver, String skRequiredDatabaseId) {
        for (XNode nodeToHandle : list) {
            //sql的id + "!selectKey"如：insertOne!selectKey
            String id = parentId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
            String databaseId = nodeToHandle.getStringAttribute("databaseId");
            boolean databaseIdMatchesCurrent = databaseIdMatchesCurrent(id, databaseId, skRequiredDatabaseId);
            if (databaseIdMatchesCurrent) {
                parseSelectKeyNode(id, nodeToHandle, parameterTypeClass, langDriver, databaseId);
            }
        }
    }

    /**
     * 解析 <selectKey/>如：
     *
     * <selectKey keyProperty="id" resultType="int" order="BEFORE">
     * <if test="_databaseId == 'oracle'">
     * select seq_users.nextval from dual
     * </if>
     * <if test="_databaseId == 'db2'">
     * select nextval for seq_users from sysibm.sysdummy1"
     * </if>
     * </selectKey>
     *
     * @param id sql的id + "!selectKey"如：insertOne!selectKey
     * @param keyNode <selectKey/>
     * @param parameterTypeClass sql的入参数，也就是主键自动生成之后需要回写到参数的某个属性
     * @param langDriver 语音驱动
     * @param databaseId 数据库id
     */
    private void parseSelectKeyNode(String id, XNode keyNode, Class<?> parameterTypeClass, LanguageDriver langDriver, String databaseId) {
        //返回主键类型
        String keyResultType = keyNode.getStringAttribute("resultType");
        Class<?> keyResultTypeClass = resolveClass(keyResultType);
        //生成主键sql也可以指定 statementType
        StatementType statementType = StatementType.valueOf(keyNode.getStringAttribute("statementType", StatementType.PREPARED.toString()));
        //主键的属性名称
        String keyProperty = keyNode.getStringAttribute("keyProperty");
        //主键列
        String keyColumn = keyNode.getStringAttribute("keyColumn");
        //order="AFTER"，默认为after,是指在执行sql之前还是之后获取主键
        boolean executeBefore = "BEFORE".equals(keyNode.getStringAttribute("order", "AFTER"));
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
        //TODO ？ langDriver 实例如何来？
        SqlSource sqlSource = langDriver.createSqlSource(configuration, keyNode, parameterTypeClass);
        //主键相关为查询类型
        SqlCommandType sqlCommandType = SqlCommandType.SELECT;
        builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
                fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, keyResultTypeClass,
                resultSetTypeEnum, flushCache, useCache, resultOrdered,
                keyGenerator, keyProperty, keyColumn, databaseId, langDriver, null);
        //org.apache.ibatis.submitted.selectkey.Table1.insert!selectKey
        id = builderAssistant.applyCurrentNamespace(id, false);

        MappedStatement keyStatement = configuration.getMappedStatement(id, false);
        SelectKeyGenerator selectKeyGenerator = new SelectKeyGenerator(keyStatement, executeBefore);
        //丢进map
        configuration.addKeyGenerator(id, selectKeyGenerator);
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
