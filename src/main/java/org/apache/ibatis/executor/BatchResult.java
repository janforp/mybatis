package org.apache.ibatis.executor;

import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.mapping.MappedStatement;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeff Butler
 */
public class BatchResult {

    @Getter
    private final MappedStatement mappedStatement;

    @Getter
    private final String sql;

    /**
     * 参数
     */
    @Getter
    private final List<Object> parameterObjects;

    /**
     * an array of update counts containing one element for each
     * command in the batch.  The elements of the array are ordered according
     * to the order in which commands were added to the batch.
     */
    @Setter
    @Getter
    private int[] updateCounts;

    public BatchResult(MappedStatement mappedStatement, String sql) {
        super();
        this.mappedStatement = mappedStatement;
        this.sql = sql;
        this.parameterObjects = new ArrayList<Object>();
    }

    public BatchResult(MappedStatement mappedStatement, String sql, Object parameterObject) {
        this(mappedStatement, sql);
        addParameterObject(parameterObject);
    }

    @Deprecated
    public Object getParameterObject() {
        return parameterObjects.get(0);
    }

    public void addParameterObject(Object parameterObject) {
        this.parameterObjects.add(parameterObject);
    }
}
