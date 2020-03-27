

package org.apache.ibatis.builder.xml;

import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
        if ("include".equals(source.getNodeName())) {
            //走到这里，单独解析<include refid="userColumns"/>
            //拿到SQL片段
            String refid = getStringAttribute(source, "refid");
            Node toInclude = findSqlFragment(refid);
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
        } else if (source.getNodeType() == Node.ELEMENT_NODE) {
            //一开始会走这段，取得所有儿子
            NodeList children = source.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                //递归调用自己
                applyIncludes(children.item(i));
            }
        }
    }

    private Node findSqlFragment(String refid) {
        refid = PropertyParser.parse(refid, configuration.getVariables());
        refid = builderAssistant.applyCurrentNamespace(refid, true);
        try {
            //去之前存到内存map的SQL片段中寻找
            XNode nodeToInclude = configuration.getSqlFragments().get(refid);
            //clone一下，以防改写？
            return nodeToInclude.getNode().cloneNode(true);
        } catch (IllegalArgumentException e) {
            throw new IncompleteElementException("Could not find SQL statement to include with refid '" + refid + "'", e);
        }
    }

    private String getStringAttribute(Node node, String name) {
        return node.getAttributes().getNamedItem(name).getNodeValue();
    }
}
