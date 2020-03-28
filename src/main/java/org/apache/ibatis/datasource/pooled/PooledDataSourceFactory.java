package org.apache.ibatis.datasource.pooled;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;

/**
 * @author Clinton Begin
 */

/**
 * 有连接池的数据源工厂
 * 继承了UnpooledDataSourceFactory
 */
public class PooledDataSourceFactory extends UnpooledDataSourceFactory {

    //数据源换成了PooledDataSource
    public PooledDataSourceFactory() {
        this.dataSource = new PooledDataSource();
    }
}
