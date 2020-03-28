package org.apache.ibatis.executor.result;

import org.apache.ibatis.session.ResultContext;

/**
 * @author Clinton Begin
 */

/**
 * 默认结果上下文
 */
public class DefaultResultContext implements ResultContext {

    private Object resultObject;

    private int resultCount;

    private boolean stopped;

    public DefaultResultContext() {
        resultObject = null;
        resultCount = 0;
        stopped = false;
    }

    @Override
    public Object getResultObject() {
        return resultObject;
    }

    @Override
    public int getResultCount() {
        return resultCount;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    //应该是每次调用nextResultObject这个方法，这样内部count就加1
    public void nextResultObject(Object resultObject) {
        resultCount++;
        this.resultObject = resultObject;
    }

    @Override
    public void stop() {
        this.stopped = true;
    }

}
