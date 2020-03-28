package org.apache.ibatis.reflection.wrapper;

import org.apache.ibatis.reflection.MetaObject;

/**
 * 对象包装器工厂
 *
 * @author Clinton Begin
 */
public interface ObjectWrapperFactory {

    //有没有包装器
    boolean hasWrapperFor(Object object);

    //得到包装器
    ObjectWrapper getWrapperFor(MetaObject metaObject, Object object);
}
