package org.apache.ibatis.binding;

import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 映射器注册机
 * 这个注册器显而易见会保存在Configuration实例中备用
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 */
public class MapperRegistry {

    /**
     * 将已经添加的映射都放入HashMap
     * 注册的 mapperClass
     * value:通过这个工厂类可以获取到对应的映射器代理类MapperProxy，这里只需要保存一个映射器的代理工厂，根据工厂就可以获取到对应的映射器。
     *
     * 即使是在一般的项目中也会存在很多的映射器，这些映射器都要注册到注册器中，
     * 注册器集合中的每个映射器中都保存着一个独有的映射器代理工厂实例，
     * 而不是映射器实例，映射器实例只在需要的时候使用代理工厂进行创建
     * ，所以我们可以这么来看，MapperProxyFactory会存在多个实例，针对每个映射器有一个实例，这个实例就作为值保存在注册器中
     */
    private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<Class<?>, MapperProxyFactory<?>>();

    private Configuration config;

    public MapperRegistry(Configuration config) {
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    //返回代理类
    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
        final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
        if (mapperProxyFactory == null) {
            throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
        }
        try {
            return mapperProxyFactory.newInstance(sqlSession);
        } catch (Exception e) {
            throw new BindingException("Error getting mapper instance. Cause: " + e, e);
        }
    }

    public <T> boolean hasMapper(Class<T> type) {
        return knownMappers.containsKey(type);
    }

    /**
     * 将指定的类型的映射器添加到注册器中
     *
     * @param mapperClass 映射类
     * @param <T> 任何类型
     */
    public <T> void addMapper(Class<T> mapperClass) {
        //mapper必须是接口！才会添加
        if (mapperClass.isInterface()) {
            //knownMappers.containsKey(type) 该 class 已经注册到映射器中了，不能重复
            if (hasMapper(mapperClass)) {
                //如果重复添加了，报错
                throw new BindingException("Type " + mapperClass + " is already known to the MapperRegistry.");
            }
            boolean loadCompleted = false;
            try {
                //先把 mapper/dao 封装到 MapperProxyFactory 中
                MapperProxyFactory<T> mapperProxyFactory = new MapperProxyFactory<T>(mapperClass);
                //注册
                knownMappers.put(mapperClass, mapperProxyFactory);
                // It's important that the mapperClass is added before the parser is run
                // otherwise the binding may automatically be attempted by the
                // mapper parser. If the mapperClass is already known, it won't try.
                //对使用注解方式实现的映射器进行注册
                MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, mapperClass);
                parser.parse();
                loadCompleted = true;
            } finally {
                //如果加载过程中出现异常需要再将这个mapper从mybatis中删除,这种方式比较丑陋吧，难道是不得已而为之？
                if (!loadCompleted) {
                    knownMappers.remove(mapperClass);
                }
            }
        }
    }

    /**
     * Collections.unmodifiableCollection这个可以得到一个集合的镜像，它的返回结果不可直接被改变，否则会报错
     *
     * @since 3.2.2
     */
    public Collection<Class<?>> getMappers() {
        return Collections.unmodifiableCollection(knownMappers.keySet());
    }

    /**
     * 将包下满足以superType为超类的映射器注册到注册器中
     *
     * @param packageName 包名称
     * @param superType 超类
     * @since 3.2.2
     */
    public void addMappers(String packageName, Class<?> superType) {
        //查找包下所有是superType的类
        ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
        //包下面的类的匹配器，该匹配器专门去匹配传入的class是不是superType类型
        ResolverUtil.IsA test = new ResolverUtil.IsA(superType);
        //在包packageName中找出 superType 的所有子类，并且放到resolverUtil的 matches 集合中
        resolverUtil.find(test, packageName);
        //从包中找出的class集合
        Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
        for (Class<?> mapperClass : mapperSet) {
            addMapper(mapperClass);
        }
    }

    /**
     * 查找包下所有类,扫描包下的每个接口注册进去
     *
     * @since 3.2.2
     */
    public void addMappers(String packageName) {
        //表示所有的java类都可以被注册
        addMappers(packageName, Object.class);
    }
}
