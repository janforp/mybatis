/*
 *    Copyright 2009-2012 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.apache.ibatis.session;

import java.sql.Connection;

/**
 * Creates an {@link SqlSesion} out of a connection or a DataSource
 *
 * @author Clinton Begin
 */

/**
 * SqlSessionFactory 有六个方法创建 SqlSession 实例。通常来说，当你选择其中一个方法时，你需要考虑以下几点：
 *
 * 事务处理：你希望在 session 作用域中使用事务作用域，还是使用自动提交（auto-commit）？（对很多数据库和/或 JDBC 驱动来说，等同于关闭事务支持）
 * 数据库连接：你希望 MyBatis 帮你从已配置的数据源获取连接，还是使用自己提供的连接？
 * 语句执行：你希望 MyBatis 复用 PreparedStatement 和/或批量更新语句（包括插入语句和删除语句）吗？
 *
 * 构建SqlSession的工厂.工厂模式
 */
public interface SqlSessionFactory {

    /**
     * 默认的 openSession() 方法没有参数，它会创建具备如下特性的 SqlSession：
     *
     * 事务作用域将会开启（也就是不自动提交）。
     * 将由当前环境配置的 DataSource 实例中获取 Connection 对象。
     * 事务隔离级别将会使用驱动或数据源的默认设置。
     * 预处理语句不会被复用，也不会批量处理更新。
     *
     * @return
     */
    SqlSession openSession();

    //自动提交
    SqlSession openSession(boolean autoCommit);

    //连接
    SqlSession openSession(Connection connection);

    //事务隔离级别
    SqlSession openSession(TransactionIsolationLevel level);

    //执行器的类型

    /**
     * 你可能对 ExecutorType 参数感到陌生。这个枚举类型定义了三个值:
     *
     * ExecutorType.SIMPLE：该类型的执行器没有特别的行为。它为每个语句的执行创建一个新的预处理语句。
     * ExecutorType.REUSE：该类型的执行器会复用预处理语句。
     * ExecutorType.BATCH：该类型的执行器会批量执行所有更新语句，如果 SELECT 在多个更新中间执行，将在必要时将多条更新语句分隔开来，以方便理解。
     *
     * @param execType
     * @return
     */
    SqlSession openSession(ExecutorType execType);

    SqlSession openSession(ExecutorType execType, boolean autoCommit);

    SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level);

    SqlSession openSession(ExecutorType execType, Connection connection);

    Configuration getConfiguration();
}
