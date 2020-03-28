package org.apache.ibatis.reflection.wrapper;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectionException;

/**
 * @author Clinton Begin
 */
/**
 * 默认对象包装器工厂
 * 
 */
public class DefaultObjectWrapperFactory implements ObjectWrapperFactory {

  @Override
  //默认没有包装器
  public boolean hasWrapperFor(Object object) {
    return false;
  }

  //默认抛异常
  @Override
  public ObjectWrapper getWrapperFor(MetaObject metaObject, Object object) {
    throw new ReflectionException("The DefaultObjectWrapperFactory should never be called to provide an ObjectWrapper.");
  }

}
