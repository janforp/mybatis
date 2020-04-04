package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Clinton Begin
 */

/**
 * XML脚本构建器
 */
public class XMLScriptBuilder extends BaseBuilder {

    /**
     * xml节点
     */
    private XNode xmlNode;

    /**
     * 是否动态sql
     */
    private boolean isDynamic;

    /**
     * 参数类型
     */
    private Class<?> parameterType;

    public XMLScriptBuilder(Configuration configuration, XNode context) {
        this(configuration, context, null);
    }

    public XMLScriptBuilder(Configuration configuration, XNode xmlNode, Class<?> parameterType) {
        //org.apache.ibatis.builder.BaseBuilder.BaseBuilder
        super(configuration);
        this.xmlNode = xmlNode;
        this.parameterType = parameterType;
    }

    /**
     * TODO 通过xml node 获取一个动态或者静态的 SqlSource
     *
     * @return SqlSource
     */
    public SqlSource parseScriptNode() {
        //<select id="selectByIdNoFlush" resultMap="personMap" parameterType="int">
        //        SELECT id, firstName, lastName
        //        FROM person
        //        WHERE id = #{id}
        //</select>
        List<SqlNode> sqlNodeList = parseDynamicTags(xmlNode);
        MixedSqlNode rootSqlNode = new MixedSqlNode(sqlNodeList);
        SqlSource sqlSource;//准备解析出待 ？ 占位符的sql以及参数对象列表
        if (isDynamic) {
            sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
        } else {//没有动态标签
            sqlSource = new RawSqlSource(configuration, rootSqlNode, parameterType);
        }
        return sqlSource;
    }

    /**
     * 解析动态标签
     *
     * //<update parameterType="org.apache.ibatis.domain.blog.Author" id="updateAuthorIfNecessary">
     * //<set>
     * //<if test="username != null">username=#{username},</if>
     * //<if test="password != null">password=#{password},</if>
     * //<if test="email != null">email=#{email},</if>
     * //<if test="bio != null">bio=#{bio}</if>
     * //</set>
     * //</update>
     *
     * @param methodXnode 被解析的node，一般情况是一个 mapper.xml 文件中的一个方法
     * @return 标签列表
     */
    List<SqlNode> parseDynamicTags(XNode methodXnode) {
        //返回
        List<SqlNode> sqlNodeList = new ArrayList<SqlNode>();

        Node methodNode = methodXnode.getNode();
        NodeList children = methodNode.getChildNodes();
        int childrenLength = children.getLength();

        //试图解析sql中的各种标签，动态标签
        for (int i = 0; i < childrenLength; i++) {

            Node item = children.item(i);

            //<if test="username != null">username=#{username},</if>
            XNode child = methodXnode.newXNode(item);

            Node node = child.getNode();

            short childNodeType = node.getNodeType();
            //CDATASection || Text
            boolean isTextOrCdata = (childNodeType == Node.CDATA_SECTION_NODE || childNodeType == Node.TEXT_NODE);

            if (isTextOrCdata) {
                String data = child.getStringBody("");
                TextSqlNode textSqlNode = new TextSqlNode(data);

                //先计算，再返回，如果文本有 ${} 占位符，则为动态的，否则，静态
                boolean dynamic = textSqlNode.isDynamic();
                if (dynamic) {
                    sqlNodeList.add(textSqlNode);
                    isDynamic = true;
                } else {
                    StaticTextSqlNode staticTextSqlNode = new StaticTextSqlNode(data);
                    sqlNodeList.add(staticTextSqlNode);
                }
            }

            //子类型还是 一个元素如：<if>
            if (childNodeType == Node.ELEMENT_NODE) {
                //Element
                String nodeName = node.getNodeName();

                //获取具体动态标签处理器
                //Map<String, NodeHandler> map = new HashMap<String, NodeHandler>();
                //        map.put("trim", new TrimHandler());
                //        map.put("where", new WhereHandler());
                //        map.put("set", new SetHandler());
                //        map.put("foreach", new ForEachHandler());
                //        map.put("if", new IfHandler());
                //        map.put("choose", new ChooseHandler());
                //        map.put("when", new IfHandler());
                //        map.put("otherwise", new OtherwiseHandler());
                //        map.put("bind", new BindHandler());
                //        return map.get(nodeName);
                NodeHandler handler = nodeHandlers(nodeName);
                //瞎几把写的 动态标签,报错
                if (handler == null) {
                    throw new BuilderException("Unknown element <" + nodeName + "> in SQL statement.");
                }
                handler.handleNode(child, sqlNodeList);
                //有这些元素，必须是动态sql
                isDynamic = true;
            }
        }
        return sqlNodeList;
    }

