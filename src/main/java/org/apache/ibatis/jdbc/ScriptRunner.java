package org.apache.ibatis.jdbc;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Clinton Begin
 */

/**
 * 脚本运行器,可以运行SQL脚本，如建表，插入数据，作为单元测试的前期准备
 * 这个类其实可以被所有项目的单元测试作为工具所利用
 */
public class ScriptRunner {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

    /**
     * sql语句的结束标志
     */
    private static final String DEFAULT_DELIMITER = ";";

    private Connection connection;

    /**
     * 如果true，则执行失败直接退出
     * 如果false,则执行失败打印日志
     */
    private boolean stopOnError;

    private boolean autoCommit;

    private boolean sendFullScript;

    /**
     * 是否需要把脚本sql中的 "\r\n" 转换为 "\n"
     */
    private boolean removeCRs;

    private boolean escapeProcessing = true;

    private PrintWriter logWriter = new PrintWriter(System.out);

    private PrintWriter errorLogWriter = new PrintWriter(System.err);

    private String delimiter = DEFAULT_DELIMITER;

    private boolean fullLineDelimiter = false;

    public ScriptRunner(Connection connection) {
        this.connection = connection;
    }

    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public void setSendFullScript(boolean sendFullScript) {
        this.sendFullScript = sendFullScript;
    }

    public void setRemoveCRs(boolean removeCRs) {
        this.removeCRs = removeCRs;
    }

    /**
     * @since 3.1.1
     */
    public void setEscapeProcessing(boolean escapeProcessing) {
        this.escapeProcessing = escapeProcessing;
    }

    public void setLogWriter(PrintWriter logWriter) {
        this.logWriter = logWriter;
    }

