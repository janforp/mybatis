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

    /**
     * 构造器初始化
     */
    private final SqlSession sqlSession;

    /**
     * mapper/DAO接口类型
     * 构造器初始化
     */
    private final Class<T> mapperInterface;

    /**
     * 构造器初始化
     */
    private final Map<Method, MapperMethod> methodCache;

    /**
     * java动态代理，如果sqlSession.getMapper(Mapper.class)获取到的映射接口去操作的话，就会通过该动态代理生成mapper接口的实例
     *
     * @param proxy MapperProxy 实例
     * @param method mapper接口的某一个方法对象
     * @param args mapper接口的某一个方法的入参数
     * @return 函数执行结果
     * @throws Throwable 异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //代理以后，所有Mapper的方法调用时，都会调用这个invoke方法
        //并不是任何一个方法都需要执行调用代理对象进行执行，如果这个方法是Object中通用的方法（toString、hashCode等）无需执行

        //该方法的申明类
        Class<?> declaringClass = method.getDeclaringClass();
        //如果该方法是 Object 里面的方法，则直执行即可
        if (Object.class.equals(declaringClass)) {
            try {
                //进入此分支的是：toString,equal等方法
                return method.invoke(this, args);
            } catch (Throwable t) {
                throw ExceptionUtil.unwrapThrowable(t);
            }
        }
        //这里优化了，去缓存中找MapperMethod
        final MapperMethod mapperMethod = cachedMapperMethod(method);
        //通过 org.apache.ibatis.binding.MapperMethod.execute 执行数据库操作，最终还是通过 sqlSession 的
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

    public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
        this.sqlSession = sqlSession;
        this.mapperInterface = mapperInterface;
        this.methodCache = methodCache;
    }
}
