package org.apache.ibatis.scripting.xmltags;

import ognl.ClassResolver;
import org.apache.ibatis.io.Resources;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom ognl {@code ClassResolver} which behaves same like ognl's
 * {@code DefaultClassResolver}. But uses the {@code Resources}
 * utility class to find the target class instead of {@code Class#forName(String)}.
 *
 * @author Daniel Guggi
 * @see https://github.com/mybatis/mybatis-3/issues/161
 */
public class OgnlClassResolver implements ClassResolver {

    private Map<String, Class<?>> classes = new HashMap<String, Class<?>>(101);

    @Override
    public Class classForName(String className, Map context) throws ClassNotFoundException {
        Class<?> result = classes.get(className);
        if (result == null) {
            try {
                result = Resources.classForName(className);
            } catch (ClassNotFoundException e1) {
                //如果类名称没有 . 则试图在 java.lang包下加载该类
                if (className.indexOf('.') == -1) {
                    result = Resources.classForName("java.lang." + className);
                    classes.put("java.lang." + className, result);
                }
            }
            classes.put(className, result);
        }
        return result;
    }
}