    /**
     * sql中支持的动态元素
     * 根据node名称来获取对应类型的node处理器
     *
     * @param nodeName 元素名称
     * @return 根据名称查询
     */
    NodeHandler nodeHandlers(String nodeName) {
        Map<String, NodeHandler> map = new HashMap<String, NodeHandler>();
        map.put("trim", new TrimHandler());
        map.put("where", new WhereHandler());
        map.put("set", new SetHandler());
        map.put("foreach", new ForEachHandler());
        map.put("if", new IfHandler());
        map.put("choose", new ChooseHandler());
        map.put("when", new IfHandler());
        map.put("otherwise", new OtherwiseHandler());
        map.put("bind", new BindHandler());
        return map.get(nodeName);
    }

    private interface NodeHandler {

        /**
         * 被该处理器处理的node，处理之后放进该集合，后面直接使用
         *
         * @param nodeToHandle 被该处理器处理的node
         * @param targetContents 处理之后放进该集合，后面直接使用
         */
        void handleNode(XNode nodeToHandle, List<SqlNode> targetContents);
    }

    /**
     * <bind name="pattern" value="'%' + _parameter + '%'" />
     */
    private static class BindHandler implements NodeHandler {

        public BindHandler() {
            // Prevent Synthetic Access
        }

        @Override
        public void handleNode(XNode bindNode, List<SqlNode> targetContents) {
            final String name = bindNode.getStringAttribute("name");
            final String expression = bindNode.getStringAttribute("value");
            final VarDeclSqlNode node = new VarDeclSqlNode(name, expression);
            targetContents.add(node);
        }
    }

    /**
     * <update id="testTrim" parameterType="com.mybatis.pojo.User">
     * update user
     * <trim prefix="set" suffixOverrides=",">
     * <if test="cash!=null and cash!=''">cash= #{cash},</if>
     * <if test="address!=null and address!=''">address= #{address},</if>
     * </trim>
     * <where>id = #{id}</where>
     * </update>
     *
     * <trim prefix="(" suffix=")" suffixOverrides="," >
     * 1.<trim prefix="" suffix="" suffixOverrides="" prefixOverrides=""></trim>
     * prefix:在trim标签内sql语句加上前缀。
     * suffix:在trim标签内sql语句加上后缀。
     * suffixOverrides:指定去除多余的后缀内容，如：suffixOverrides=","，去除trim标签内sql语句多余的后缀","。
     * prefixOverrides:指定去除多余的前缀内容
     *
     * 执行的sql语句也许是这样的：insert into cart (id,user_id,deal_id,) values(1,2,1,);显然是错误的
     * 指定之后语句就会变成insert into cart (id,user_id,deal_id) values(1,2,1);这样就将“，”去掉了。
     * 前缀也是一个道理这里就不说了。
     */
    private class TrimHandler implements NodeHandler {

        public TrimHandler() {
            // Prevent Synthetic Access
        }

