package org.apache.ibatis.builder.xml;

import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Map;
import java.util.Properties;

/**
 * XML include转换器
 * select <include refid="Base_Column_List"/>
 *
 * @author Frank D. Martinez [mnesarco]
 */
public class XMLIncludeTransformer {

    private final Configuration configuration;

    private final MapperBuilderAssistant builderAssistant;

    public XMLIncludeTransformer(Configuration configuration, MapperBuilderAssistant builderAssistant) {
        this.configuration = configuration;
        this.builderAssistant = builderAssistant;
    }

    //<select id="selectUsers" resultType="map">
    //  select <include refid="userColumns"/>
    //  from some_table
    //  where id = #{id}
    //</select>
    public void applyIncludes(Node source) {
        String nodeName = source.getNodeName();
        short nodeType = source.getNodeType();
        if ("include".equals(nodeName)) {
            //走到这里，单独解析<include refid="userColumns"/>
            //拿到SQL片段
            String refId = getStringAttribute(source, "refid");
            Node toInclude = findSqlFragment(refId);
            //递归调用自己,应用上
            // <sql id="Base_Column_List">
            //        <include refid="Id_name"/>,
            //        config_id, config_key, config_value,config_start_time, config_end_time, location_code,channel
            //    </sql>
            applyIncludes(toInclude);
            //总之下面就是将字符串拼接进来，看不懂。。。
            if (toInclude.getOwnerDocument() != source.getOwnerDocument()) {
                toInclude = source.getOwnerDocument().importNode(toInclude, true);
            }
            source.getParentNode().replaceChild(toInclude, source);
            while (toInclude.hasChildNodes()) {
                toInclude.getParentNode().insertBefore(toInclude.getFirstChild(), toInclude);
            }
            toInclude.getParentNode().removeChild(toInclude);
        } else if (nodeType == Node.ELEMENT_NODE) {
            //一开始会走这段，取得所有儿子
            NodeList children = source.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                //递归调用自己
                Node item = children.item(i);
                applyIncludes(item);
            }
        }
    }

    private Node findSqlFragment(String refId) {
        Properties configurationVariables = configuration.getVariables();
        //${}占位符的直接替换，输入字符串 (name = ${username}),可能会输出(name = 张三)，当然映射中要有 key=username,value=张三
        refId = PropertyParser.parse(refId, configurationVariables);
        refId = builderAssistant.applyCurrentNamespace(refId, true);
        try {
            //去之前存到内存map的SQL片段中寻找
            Map<String, XNode> sqlFragments = configuration.getSqlFragments();
            XNode nodeToInclude = sqlFragments.get(refId);
            //clone一下，以防改写？
            Node nodeToIncludeNode = nodeToInclude.getNode();
            Node cloneNode = nodeToIncludeNode.cloneNode(true);
            return cloneNode;
        } catch (IllegalArgumentException e) {
            throw new IncompleteElementException("Could not find SQL statement to include with refId '" + refId + "'", e);
        }
    }

    private String getStringAttribute(Node node, String name) {
        return node.getAttributes().getNamedItem(name).getNodeValue();
    }
}
