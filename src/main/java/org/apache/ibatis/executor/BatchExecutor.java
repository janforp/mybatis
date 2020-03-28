package org.apache.ibatis.executor;

import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 进行批量操作，通过批量操作来提高性能
 *
 * @author Jeff Butler
 */
public class BatchExecutor extends BaseExecutor {

    public static final int BATCH_UPDATE_RETURN_VALUE = Integer.MIN_VALUE + 1002;

    /**
     * Statement链表
     **/
    private final List<Statement> statementList = new ArrayList<Statement>();

    /**
     * batch结果链表
     **/
    private final List<BatchResult> batchResultList = new ArrayList<BatchResult>();

    /**
     * 当前sql
     */
    private String currentSql;

    /**
     * 当前 MappedStatement
     */
    private MappedStatement currentStatement;

    public BatchExecutor(Configuration configuration, Transaction transaction) {
        super(configuration, transaction);
    }

    @Override
    public int doUpdate(MappedStatement mappedStatement, Object parameterObject) throws SQLException {
        final Configuration configuration = mappedStatement.getConfiguration();
        final StatementHandler handler = configuration.newStatementHandler(this, mappedStatement, parameterObject, RowBounds.DEFAULT, null, null);
        final BoundSql boundSql = handler.getBoundSql();
        final String sql = boundSql.getSql();
        final Statement statement;
        if (sql.equals(currentSql) && mappedStatement.equals(currentStatement)) {
            int last = statementList.size() - 1;
            statement = statementList.get(last);
            BatchResult batchResult = batchResultList.get(last);
            batchResult.addParameterObject(parameterObject);
        } else {
            //获取同一个事务中的连接
            Connection connection = getConnection(mappedStatement.getStatementLog());
            statement = handler.prepare(connection);
            currentSql = sql;
            currentStatement = mappedStatement;
            statementList.add(statement);
            batchResultList.add(new BatchResult(mappedStatement, sql, parameterObject));
        }
        handler.parameterize(statement);
        handler.batch(statement);
        return BATCH_UPDATE_RETURN_VALUE;
    }

    @Override
    public <E> List<E> doQuery(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        Statement stmt = null;
        try {
            flushStatements();
            Configuration configuration = ms.getConfiguration();
            StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameterObject, rowBounds, resultHandler, boundSql);
            Connection connection = getConnection(ms.getStatementLog());
            stmt = handler.prepare(connection);
            handler.parameterize(stmt);
            return handler.query(stmt, resultHandler);
        } finally {
            closeStatement(stmt);
        }
    }

    @Override
    public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
        try {
            List<BatchResult> results = new ArrayList<BatchResult>();
            if (isRollback) {
                return Collections.emptyList();
            }
            for (int i = 0, n = statementList.size(); i < n; i++) {
                Statement statement = statementList.get(i);
                BatchResult batchResult = batchResultList.get(i);
                try {
                    //执行批处理
                    //an array of update counts containing one element for each
                    //command in the batch.  The elements of the array are ordered according
                    //to the order in which commands were added to the batch.
                    int[] executeBatch = statement.executeBatch();
                    batchResult.setUpdateCounts(executeBatch);
                    MappedStatement mappedStatement = batchResult.getMappedStatement();
                    //参数
                    List<Object> parameterObjects = batchResult.getParameterObjects();
                    KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();

                    Class<? extends KeyGenerator> keyGeneratorClass = keyGenerator.getClass();
                    if (Jdbc3KeyGenerator.class.equals(keyGeneratorClass)) {
                        Jdbc3KeyGenerator jdbc3KeyGenerator = (Jdbc3KeyGenerator) keyGenerator;
                        jdbc3KeyGenerator.processBatch(mappedStatement, statement, parameterObjects);
                    } else if (!NoKeyGenerator.class.equals(keyGeneratorClass)) { //issue #141
                        for (Object parameter : parameterObjects) {
                            keyGenerator.processAfter(this, mappedStatement, statement, parameter);
                        }
                    }
                } catch (BatchUpdateException e) {
                    StringBuilder message = new StringBuilder();
                    message.append(batchResult.getMappedStatement().getId())
                            .append(" (batch index #")
                            .append(i + 1)
                            .append(")")
                            .append(" failed.");
                    if (i > 0) {
                        message.append(" ")
                                .append(i)
                                .append(" prior sub executor(s) completed successfully, but will be rolled back.");
                    }
                    throw new BatchExecutorException(message.toString(), e, results, batchResult);
                }
                results.add(batchResult);
            }
            return results;
        } finally {
            for (Statement stmt : statementList) {
                closeStatement(stmt);
            }
            currentSql = null;
            statementList.clear();
            batchResultList.clear();
        }
    }
}
