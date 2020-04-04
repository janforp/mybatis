package org.apache.ibatis.parsing;

import lombok.Getter;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Clinton Begin
 */

/**
 * 对org.w3c.dom.Node的包装
 */
public class XNode {

    //org.w3c.dom.Node
    @Getter
    private Node node;

    //以下都是预先把信息都解析好，放到map等数据结构中（内存中）
    @Getter
    private String name;

    private String body;

    /**
     * <select id="getUser" resultType="org.apache.ibatis.submitted.propertiesinmapperfiles.User">
     * 就就得到:id : getUser 以及 resultType:org.apache.ibatis.submitted.propertiesinmapperfiles.User 2个键值对
     * {"resultType":"org.apache.ibatis.submitted.propertiesinmapperfiles.User","id":"getUser"}
     */
    private Properties attributes;

    private Properties variables;

    //XPathParser方便xpath解析
    private XPathParser xpathParser;

    //在构造时就把一些信息（属性，body）全部解析好，以便我们直接通过getter函数取得
    public XNode(XPathParser xpathParser, Node node, Properties variables) {
        this.xpathParser = xpathParser;
        this.node = node;
        this.name = node.getNodeName();
        this.variables = variables;
        this.attributes = parseAttributes(node);
        this.body = parseBody(node);
    }

    public XNode newXNode(Node node) {
        return new XNode(xpathParser, node, variables);
    }

    public XNode getParent() {
        //调用Node.getParentNode,如果取到，包装一下，返回XNode
        Node parent = node.getParentNode();
        if (parent == null || !(parent instanceof Element)) {
            return null;
        } else {
            return new XNode(xpathParser, parent, variables);
        }
    }

    /**
     * 取得完全的path (a/b/c)
     *
     * @return 若传入的是 id，则得到的字符串：employees/employee/department/id
     */
    public String getPath() {
        //循环依次取得节点的父节点，然后倒序打印,也可以用一个堆栈实现
        StringBuilder builder = new StringBuilder();
        Node current = node;
        while (current != null && current instanceof Element) {
            if (current != node) {
                //在字符串的开头处添加一个 "/"
                builder.insert(0, "/");
            }
            builder.insert(0, current.getNodeName());
            current = current.getParentNode();
        }
        return builder.toString();
    }

    //取得标示符   ("resultMap[authorResult]")
    //XMLMapperBuilder.resultMapElement调用
    //	<resultMap id="authorResult" type="Author">
    //	  <id property="id" column="author_id"/>
    //	  <result property="username" column="author_username"/>
    //	  <result property="password" column="author_password"/>
    //	  <result property="email" column="author_email"/>
    //	  <result property="bio" column="author_bio"/>
    //	</resultMap>
    public String getValueBasedIdentifier() {
        //TODO
        StringBuilder builder = new StringBuilder();
        XNode current = this;
        while (current != null) {
            if (current != this) {
                //在字符串的前面添加一个 "_"
                builder.insert(0, "_");
            }
            //先拿id，拿不到再拿value,再拿不到拿property
            String value = current.getStringAttribute("id",
                    current.getStringAttribute("value",
                            current.getStringAttribute("property", null)));
            if (value != null) {
                value = value.replace('.', '_');
                builder.insert(0, "]");
                builder.insert(0,
                        value);
                builder.insert(0, "[");
            }
            builder.insert(0, current.getName());
            current = current.getParent();
        }
        return builder.toString();
    }

    //以下方法都是把XPathParser的方法再重复一遍
    public String evalString(String expression) {
        return xpathParser.evalString(node, expression);
    }

    public Boolean evalBoolean(String expression) {
        return xpathParser.evalBoolean(node, expression);
    }

    public Double evalDouble(String expression) {
        return xpathParser.evalDouble(node, expression);
    }

    public List<XNode> evalNodes(String expression) {
        return xpathParser.evalNodes(node, expression);
    }

    public XNode evalNode(String expression) {
        return xpathParser.evalNode(node, expression);
    }

    //以下是一些getBody的方法
    public String getStringBody() {
        return getStringBody(null);
    }

    public String getStringBody(String def) {
        if (body == null) {
            return def;
        } else {
            return body;
        }
    }

    public Boolean getBooleanBody() {
        return getBooleanBody(null);
    }

    public Boolean getBooleanBody(Boolean def) {
        if (body == null) {
            return def;
        } else {
            return Boolean.valueOf(body);
        }
    }

    public Integer getIntBody() {
        return getIntBody(null);
    }

    public Integer getIntBody(Integer def) {
        if (body == null) {
            return def;
        } else {
            return Integer.parseInt(body);
        }
    }

    public Long getLongBody() {
        return getLongBody(null);
    }

    public Long getLongBody(Long def) {
        if (body == null) {
            return def;
        } else {
            return Long.parseLong(body);
        }
    }

    public Double getDoubleBody() {
        return getDoubleBody(null);
    }

    public Double getDoubleBody(Double def) {
        if (body == null) {
            return def;
        } else {
            return Double.parseDouble(body);
        }
    }

    public Float getFloatBody() {
        return getFloatBody(null);
    }

    public Float getFloatBody(Float def) {
        if (body == null) {
            return def;
        } else {
            return Float.parseFloat(body);
        }
    }

