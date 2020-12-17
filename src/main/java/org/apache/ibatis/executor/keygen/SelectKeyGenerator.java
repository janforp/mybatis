package org.apache.ibatis.executor.keygen;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

import java.sql.Statement;
import java.util.List;

/**
 * 典型的命令模式设计
 *
 * <selectKey keyProperty="nameId,generatedName" order="AFTER" resultType="org.apache.ibatis.submitted.selectkey.Name">
 * select id as nameId, name_fred as generatedName from table2 where id = identity()
 * </selectKey>
 * 的情况使用该方法
 *
 * 而 sql:<insert id="insertTable2WithGeneratedKeyXml" useGeneratedKeys="true" keyProperty="nameId,generatedName" keyColumn="ID,NAME_FRED"> 的情况使用该方法
 * 使用 Jdbc3KeyGenerator
 *
 * @author Clinton Begin
 * @author Jeff Butler
 */
public class SelectKeyGenerator implements KeyGenerator {

    public static final String SELECT_KEY_SUFFIX = "!selectKey";

    private boolean executeBefore;

    private MappedStatement keyStatement;

    public SelectKeyGenerator(MappedStatement keyStatement, boolean executeBefore) {
        this.executeBefore = executeBefore;
        this.keyStatement = keyStatement;
    }

    @Override
    public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        if (executeBefore) {
            processGeneratedKeys(executor, ms, parameter);
        }
    }

    @Override
    public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        if (!executeBefore) {
            processGeneratedKeys(executor, ms, parameter);
        }
    }

    private void processGeneratedKeys(Executor executor, MappedStatement mappedStatement, Object parameter) {
        try {
            if (parameter == null || keyStatement == null || keyStatement.getKeyProperties() == null) {
                return;
            }
            String[] keyProperties = keyStatement.getKeyProperties();
            if (keyProperties == null) {
                return;
            }
            final Configuration configuration = mappedStatement.getConfiguration();
            final MetaObject metaParam = configuration.newMetaObject(parameter);
            // Do not close keyExecutor.
            // The transaction will be closed by parent executor.
            Transaction transaction = executor.getTransaction();
            Executor keyExecutor = configuration.newExecutor(transaction, ExecutorType.SIMPLE);
            //插入的时候对象带主键的原理：其实是mybatis去查询了一次
            List<Object> values = keyExecutor.query(keyStatement, parameter, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER);
            int resultListSize = values.size();
            if (resultListSize == 0) {
                //没有返回数据-报错
                throw new ExecutorException("SelectKey returned no data.");
            }
            if (resultListSize > 1) {
                //返回数组多余1条-报错
                throw new ExecutorException("SelectKey returned more than one value.");
            }

            Object resultObject = values.get(0);
            MetaObject metaResult = configuration.newMetaObject(resultObject);
            //单个的情况
            if (keyProperties.length == 1) {

                String keyProperty = keyProperties[0];

                if (metaResult.hasGetter(keyProperty)) {

                    //如果返回的结果是一个对，且还有getter，则把 keyProperty 属性get出来，就是主键了
                    Object metaResultValue = metaResult.getValue(keyProperty);
                    //把主键塞入参数中
                    setValue(metaParam, keyProperty, metaResultValue);

                } else {

                    //否则返回的结果本身就是主键，直接塞进去即可
                    // no getter for the property - maybe just a single value object
                    // so try that
                    setValue(metaParam, keyProperty, resultObject);
                }
            } else {

                //keyProperties.length > 1)
                //处理多个的情况
                handleMultipleProperties(keyProperties, metaParam, metaResult);
            }
        } catch (ExecutorException e) {
            throw e;
        } catch (Exception e) {
            throw new ExecutorException("Error selecting key or setting result to parameter object. Cause: " + e, e);
        }
    }

    /**
     * 当主键是多个，用逗号隔开
     *
     * @param keyProperties id,sex
     * @param metaParam 入参数对象
     * @param metaResult 主键sql返回对象
     */
    private void handleMultipleProperties(String[] keyProperties, MetaObject metaParam, MetaObject metaResult) {
        String[] keyColumns = keyStatement.getKeyColumns();
        if (keyColumns == null || keyColumns.length == 0) {

            // no key columns specified, just use the property names
            for (String keyProperty : keyProperties) {
                setValue(metaParam, keyProperty, metaResult.getValue(keyProperty));
            }
            return;
        }

        //keyColumns 不为空的时候
        int keyColumnLength = keyColumns.length;
        int keyPropertiesLength = keyProperties.length;

        //主键sql中 column 跟 properties 的数量要一样
        if (keyColumnLength != keyPropertiesLength) {
            //数量不匹配-报错
            throw new ExecutorException("If SelectKey has key columns, the number must match the number of key properties.");
        }

        //主键sql中 column 跟 properties 的数量一样的时候，就可以赋值
        for (int i = 0; i < keyPropertiesLength; i++) {
            //数量匹配-赋值
            Object metaResultValue = metaResult.getValue(keyColumns[i]);
            setValue(metaParam, keyProperties[i], metaResultValue);
        }
    }

    private void setValue(MetaObject metaParam, String property, Object value) {
        if (metaParam.hasSetter(property)) {
            metaParam.setValue(property, value);
        } else {
            throw new ExecutorException("No setter found for the keyProperty '" + property + "' in " + metaParam.getOriginalObject().getClass().getName() + ".");
        }
    }
}
