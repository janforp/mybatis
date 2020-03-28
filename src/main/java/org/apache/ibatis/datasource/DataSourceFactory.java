package org.apache.ibatis.datasource;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 数据源工厂
 * 有三种内建的数据源类型 UNPOOLED POOLED JNDI
 *
 * @author Clinton Begin
 */
public interface DataSourceFactory {

    //设置属性,被XMLConfigBuilder所调用
    void setProperties(Properties props);

    //生产数据源,直接得到javax.sql.DataSource
    DataSource getDataSource();
}