    //以下是一些getAttribute的方法
    public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name) {
        return getEnumAttribute(enumType, name, null);
    }

    public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name, T def) {
        String value = getStringAttribute(name);
        if (value == null) {
            return def;
        } else {
            return Enum.valueOf(enumType, value);
        }
    }

    public String getStringAttribute(String name) {
        return getStringAttribute(name, null);
    }

    /**
     * 获取字符串类型的属性
     * Properties attributes;
     *
     * @param name 属性名称
     * @param def 如何根据name获取不到，则返回该默认值
     * @return 获取字符串类型的属性
     */
    public String getStringAttribute(String name, String def) {
        String value = attributes.getProperty(name);
        if (value == null) {
            return def;
        } else {
            return value;
        }
    }

    public Boolean getBooleanAttribute(String name) {
        return getBooleanAttribute(name, null);
    }

    public Boolean getBooleanAttribute(String name, Boolean def) {
        String value = attributes.getProperty(name);
        if (value == null) {
            return def;
        } else {
            return Boolean.valueOf(value);
        }
    }

    public Integer getIntAttribute(String name) {
        return getIntAttribute(name, null);
    }

    public Integer getIntAttribute(String name, Integer def) {
        String value = attributes.getProperty(name);
        if (value == null) {
            return def;
        } else {
            return Integer.parseInt(value);
        }
    }

    public Long getLongAttribute(String name) {
        return getLongAttribute(name, null);
    }

    public Long getLongAttribute(String name, Long def) {
        String value = attributes.getProperty(name);
        if (value == null) {
            return def;
        } else {
            return Long.parseLong(value);
        }
    }

    public Double getDoubleAttribute(String name) {
        return getDoubleAttribute(name, null);
    }

    public Double getDoubleAttribute(String name, Double def) {
        String value = attributes.getProperty(name);
        if (value == null) {
            return def;
        } else {
            return Double.parseDouble(value);
        }
    }

    public Float getFloatAttribute(String name) {
        return getFloatAttribute(name, null);
    }

    public Float getFloatAttribute(String name, Float def) {
        String value = attributes.getProperty(name);
        if (value == null) {
            return def;
        } else {
            return Float.parseFloat(value);
        }
    }

    //得到孩子，原理是调用Node.getChildNodes
    public List<XNode> getChildren() {
        List<XNode> children = new ArrayList<XNode>();
        NodeList nodeList = node.getChildNodes();
        if (nodeList == null) {
            return children;
        }

        int nodeListLength = nodeList.getLength();
        for (int i = 0, n = nodeListLength; i < n; i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                children.add(new XNode(xpathParser, node, variables));
            }
        }
        return children;
    }

    //得到孩子，返回Properties，孩子的格式肯定都有name,value属性
    public Properties getChildrenAsProperties() {
        Properties properties = new Properties();
        for (XNode child : getChildren()) {
            String name = child.getStringAttribute("name");
            String value = child.getStringAttribute("value");
            if (name != null && value != null) {
                properties.setProperty(name, value);
            }
        }
        return properties;
    }

    //打印信息，为了调试用
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<");
        builder.append(name);
        for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
            builder.append(" ");
            builder.append(entry.getKey());
            builder.append("=\"");
            builder.append(entry.getValue());
            builder.append("\"");
        }
        List<XNode> children = getChildren();
        if (!children.isEmpty()) {
            builder.append(">\n");
            for (XNode node : children) {
                //递归取得孩子的toString
                builder.append(node.toString());
            }
            builder.append("</");
            builder.append(name);
            builder.append(">");
        } else if (body != null) {
            builder.append(">");
            builder.append(body);
            builder.append("</");
            builder.append(name);
            builder.append(">");
        } else {
            builder.append("/>");
        }
        builder.append("\n");
        return builder.toString();
    }

    //以下2个方法在构造时就解析
    private Properties parseAttributes(Node node) {
        Properties attributes = new Properties();
        NamedNodeMap attributeNodes = node.getAttributes();
        if (attributeNodes == null) {
            return attributes;
        }

        int attributeNodesLength = attributeNodes.getLength();
        for (int i = 0; i < attributeNodesLength; i++) {
            Node attribute = attributeNodes.item(i);
            String nodeValue = attribute.getNodeValue();
            //输入字符串 (name = ${username}),可能会输出(name = 张三)，当然映射中要有 key=username,value=张三
            String value = PropertyParser.parse(nodeValue, variables);
            String attributeNodeName = attribute.getNodeName();
            attributes.put(attributeNodeName, value);
        }
        return attributes;
    }

    private String parseBody(Node node) {
        //取不到body，循环取孩子的body，只要取到第一个，立即返回
        String data = getBodyData(node);
        if (data == null) {
            NodeList children = node.getChildNodes();
            int childrenLength = children.getLength();
            for (int i = 0; i < childrenLength; i++) {
                Node child = children.item(i);
                data = getBodyData(child);
                if (data != null) {
                    break;
                }
            }
        }
        return data;
    }

    private String getBodyData(Node child) {
        short nodeType = child.getNodeType();
        if (nodeType != Node.CDATA_SECTION_NODE && nodeType != Node.TEXT_NODE) {
            return null;
        }
        String data = ((CharacterData) child).getData();
        //输入字符串 (name = ${username}),可能会输出(name = 张三)，当然映射中要有 key=username,value=张三
        data = PropertyParser.parse(data, variables);
        return data;
    }

}