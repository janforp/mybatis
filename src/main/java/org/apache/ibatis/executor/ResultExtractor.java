package org.apache.ibatis.executor;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Array;
import java.util.List;

/**
 * @author Andrew Gustafson
 */

/**
 * 结果抽取器
 */
public class ResultExtractor {

    private final Configuration configuration;

    private final ObjectFactory objectFactory;

    public ResultExtractor(Configuration configuration, ObjectFactory objectFactory) {
        this.configuration = configuration;
        this.objectFactory = objectFactory;
    }

    /**
     * 从list中抽取对象
     *
     * @param list 对象集合
     * @param targetType 目标类型
     * @return 从list中抽取对象
     */
    public Object extractObjectFromList(List<Object> list, Class<?> targetType) {
        Object value = null;
        if (targetType != null && targetType.isAssignableFrom(list.getClass())) {
            //1.如果targetType是list，直接返回list
            value = list;
        } else if (targetType != null && objectFactory.isCollection(targetType)) {
            //2.如果targetType是Collection，返回包装好的list
            //初始化对象
            value = objectFactory.create(targetType);
            MetaObject metaObject = configuration.newMetaObject(value);
            //把list全部放进 value
            metaObject.addAll(list);
        } else if (targetType != null && targetType.isArray()) {
            //3.如果targetType是数组，则数组转list
            Class<?> arrayComponentType = targetType.getComponentType();
            Object array = Array.newInstance(arrayComponentType, list.size());
            if (arrayComponentType.isPrimitive()) {
                //基础类型的数组
                for (int i = 0; i < list.size(); i++) {
                    Array.set(array, i, list.get(i));
                }
                value = array;
            } else {
                value = list.toArray((Object[]) array);
            }
        } else {
            //4.最后返回list的第0个元素
            if (list != null && list.size() > 1) {
                throw new ExecutorException("Statement returned more than one row, where no more than one was expected.");
            } else if (list != null && list.size() == 1) {
                value = list.get(0);
            }
        }
        return value;
    }
}