        @Override
        public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
            List<SqlNode> contents = parseDynamicTags(nodeToHandle);
            MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
            String prefix = nodeToHandle.getStringAttribute("prefix");
            String prefixOverrides = nodeToHandle.getStringAttribute("prefixOverrides");
            String suffix = nodeToHandle.getStringAttribute("suffix");
            String suffixOverrides = nodeToHandle.getStringAttribute("suffixOverrides");
            TrimSqlNode trim = new TrimSqlNode(configuration, mixedSqlNode, prefix, prefixOverrides, suffix, suffixOverrides);
            targetContents.add(trim);
        }
    }

    /**
     * <where>
     * ***<if test="true">
     * *****ORDER_TYPE = #{value}
     * ***</if>
     * </where>
     */
    private class WhereHandler implements NodeHandler {

        public WhereHandler() {
            // Prevent Synthetic Access
        }

        @Override
        public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
            List<SqlNode> contents = parseDynamicTags(nodeToHandle);
            MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
            WhereSqlNode where = new WhereSqlNode(configuration, mixedSqlNode);
            targetContents.add(where);
        }
    }

    /**
     * <set>
     * <if test="username != null">username=#{username},</if>
     * <if test="password != null">password=#{password},</if>
     * <if test="email != null">email=#{email},</if>
     * <if test="bio != null">bio=#{bio}</if>
     * </set>
     */
    private class SetHandler implements NodeHandler {

        public SetHandler() {
            // Prevent Synthetic Access
        }

        @Override
        public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
            List<SqlNode> contents = parseDynamicTags(nodeToHandle);
            MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
            SetSqlNode set = new SetSqlNode(configuration, mixedSqlNode);
            targetContents.add(set);
        }
    }

    /**
     * <foreach item="item" index="index" collection="list" open="(" close=")" separator=",">
     * <if test="index != 0">,</if> #{item}
     * </foreach>
     */
    private class ForEachHandler implements NodeHandler {

        public ForEachHandler() {
            // Prevent Synthetic Access
        }

        @Override
        public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
            List<SqlNode> contents = parseDynamicTags(nodeToHandle);
            MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
            String collection = nodeToHandle.getStringAttribute("collection");
            String item = nodeToHandle.getStringAttribute("item");
            String index = nodeToHandle.getStringAttribute("index");
            String open = nodeToHandle.getStringAttribute("open");
            String close = nodeToHandle.getStringAttribute("close");
            String separator = nodeToHandle.getStringAttribute("separator");
            ForEachSqlNode forEachSqlNode = new ForEachSqlNode(configuration, mixedSqlNode, collection, index, item, open, close, separator);
            targetContents.add(forEachSqlNode);
        }
    }

    private class IfHandler implements NodeHandler {

        public IfHandler() {
            // Prevent Synthetic Access
        }

        @Override
        public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
            List<SqlNode> contents = parseDynamicTags(nodeToHandle);//动态标签内部还可能会嵌套标签，也可能是一段带占位符的sql
            MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
            String test = nodeToHandle.getStringAttribute("test");//<if test="id != null">,此处拿到test表达式
            IfSqlNode ifSqlNode = new IfSqlNode(mixedSqlNode, test);
            targetContents.add(ifSqlNode);
        }
    }

    /**
     * <where>
     * <choose>
     * <when test="id != null">id = #{id}</when>
     * <when test="author_id != null">AND author_id = #{author_id}</when>
     * <otherwise>
     * <if test="ids != null">
     * AND id IN
     * <foreach item="item_id" index="index" open="(" close=")" separator="," collection="ids">#{ids[${index}]}
     * </foreach>
     * </if>
     * <trim prefix="AND">
     * <include refid="byBlogId"/>
     * </trim>
     * </otherwise>
     * </choose>
     * </where>
     */
    private class OtherwiseHandler implements NodeHandler {

        public OtherwiseHandler() {
            // Prevent Synthetic Access
        }

        @Override
        public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
            List<SqlNode> contents = parseDynamicTags(nodeToHandle);
            MixedSqlNode mixedSqlNode = new MixedSqlNode(contents);
            targetContents.add(mixedSqlNode);
        }
    }

    /**
     * <where>
     * <choose>
     * <when test="id != null">id = #{id}</when>
     * <when test="author_id != null">AND author_id = #{author_id}</when>
     * <otherwise>
     * <if test="ids != null">
     * AND id IN
     * <foreach item="item_id" index="index" open="(" close=")" separator="," collection="ids">#{ids[${index}]}
     * </foreach>
     * </if>
     * <trim prefix="AND">
     * <include refid="byBlogId"/>
     * </trim>
     * </otherwise>
     * </choose>
     * </where>
     */
    private class ChooseHandler implements NodeHandler {

        public ChooseHandler() {
            // Prevent Synthetic Access
        }

        @Override
        public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
            List<SqlNode> whenSqlNodes = new ArrayList<SqlNode>();
            List<SqlNode> otherwiseSqlNodes = new ArrayList<SqlNode>();
            handleWhenOtherwiseNodes(nodeToHandle, whenSqlNodes, otherwiseSqlNodes);
            SqlNode defaultSqlNode = getDefaultSqlNode(otherwiseSqlNodes);
            ChooseSqlNode chooseSqlNode = new ChooseSqlNode(whenSqlNodes, defaultSqlNode);
            targetContents.add(chooseSqlNode);
        }

        private void handleWhenOtherwiseNodes(XNode chooseSqlNode, List<SqlNode> ifSqlNodes, List<SqlNode> defaultSqlNodes) {
            List<XNode> children = chooseSqlNode.getChildren();
            for (XNode child : children) {
                String nodeName = child.getNode().getNodeName();
                NodeHandler handler = nodeHandlers(nodeName);
                if (handler instanceof IfHandler) {
                    handler.handleNode(child, ifSqlNodes);
                } else if (handler instanceof OtherwiseHandler) {
                    handler.handleNode(child, defaultSqlNodes);
                }
            }
        }

        private SqlNode getDefaultSqlNode(List<SqlNode> defaultSqlNodes) {
            SqlNode defaultSqlNode = null;
            if (defaultSqlNodes.size() == 1) {
                defaultSqlNode = defaultSqlNodes.get(0);
            } else if (defaultSqlNodes.size() > 1) {
                throw new BuilderException("Too many default (otherwise) elements in choose statement.");
            }
            return defaultSqlNode;
        }
    }
}
