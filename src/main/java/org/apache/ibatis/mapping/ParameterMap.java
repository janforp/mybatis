package org.apache.ibatis.mapping;

import lombok.Getter;
import org.apache.ibatis.session.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * @author Clinton Begin
 */
public class ParameterMap {

    @Getter
    private String id;

    @Getter
    private Class<?> type;

    /**
     * #{property,javaType=int,jdbcType=NUMERIC}
     */
    @Getter
    private List<ParameterMapping> parameterMappings;

    private ParameterMap() {
    }

    /**
     * 用于构建 ParameterMap
     */
    public static class Builder {

        /**
         * 构造器初始化该对象属性
         */
        private ParameterMap parameterMap = new ParameterMap();

        public Builder(Configuration configuration, String id, Class<?> type, List<ParameterMapping> parameterMappings) {
            parameterMap.id = id;
            parameterMap.type = type;
            parameterMap.parameterMappings = parameterMappings;
        }

        public Class<?> type() {
            return parameterMap.type;
        }

        /**
         * 对外提供实例
         *
         * @return 对外提供实例
         */
        public ParameterMap build() {
            //lock down collections
            parameterMap.parameterMappings = Collections.unmodifiableList(parameterMap.parameterMappings);
            return parameterMap;
        }
    }
}
