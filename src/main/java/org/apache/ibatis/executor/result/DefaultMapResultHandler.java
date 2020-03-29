package org.apache.ibatis.executor.result;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

import java.util.Map;

/**
 * @author Clinton Begin
 */

/**
 * 默认Map结果处理器
 */
public class DefaultMapResultHandler<K, V> implements ResultHandler {

    //内部实现是存了一个Map
    private final Map<K, V> mappedResults;

    private final String mapKey;

    private final ObjectFactory objectFactory;

    private final ObjectWrapperFactory objectWrapperFactory;

    /**
     * 构造默认的 map 结果处理器
     *
     * @param mapKey The property to use as key for each value in the list.
     * @param objectFactory configuration.getObjectFactory(), configuration.getObjectWrapperFactory()
     * @param objectWrapperFactory configuration.getObjectFactory(), configuration.getObjectWrapperFactory()
     */
    @SuppressWarnings("unchecked")
    public DefaultMapResultHandler(String mapKey, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory) {
        this.objectFactory = objectFactory;
        this.objectWrapperFactory = objectWrapperFactory;
        this.mappedResults = objectFactory.create(Map.class);
        this.mapKey = mapKey;
    }

    @Override
    public void handleResult(ResultContext resultContext) {
        // TODO is that assignment always true?
        //得到一条记录
        //这边黄色警告没法去掉了？因为返回Object型
        final V value = (V) resultContext.getResultObject();
        //MetaObject.forObject,包装一下记录
        //MetaObject是用反射来包装各种类型
        final MetaObject metaObject = MetaObject.forObject(value, objectFactory, objectWrapperFactory);
        // TODO is that assignment always true?
        //从对象 value 中获取属性 mapKey 的值
        final K key = (K) metaObject.getValue(mapKey);
        mappedResults.put(key, value);
        //这个类主要目的是把得到的List转为Map
    }

    /**
     * 获取处理好的结果
     *
     * @return 处理好的结果
     */
    public Map<K, V> getMappedResults() {
        return mappedResults;
    }
}