    public void setErrorLogWriter(PrintWriter errorLogWriter) {
        this.errorLogWriter = errorLogWriter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public void setFullLineDelimiter(boolean fullLineDelimiter) {
        this.fullLineDelimiter = fullLineDelimiter;
    }

    public void runScript(Reader reader) {
        setAutoCommit();
        try {
            if (sendFullScript) {
                executeFullScript(reader);
            } else {
                executeLineByLine(reader);
            }
        } finally {
            rollbackConnection();
        }
    }

    /**
     * 执行完整的脚本
     *
     * @param reader 脚本数据来源
     */
    private void executeFullScript(Reader reader) {
        StringBuilder script = new StringBuilder();
        try {
            BufferedReader lineReader = new BufferedReader(reader);
            String line;
            //一行一行的读，全部读完
            while ((line = lineReader.readLine()) != null) {
                script.append(line);
                //添加换行符
                script.append(LINE_SEPARATOR);
            }
            String fullScript = script.toString();
            //执行
            executeStatement(fullScript);
            //提交事务
            commitConnection();
        } catch (Exception e) {
            String message = "Error executing: " + script + ".  Cause: " + e;
            printlnError(message);
            throw new RuntimeSqlException(message, e);
        }
    }

    private void executeLineByLine(Reader reader) {
        StringBuilder command = new StringBuilder();
        try {
            BufferedReader lineReader = new BufferedReader(reader);
            String line;
            while ((line = lineReader.readLine()) != null) {
                //如果该 line 不满足执行条件，则继续放command
                command = handleLine(command, line);
            }
            //提交事务
            commitConnection();
            //如果 command 有数据，则抛出异常
            checkForMissingLineTerminator(command);
        } catch (Exception e) {
            String message = "Error executing: " + command + ".  Cause: " + e;
            printlnError(message);
            throw new RuntimeSqlException(message, e);
        }
    }

    public void closeConnection() {
        try {
            connection.close();
        } catch (Exception e) {
            // ignore
        }
    }

    private void setAutoCommit() {
        try {
            //如果指定的自动提交与数据库链接不同，则以指定为准
            if (autoCommit != connection.getAutoCommit()) {
                connection.setAutoCommit(autoCommit);
            }
        } catch (Throwable t) {
            throw new RuntimeSqlException("Could not set AutoCommit to " + autoCommit + ". Cause: " + t, t);
        }
    }

    private void commitConnection() {
        try {
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (Throwable t) {
            throw new RuntimeSqlException("Could not commit transaction. Cause: " + t, t);
        }
    }

    private void rollbackConnection() {
        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    private void checkForMissingLineTerminator(StringBuilder command) {
        if (command != null && command.toString().trim().length() > 0) {
            throw new RuntimeSqlException("Line missing end-of-line terminator (" + delimiter + ") => " + command);
        }
    }

    /**
     * 按行提交执行sql，逻辑：
     * 如果注释，忽略
     * 如果结尾是分号，则执行，并且清空 command 返回
     * 如果不满足执行条件，则comman.append(line)，分号command，执行满足执行条件的时候一并执行，比如建表语句
     *
     * @param command 所有的sql
     * @param line 一行数据
     * @return command
     * @throws SQLException
     * @throws UnsupportedEncodingException
     */
    private StringBuilder handleLine(StringBuilder command, String line) throws SQLException, UnsupportedEncodingException {
        String trimmedLine = line.trim();
        if (lineIsComment(trimmedLine)) {
            //处理注释
            println(trimmedLine);
        } else if (commandReadyToExecute(trimmedLine)) {
            //如果有分号，执行
            command.append(line, 0, line.lastIndexOf(delimiter));//去掉最后的分号
            command.append(LINE_SEPARATOR);
            println(command);
            executeStatement(command.toString());
            //执行之后，清空 command
            command.setLength(0);
        } else if (trimmedLine.length() > 0) {
            //没有分号，先加入，等后面的分号
            command.append(line);
            command.append(LINE_SEPARATOR);
        }
        return command;
    }

    /**
     * 该行是否为注释
     *
     * @param trimmedLine 内容
     * @return 该行是否为注释
     */
    private boolean lineIsComment(String trimmedLine) {
        return trimmedLine.startsWith("//") || trimmedLine.startsWith("--");
    }

    /**
     * 判断该行sql是否满足执行提交
     *
     * @param trimmedLine 该行sql
     * @return 判断该行sql是否满足执行提交
     */
    private boolean commandReadyToExecute(String trimmedLine) {
        // issue #561 remove anything after the delimiter
        return !fullLineDelimiter && trimmedLine.contains(delimiter) || fullLineDelimiter && trimmedLine.equals(delimiter);
    }

    /**
     * 执行sql脚本
     *
     * @param command sql命令
     * @throws SQLException 数据库异常
     */
    private void executeStatement(String command) throws SQLException {
        //就是用最简单的JDBC来执行
        boolean hasResults = false;
        Statement statement = connection.createStatement();
        statement.setEscapeProcessing(escapeProcessing);
        String sql = command;
        if (removeCRs) {
            sql = sql.replaceAll("\r\n", "\n");
        }
        if (stopOnError) {
            hasResults = statement.execute(sql);
        } else {
            try {
                hasResults = statement.execute(sql);
            } catch (SQLException e) {
                String message = "Error executing: " + command + ".  Cause: " + e;
                printlnError(message);
            }
        }
        printResults(statement, hasResults);
        try {
            statement.close();
        } catch (Exception e) {
            // Ignore to workaround a bug in some connection pools
        }
    }

    /**
     * 打印结果
     *
     * @param statement 脚本 statement
     * @param hasResults 是否有执行结果
     */
    private void printResults(Statement statement, boolean hasResults) {
        try {
            if (hasResults) {
                ResultSet rs = statement.getResultSet();
                if (rs != null) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();
                    for (int i = 0; i < cols; i++) {
                        String name = md.getColumnLabel(i + 1);
                        print(name + "\t");
                    }
                    println("");
                    while (rs.next()) {
                        for (int i = 0; i < cols; i++) {
                            String value = rs.getString(i + 1);
                            print(value + "\t");
                        }
                        println("");
                    }
                }
            }
        } catch (SQLException e) {
            printlnError("Error printing results: " + e.getMessage());
        }
    }

    private void print(Object o) {
        if (logWriter != null) {
            logWriter.print(o);
            logWriter.flush();
        }
    }

    private void println(Object o) {
        if (logWriter != null) {
            logWriter.println(o);
            logWriter.flush();
        }
    }

    private void printlnError(Object o) {
        if (errorLogWriter != null) {
            errorLogWriter.println(o);
            errorLogWriter.flush();
        }
    }

}
