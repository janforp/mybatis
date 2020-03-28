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

    @SuppressWarnings("unchecked")
    public DefaultMapResultHandler(String mapKey, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory) {
        this.objectFactory = objectFactory;
        this.objectWrapperFactory = objectWrapperFactory;
        this.mappedResults = objectFactory.create(Map.class);
        this.mapKey = mapKey;
    }

    @Override
    public void handleResult(ResultContext context) {
        // TODO is that assignment always true?
        //得到一条记录
        //这边黄色警告没法去掉了？因为返回Object型
        final V value = (V) context.getResultObject();
        //MetaObject.forObject,包装一下记录
        //MetaObject是用反射来包装各种类型
        final MetaObject mo = MetaObject.forObject(value, objectFactory, objectWrapperFactory);
        // TODO is that assignment always true?
        final K key = (K) mo.getValue(mapKey);
        mappedResults.put(key, value);
        //这个类主要目的是把得到的List转为Map
    }

    public Map<K, V> getMappedResults() {
        return mappedResults;
    }
}
