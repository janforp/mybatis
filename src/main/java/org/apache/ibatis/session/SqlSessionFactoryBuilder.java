package org.apache.ibatis.session;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

/**
 * 该类负责创建 SqlSessionFactory， 当工厂出来之后最好是释放该类
 * Builds {@link SqlSession} instances.
 * 构建SqlSessionFactory的工厂.工厂模式
 *
 * 其实就是通过配置文件初始化工厂
 */
public class SqlSessionFactoryBuilder {

    /**
     * 所有的构造器最好都是通过该构造器是实例化工厂
     *
     * @param config 配置
     * @return SqlSessionFactory
     */
    public SqlSessionFactory build(Configuration config) {
        return new DefaultSqlSessionFactory(config);
    }

    /**
     * 通过一些梦如何的操作，把配置文件解析到对象 Configuration ，然后用第一个构造器实例化工厂
     *
     * 最常用的，它使用了一个参照了XML文档或更特定的SqlMapConfig.xml文件的Reader实例。
     * 可选的参数是environment和properties。Environment决定加载哪种环境(开发环境/生产环境)，包括数据源和事务管理器。
     * 如果使用properties，那么就会加载那些properties（属性配置文件），那些属性可以用${propName}语法形式多次用在配置文件中。和Spring很像，一个思想？
     *
     * @param reader 以输入流的形式的配置文件
     * @param environment 环境
     * @param properties 其他的配置参数
     * @return 工厂
     */
    public SqlSessionFactory build(Reader reader, String environment, Properties properties) {
        try {
            //委托XMLConfigBuilder来解析xml文件，并构建
            XMLConfigBuilder parser = new XMLConfigBuilder(reader, environment, properties);
            //解析配置文件
            Configuration configuration = parser.parse();
            //通过 Configuration 构建 SqlSessionFactory
            return build(configuration);
        } catch (Exception e) {
            //这里是捕获异常，包装成自己的异常并抛出的idiom？，最后还要reset ErrorContext
            throw ExceptionFactory.wrapException("Error building SqlSession.", e);
        } finally {
            ErrorContext.instance().reset();
            try {
                reader.close();
            } catch (IOException e) {
                // Intentionally ignore. Prefer previous error.
            }
        }
    }

    /**
     * 通过一些梦如何的操作，把配置文件解析到对象 Configuration ，然后用第一个构造器实例化工厂
     *
     * @param inputStream 以输入流的形式的配置文件
     * @param environment 环境
     * @param properties 其他的配置参数
     * @return 工厂
     */
    public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
        try {
            XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
            Configuration configuration = parser.parse();
            return build(configuration);
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error building SqlSession.", e);
        } finally {
            ErrorContext.instance().reset();
            try {
                inputStream.close();
            } catch (IOException e) {
                // Intentionally ignore. Prefer previous error.
            }
        }
    }

    public SqlSessionFactory build(Reader reader) {
        return build(reader, null, null);
    }

    public SqlSessionFactory build(Reader reader, String environment) {
        return build(reader, environment, null);
    }

    public SqlSessionFactory build(Reader reader, Properties properties) {
        return build(reader, null, properties);
    }

    public SqlSessionFactory build(InputStream inputStream) {
        return build(inputStream, null, null);
    }

    public SqlSessionFactory build(InputStream inputStream, String environment) {
        return build(inputStream, environment, null);
    }

    public SqlSessionFactory build(InputStream inputStream, Properties properties) {
        return build(inputStream, null, properties);
    }
}
