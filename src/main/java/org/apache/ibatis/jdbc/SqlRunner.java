package org.apache.ibatis.jdbc;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Clinton Begin
 */

/**
 * SQL运行器,可以运行SQL，如select，作为单元测试的正式测试
 * 这个类其实可以被所有项目的单元测试作为工具所利用
 */
public class SqlRunner {

    /**
     * 没有自增主键的时候返回的值
     */
    public static final int NO_GENERATED_KEY = Integer.MIN_VALUE + 1001;

    private Connection connection;

    private TypeHandlerRegistry typeHandlerRegistry;

    /**
     * 是否使用自增主键
     */
    private boolean useGeneratedKeySupport;

    public SqlRunner(Connection connection) {
        this.connection = connection;
        this.typeHandlerRegistry = new TypeHandlerRegistry();
    }

    public void setUseGeneratedKeySupport(boolean useGeneratedKeySupport) {
        this.useGeneratedKeySupport = useGeneratedKeySupport;
    }

    /**
     * Executes a SELECT statement that returns one row.
     * 其实调用的是 java.sql.PreparedStatement#executeQuery()
     *
     * @param sql The SQL
     * @param args The arguments to be set on the statement.
     * @return The row expected.
     * @throws SQLException If less or more than one row is returned
     */
    public Map<String, Object> selectOne(String sql, Object... args) throws SQLException {
        List<Map<String, Object>> results = selectAll(sql, args);
        if (results.size() != 1) {
            throw new SQLException("Statement returned " + results.size() + " results where exactly one (1) was expected.");
        }
        return results.get(0);
    }

    /**
     * Executes a SELECT statement that returns multiple rows.
     *
     * @param sql The SQL
     * @param args The arguments to be set on the statement.
     * @return The list of rows expected.
     * @throws SQLException If statement preparation or execution fails
     */
    public List<Map<String, Object>> selectAll(String sql, Object... args) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql);
        try {
            setParameters(ps, args);
            ResultSet rs = ps.executeQuery();
            return getResults(rs);
        } finally {
            try {
                ps.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }

    /*
     * Executes an INSERT statement.
     *
     * @param sql  The SQL
     * @param args The arguments to be set on the statement.
     * @return The number of rows impacted or BATCHED_RESULTS if the statements are being batched.
     * @throws SQLException If statement preparation or execution fails
     */
    public int insert(String sql, Object... args) throws SQLException {
        PreparedStatement ps;
        if (useGeneratedKeySupport) {
            ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        } else {
            ps = connection.prepareStatement(sql);
        }

        try {
            setParameters(ps, args);
            ps.executeUpdate();
            if (useGeneratedKeySupport) {
                //获取自增主键结果
                ResultSet generatedKeysResultSet = ps.getGeneratedKeys();
                List<Map<String, Object>> keys = getResults(generatedKeysResultSet);
                if (keys.size() == 1) {
                    Map<String, Object> key = keys.get(0);
                    Iterator<Object> i = key.values().iterator();
                    if (i.hasNext()) {
                        Object genkey = i.next();
                        if (genkey != null) {
                            try {
                                return Integer.parseInt(genkey.toString());
                            } catch (NumberFormatException e) {
                                //ignore, no numeric key suppot
                            }
                        }
                    }
                }
            }
            return NO_GENERATED_KEY;
        } finally {
            try {
                ps.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }

    /**
     * 修改sql以及参数
     * Executes an UPDATE statement.
     *
     * @param sql The SQL
     * @param args The arguments to be set on the statement.
     * @return The number of rows impacted or BATCHED_RESULTS if the statements are being batched.
     * @throws SQLException If statement preparation or execution fails
     */
    public int update(String sql, Object... args) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql);
        try {
            setParameters(ps, args);
            return ps.executeUpdate();
        } finally {
            try {
                ps.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }

    /*
     * Executes a DELETE statement.
     *
     * @param sql  The SQL
     * @param args The arguments to be set on the statement.
     * @return The number of rows impacted or BATCHED_RESULTS if the statements are being batched.
     * @throws SQLException If statement preparation or execution fails
     */
    public int delete(String sql, Object... args) throws SQLException {
        return update(sql, args);
    }

    /*
     * Executes any string as a JDBC Statement.
     * Good for DDL
     *
     * @param sql The SQL
     * @throws SQLException If statement preparation or execution fails
     */
    public void run(String sql) throws SQLException {
        Statement stmt = connection.createStatement();
        try {
            stmt.execute(sql);
        } finally {
            try {
                stmt.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }

    public void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            //ignore
        }
    }

    /**
     * 设置参数 PreparedStatement 中的占位符
     *
     * @param ps PreparedStatement
     * @param args 参数
     * @throws SQLException
     */
    private void setParameters(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0, n = args.length; i < n; i++) {
            Object arg = args[i];
            //数据库的 null 值需要转换为特定的类型
            if (arg == null) {
                throw new SQLException("SqlRunner requires an instance of Null to represent typed null values for JDBC compatibility");
            } else if (arg instanceof Null) {
                ((Null) arg).getTypeHandler().setParameter(ps, i + 1, null, ((Null) arg).getJdbcType());
            } else {
                //巧妙的利用TypeHandler来设置参数
                Class<?> argClass = arg.getClass();
                TypeHandler typeHandler = typeHandlerRegistry.getTypeHandler(argClass);
                if (typeHandler == null) {
                    throw new SQLException("SqlRunner could not find a TypeHandler instance for " + argClass);
                } else {
                    typeHandler.setParameter(ps, i + 1, arg, null);
                }
            }
        }
    }

    /**
     * 取得结果
     *
     * @param rs 结果
     * @return 每一条记录就是list的每一个元素，每个元素记录着 key:column,value:该column的值
     * @throws SQLException
     */
    private List<Map<String, Object>> getResults(ResultSet rs) throws SQLException {
        try {
            //结果
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            //结果的 column
            List<String> columns = new ArrayList<String>();
            //类型处理器
            List<TypeHandler<?>> typeHandlers = new ArrayList<TypeHandler<?>>();
            //元数据
            ResultSetMetaData resultSetMetaData = rs.getMetaData();
            //先计算要哪些列，已经列的类型（TypeHandler）
            for (int i = 0, n = resultSetMetaData.getColumnCount(); i < n; i++) {
                String columnLabel = resultSetMetaData.getColumnLabel(i + 1);
                columns.add(columnLabel);
                try {
                    Class<?> type = Resources.classForName(resultSetMetaData.getColumnClassName(i + 1));
                    TypeHandler<?> typeHandler = typeHandlerRegistry.getTypeHandler(type);
                    if (typeHandler == null) {
                        typeHandler = typeHandlerRegistry.getTypeHandler(Object.class);
                    }
                    typeHandlers.add(typeHandler);
                } catch (Exception e) {
                    typeHandlers.add(typeHandlerRegistry.getTypeHandler(Object.class));
                }
            }
            while (rs.next()) {
                Map<String, Object> row = new HashMap<String, Object>();
                for (int i = 0, n = columns.size(); i < n; i++) {
                    String name = columns.get(i);
                    TypeHandler<?> handler = typeHandlers.get(i);
                    //巧妙的利用TypeHandler来取得结果
                    Object result = handler.getResult(rs, name);
                    row.put(name.toUpperCase(Locale.ENGLISH), result);
                }
                list.add(row);
            }
            return list;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

}
