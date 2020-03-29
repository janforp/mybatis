package org.apache.ibatis.executor.result;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Clinton Begin
 */

/**
 * 默认结果处理器
 */
public class DefaultResultHandler implements ResultHandler {

    /**
     * 内部实现是存了一个List,用来存处理好（handleResult处理）的结果
     */
    private final List<Object> list;

    public DefaultResultHandler() {
        list = new ArrayList<Object>();
    }

    /**
     * 但不一定是ArrayList,也可以通过ObjectFactory来产生特定的List
     *
     * @param objectFactory 对象工厂
     */
    @SuppressWarnings("unchecked")
    public DefaultResultHandler(ObjectFactory objectFactory) {
        list = objectFactory.create(List.class);
    }

    @Override
    public void handleResult(ResultContext resultContext) {
        //处理很简单，就是把记录加入List
        list.add(resultContext.getResultObject());
    }

    public List<Object> getResultList() {
        return list;
    }
}
