package org.apache.ibatis.mapping;

import lombok.Getter;
import org.apache.ibatis.transaction.TransactionFactory;

import javax.sql.DataSource;

/**
 * @author Clinton Begin
 */

/**
 * 环境
 * 决定加载哪种环境(开发环境/生产环境)
 *
 * 不同环境均有：
 * 1.不同的数据库
 * 2.不同的事务
 * 3.不同的数据源
 */
public final class Environment {

    //环境id
    @Getter
    private final String id;

    //事务工厂
    @Getter
    private final TransactionFactory transactionFactory;

    //数据源
    @Getter
    private final DataSource dataSource;

    public Environment(String id, TransactionFactory transactionFactory, DataSource dataSource) {
        if (id == null) {
            throw new IllegalArgumentException("Parameter 'id' must not be null");
        }
        if (transactionFactory == null) {
            throw new IllegalArgumentException("Parameter 'transactionFactory' must not be null");
        }
        this.id = id;
        if (dataSource == null) {
            throw new IllegalArgumentException("Parameter 'dataSource' must not be null");
        }
        this.transactionFactory = transactionFactory;
        this.dataSource = dataSource;
    }

    /**
     * 建造模式 用法应该是new Environment.Builder(id).transactionFactory(xx).dataSource(xx).build();
     */
    public static class Builder {

        private String id;

        private TransactionFactory transactionFactory;

        private DataSource dataSource;

        public Builder(String id) {
            this.id = id;
        }

        public Builder transactionFactory(TransactionFactory transactionFactory) {
            this.transactionFactory = transactionFactory;
            return this;
        }

        public Builder dataSource(DataSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public String id() {
            return this.id;
        }

        public Environment build() {
            return new Environment(this.id, this.transactionFactory, this.dataSource);
        }
    }
}
