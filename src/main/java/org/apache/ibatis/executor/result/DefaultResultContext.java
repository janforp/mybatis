package org.apache.ibatis.executor.result;

import lombok.Getter;
import org.apache.ibatis.session.ResultContext;

/**
 * @author Clinton Begin
 */

/**
 * 默认结果上下文
 * 查询的结果都会先放到该对象
 */
public class DefaultResultContext implements ResultContext {

    @Getter
    private Object resultObject;

    @Getter
    private int resultCount;

    @Getter
    private boolean stopped;

    public DefaultResultContext() {
        resultObject = null;
        resultCount = 0;
        stopped = false;
    }

    /**
     * 应该是每次调用nextResultObject这个方法，这样内部count就加1
     *
     * @param resultObject 结果
     */
    public void nextResultObject(Object resultObject) {
        //每接收一条记录，就把结果记录的计数 +1
        resultCount++;
        this.resultObject = resultObject;
    }

    @Override
    public void stop() {
        this.stopped = true;
    }
}
