package org.apache.ibatis.reflection.property;

import java.util.Iterator;

/**
 * 属性分解为标记，迭代模式
 * 如person[0].birthdate.year，将依次取得person[0], birthdate, year
 *
 * @author Clinton Begin
 */
public class PropertyTokenizer implements Iterable<PropertyTokenizer>, Iterator<PropertyTokenizer> {

    //例子： person[0].birthdate.year
    private String name; //person

    private String indexedName; //person[0]

    private String index; //0

    private String children; //birthdate.year

    public PropertyTokenizer(String fullName) {
        //person[0].birthdate.year
        //找.
        int delim = fullName.indexOf('.');
        if (delim > -1) {
            name = fullName.substring(0, delim);
            children = fullName.substring(delim + 1);
        } else {
            //找不到.的话，取全部部分
            name = fullName;
            children = null;
        }
        indexedName = name;
        //把中括号里的数字给解析出来
        delim = name.indexOf('[');
        if (delim > -1) {
            index = name.substring(delim + 1, name.length() - 1);
            name = name.substring(0, delim);
        }
    }

    public String getName() {
        return name;
    }

    public String getIndex() {
        return index;
    }

    public String getIndexedName() {
        return indexedName;
    }

    public String getChildren() {
        return children;
    }

    @Override
    public boolean hasNext() {
        return children != null;
    }

    //取得下一个,非常简单，直接再通过儿子来new另外一个实例
    @Override
    public PropertyTokenizer next() {
        return new PropertyTokenizer(children);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
    }

    @Override
    public Iterator<PropertyTokenizer> iterator() {
        return this;
    }
}
