package org.apache.ibatis.builder;

import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;

import java.util.List;

/**
 * 结果映射解析器
 *
 * @author Eduardo Macarron
 */
public class ResultMapResolver {

    private final MapperBuilderAssistant assistant;

    private String id;

    private Class<?> type;

    private String extend;

    private Discriminator discriminator;

    private List<ResultMapping> resultMappings;

    private Boolean autoMapping;

    /**
     * @param assistant
     * @param id resultMap 的 id
     * @param type resultMap 的 type
     * @param extend
     * @param discriminator
     * @param resultMappings resultMap 的具体映射字段列表
     * @param autoMapping
     */
    public ResultMapResolver(MapperBuilderAssistant assistant, String id, Class<?> type,
            String extend, Discriminator discriminator, List<ResultMapping> resultMappings, Boolean autoMapping) {
        this.assistant = assistant;
        this.id = id;
        this.type = type;
        this.extend = extend;
        this.discriminator = discriminator;
        this.resultMappings = resultMappings;
        this.autoMapping = autoMapping;
    }

    public ResultMap resolve() {
        //解析又去调用MapperBuilderAssistant.addResultMap
        return assistant.addResultMap(this.id, this.type, this.extend, this.discriminator, this.resultMappings, this.autoMapping);
    }

}