package org.apache.ibatis.binding;

import lombok.Getter;
import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 映射器代理工厂，通过这个工厂类可以获取到对应的映射器代理类MapperProxy，
 * 这里只需要保存一个映射器的代理工厂，根据工厂就可以获取到对应的映射器。
 *
 * @author Lasse Voss
 */
public class MapperProxyFactory<T> {

    /**
     * DAO.class
     * mapper.xml 文件的 namespace 对应的 接口类型
     * 构造器中初始化
     */
    @Getter
    private final Class<T> mapperInterfaceClass;

    /**
     * key:dao里面的方法，value:封装了该方法的对象
     * 这个缓存集合用于保存当前映射器中的映射方法的。
     *
     * 映射方法单独定义，是因为这里并不存在一个真正的类和方法供调用，只是通过反射和代理的原理来实现的假的调用，
     * 映射方法是调用的最小单位（独立个体），将映射方法定义之后，它就成为一个实实在在的存在，
     * 我们可以将调用过的方法保存到对应的映射器的缓存中，以供下次调用，
     * 避免每次调用相同的方法的时候都需要重新进行方法的生成。很明显，方法的生成比较复杂，会消耗一定的时间，
     * 将其保存在缓存集合中备用，可以极大的解决这种时耗问题。
     */
    @Getter
    private Map<Method, MapperMethod> methodCache = new ConcurrentHashMap<Method, MapperMethod>();

    /**
     * 一般通过该方法获取代理的mapper对象
     *
     * @param sqlSession 对话
     * @return mapper代理实例对象
     */
    public T newInstance(SqlSession sqlSession) {
        final MapperProxy<T> mapperProxy = new MapperProxy<T>(sqlSession, mapperInterfaceClass, methodCache);
        return newInstance(mapperProxy);
    }

    @SuppressWarnings("unchecked")
    protected T newInstance(MapperProxy<T> mapperProxy) {
        //用JDK自带的动态代理生成映射器
        ClassLoader classLoader = mapperInterfaceClass.getClassLoader();
        Class[] classes = { mapperInterfaceClass };
        return (T) Proxy.newProxyInstance(classLoader, classes, mapperProxy);
    }

    public MapperProxyFactory(Class<T> mapperInterface) {
        this.mapperInterfaceClass = mapperInterface;
    }
}
