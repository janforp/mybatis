package org.apache.ibatis.reflection.property;

import org.apache.ibatis.reflection.ReflectionException;

import java.util.Locale;

/**
 * 属性命名器
 *
 * @author Clinton Begin
 */
public final class PropertyNamer {

    private PropertyNamer() {
        // Prevent Instantiation of Static Class
    }

    //方法转为属性
    public static String methodToProperty(String methodName) {
        //去掉get|set|is
        if (methodName.startsWith("is")) {
            methodName = methodName.substring(2);
        } else if (methodName.startsWith("get") || methodName.startsWith("set")) {
            methodName = methodName.substring(3);
        } else {
            throw new ReflectionException("Error parsing property methodName '" + methodName + "'.  Didn't start with 'is', 'get' or 'set'.");
        }

        //如果只有1个字母-->转为小写
        //如果大于1个字母，第二个字母非大写-->转为小写
        //String uRL -->String getuRL() {
        if (methodName.length() == 1 || (methodName.length() > 1 && !Character.isUpperCase(methodName.charAt(1)))) {
            methodName = methodName.substring(0, 1).toLowerCase(Locale.ENGLISH) + methodName.substring(1);
        }
        return methodName;
    }

    //是否是属性
    public static boolean isProperty(String name) {
        //必须以get|set|is开头
        return name.startsWith("get") || name.startsWith("set") || name.startsWith("is");
    }

    //是否是getter
    public static boolean isGetter(String name) {
        return name.startsWith("get") || name.startsWith("is");
    }

    //是否是setter
    public static boolean isSetter(String name) {
        return name.startsWith("set");
    }
}
