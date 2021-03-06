package org.apache.ibatis.mapping;

import lombok.Getter;
import org.apache.ibatis.session.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * resultMap 元素是 MyBatis 中最重要最强大的元素。它可以让你从 90% 的 JDBC ResultSets
 * 数据提取代码中解放出来，并在一些情形下允许你进行一些 JDBC 不支持的操作。
 * 实际上，在为一些比如连接的复杂语句编写映射代码的时候，一份 resultMap 能够代替实现同等功能的数千行代码。
 * ResultMap 的设计思想是，对简单的语句做到零配置，对于复杂一点的语句，只需要描述语句之间的关系就行了。
 *
 * 结果映射
 * MyBatis 中最重要最强大的元素
 *
 * <resultMap id="BaseResultMap" type="cn.com.janita.employeecore.dao.account.dataobj.AccountLastChooseDO">
 * <result column="account_id" property="accountId" jdbcType="VARCHAR"/>
 * <result column="customer_id" property="customerId" jdbcType="BIGINT"/>
 * <result column="dept_id" property="deptId" jdbcType="INTEGER"/>
 * </resultMap>
 *
 * @author Clinton Begin
 */
public class ResultMap {

    @Getter
    private String id;

    @Getter
    private Class<?> type;

    @Getter
    private List<ResultMapping> resultMappings;

    @Getter // <id property="id" column="id"/>
    private List<ResultMapping> idResultMappings;

    @Getter //通过构造器构造的 resultMap
    private List<ResultMapping> constructorResultMappings;

    @Getter // 通过 getter/setter 构造的 resultMap
    private List<ResultMapping> propertyResultMappings;

    @Getter //resultMap 下的所有的 column 的大写形式的列表
    private Set<String> mappedColumns;

    @Getter
    private Discriminator discriminator;

    private boolean hasNestedResultMaps;

    private boolean hasNestedQueries;

    @Getter
    private Boolean autoMapping;

    private ResultMap() {
    }

    public boolean hasNestedResultMaps() {
        return hasNestedResultMaps;
    }

    public boolean hasNestedQueries() {
        return hasNestedQueries;
    }

    public void forceNestedResultMaps() {
        hasNestedResultMaps = true;
    }

    //静态内部类，建造者模式
    public static class Builder {

        private ResultMap resultMap = new ResultMap();

        public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings) {
            this(configuration, id, type, resultMappings, null);
        }

        public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings, Boolean autoMapping) {
            resultMap.id = id;
            resultMap.type = type;
            resultMap.resultMappings = resultMappings;
            resultMap.autoMapping = autoMapping;
        }

        public Builder discriminator(Discriminator discriminator) {
            resultMap.discriminator = discriminator;
            return this;
        }

        public Class<?> type() {
            return resultMap.type;
        }

        /**
         * 构造 ResultMap
         *
         * @return ResultMap
         */
        public ResultMap build() {
            if (resultMap.id == null) {//org.apache.ibatis.submitted.force_flush_on_select.PersonMapper.personMap
                throw new IllegalArgumentException("ResultMaps must have an id");
            }
            //resultMap 下的所有的 column 的大写形式的列表
            resultMap.mappedColumns = new HashSet<String>();
            // <id property="id" column="id"/>
            resultMap.idResultMappings = new ArrayList<ResultMapping>();
            //通过构造器构造的 resultMap
            resultMap.constructorResultMappings = new ArrayList<ResultMapping>();
            // 通过 getter/setter 构造的 resultMap
            resultMap.propertyResultMappings = new ArrayList<ResultMapping>();
            //该resultMap下的所有项如：<id property="id" column="id"/> <result property="firstName" column="firstName"/>
            List<ResultMapping> resultMappingList = resultMap.resultMappings;
            for (ResultMapping resultMapping : resultMappingList) {
                resultMap.hasNestedQueries = resultMap.hasNestedQueries || resultMapping.getNestedQueryId() != null;
                resultMap.hasNestedResultMaps = resultMap.hasNestedResultMaps || (resultMapping.getNestedResultMapId() != null && resultMapping.getResultSet() == null);
                final String column = resultMapping.getColumn();
                if (column != null) {
                    String upperColumn = column.toUpperCase(Locale.ENGLISH);
                    resultMap.mappedColumns.add(upperColumn);
                } else if (resultMapping.isCompositeResult()) {
                    for (ResultMapping compositeResultMapping : resultMapping.getComposites()) {
                        final String compositeColumn = compositeResultMapping.getColumn();
                        if (compositeColumn != null) {
                            resultMap.mappedColumns.add(compositeColumn.toUpperCase(Locale.ENGLISH));
                        }
                    }
                }
                if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
                    resultMap.constructorResultMappings.add(resultMapping);
                } else {
                    resultMap.propertyResultMappings.add(resultMapping);
                }
                if (resultMapping.getFlags().contains(ResultFlag.ID)) {
                    resultMap.idResultMappings.add(resultMapping);
                }
            }
            if (resultMap.idResultMappings.isEmpty()) {//TODO ？
                resultMap.idResultMappings.addAll(resultMap.resultMappings);
            }
            // lock down collections
            resultMap.resultMappings = Collections.unmodifiableList(resultMap.resultMappings);
            resultMap.idResultMappings = Collections.unmodifiableList(resultMap.idResultMappings);
            resultMap.constructorResultMappings = Collections.unmodifiableList(resultMap.constructorResultMappings);
            resultMap.propertyResultMappings = Collections.unmodifiableList(resultMap.propertyResultMappings);
            resultMap.mappedColumns = Collections.unmodifiableSet(resultMap.mappedColumns);
            return resultMap;
        }
    }
}
