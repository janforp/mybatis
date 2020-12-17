package org.apache.ibatis.executor.keygen;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;

import java.sql.Statement;

/**
 * 键值生成器：典型的命令模式设计
 *
 * @author Clinton Begin
 */
public interface KeyGenerator {

    //定了2个回调方法，processBefore,processAfter
    void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter);

    void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter);
}
