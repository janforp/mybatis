package org.apache.ibatis.executor.keygen;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;

import java.sql.Statement;

/**
 * 不用键值生成器
 * MappedStatement有一个keyGenerator属性，默认的就用NoKeyGenerator
 *
 * @author Clinton Begin
 */
public class NoKeyGenerator implements KeyGenerator {

    //都是空方法
    @Override
    public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        // Do Nothing
    }

    @Override
    public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        // Do Nothing
    }

}
