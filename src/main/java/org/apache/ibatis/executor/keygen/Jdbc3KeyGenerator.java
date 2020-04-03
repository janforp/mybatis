package org.apache.ibatis.executor.keygen;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC3键值生成器,核心是使用JDBC3的Statement.getGeneratedKeys
 *
 * sql:<insert id="insertTable2WithGeneratedKeyXml" useGeneratedKeys="true" keyProperty="nameId,generatedName" keyColumn="ID,NAME_FRED"> 的情况使用该方法
 *
 * @author Clinton Begin
 */
public class Jdbc3KeyGenerator implements KeyGenerator {

    @Override
    public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        // do nothing
    }

    @Override
    public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        List<Object> parameters = new ArrayList<Object>();
        parameters.add(parameter);
        processBatch(ms, stmt, parameters);
    }

    /**
     * 回写自增主键
     *
     * @param mappedStatement sql对应的 批处理
     * @param statement 与数据库交互的对象
     * @param parameters 插入对象，也即参数，主键就是要回写到该对象中，用户就可以直接拿来使用
     */
    public void processBatch(MappedStatement mappedStatement, Statement statement, List<Object> parameters) {
        ResultSet resultSet = null;
        try {
            //核心是使用JDBC3的Statement.getGeneratedKeys

            //执行sql拿主键
            resultSet = statement.getGeneratedKeys();
            final Configuration configuration = mappedStatement.getConfiguration();

            //类型处理注册机
            final TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            final String[] keyProperties = mappedStatement.getKeyProperties();
            final ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            TypeHandler<?>[] typeHandlers = null;
            if (keyProperties == null) {
                return;
            }

            int resultSetMetaDataColumnCount = resultSetMetaData.getColumnCount();
            int kePropertiesLen = keyProperties.length;
            boolean resultMoreThanKey = resultSetMetaDataColumnCount >= kePropertiesLen;
            if (!resultMoreThanKey) {
                return;
            }
            for (Object parameter : parameters) {
                // there should be one row for each statement (also one for each parameter)
                if (!resultSet.next()) {
                    break;
                }
                final MetaObject metaParam = configuration.newMetaObject(parameter);
                if (typeHandlers == null) {
                    //先取得类型处理器
                    typeHandlers = getTypeHandlers(typeHandlerRegistry, metaParam, keyProperties);
                }
                //填充键值
                populateKeys(resultSet, metaParam, keyProperties, typeHandlers);
            }

        } catch (Exception e) {
            throw new ExecutorException("Error getting generated key or setting result to parameter object. Cause: " + e, e);
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    private TypeHandler<?>[] getTypeHandlers(TypeHandlerRegistry typeHandlerRegistry, MetaObject metaParam, String[] keyProperties) {

        TypeHandler<?>[] typeHandlers = new TypeHandler<?>[keyProperties.length];

        for (int i = 0; i < keyProperties.length; i++) {

            String keyProperty = keyProperties[i];
            boolean hasSetter = metaParam.hasSetter(keyProperty);
            if (!hasSetter) {
                continue;
            }
            Class<?> keyPropertyType = metaParam.getSetterType(keyProperty);
            TypeHandler<?> th = typeHandlerRegistry.getTypeHandler(keyPropertyType);
            typeHandlers[i] = th;
        }
        return typeHandlers;
    }

    private void populateKeys(ResultSet resultSet, MetaObject metaParam, String[] keyProperties, TypeHandler<?>[] typeHandlers) throws SQLException {
        for (int i = 0; i < keyProperties.length; i++) {
            TypeHandler<?> typeHandler = typeHandlers[i];
            if (typeHandler == null) {
                continue;
            }
            Object value = typeHandler.getResult(resultSet, i + 1);
            String keyProperty = keyProperties[i];
            metaParam.setValue(keyProperty, value);
        }
    }
}
