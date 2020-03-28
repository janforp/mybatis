package org.apache.ibatis.binding;

import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 映射器代理，代理模式
 *
 * 在每个映射器代理中都存在以上三个参数，也就是说我们一旦我们使用过某个操作，
 * 那么这个操作过程中产生的代理实例将会一直存在，且具体操作方法会保存在这个代理实例的方法缓存中备用
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
public class MapperProxy<T> implements InvocationHandler, Serializable {

    private static final long serialVersionUID = -6424540398559729838L;

    private final SqlSession sqlSession;

    /**
     * mapper/DAO接口类型
     */
    private final Class<T> mapperInterface;

    private final Map<Method, MapperMethod> methodCache;

    public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
        this.sqlSession = sqlSession;
        this.mapperInterface = mapperInterface;
        this.methodCache = methodCache;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //代理以后，所有Mapper的方法调用时，都会调用这个invoke方法
        //并不是任何一个方法都需要执行调用代理对象进行执行，如果这个方法是Object中通用的方法（toString、hashCode等）无需执行

        //该方法的申明类
        Class<?> declaringClass = method.getDeclaringClass();
        //如果该方法是 Object 里面的方法，则直执行即可
        if (Object.class.equals(declaringClass)) {
            try {
                return method.invoke(this, args);
            } catch (Throwable t) {
                throw ExceptionUtil.unwrapThrowable(t);
            }
        }
        //这里优化了，去缓存中找MapperMethod
        final MapperMethod mapperMethod = cachedMapperMethod(method);
        //执行
        return mapperMethod.execute(sqlSession, args);
    }

    /**
     * 去缓存中找MapperMethod
     *
     * @param method DAO 中的方法
     * @return 代理方法
     */
    private MapperMethod cachedMapperMethod(Method method) {
        MapperMethod mapperMethod = methodCache.get(method);
        if (mapperMethod == null) {
            //找不到才去new
            mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
            //缓存起来，避免每次都 new
            methodCache.put(method, mapperMethod);
        }
        return mapperMethod;
    }
}
