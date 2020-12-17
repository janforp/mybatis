package org.apache.ibatis.scripting.xmltags;

import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态上下文：主要用于记录解析动态SQL语句之后产生的SQL语句片段，可以认为它是一个用于记录动态SQL语句解析结果的容器
 * 组合模式：
 * 抽象构件为 SqlNode 接口，源码如下
 * public interface SqlNode {
 * boolean apply(DynamicContext context);
 * }
 * 复制代码apply 是 SQLNode 接口中定义的唯一方法，该方法会根据用户传入的实参，参数解析该SQLNode所记录的动态SQL节点，
 * 并调用 DynamicContext.appendSql() 方法将解析后的SQL片段追加到 DynamicContext.sqlBuilder 中保存，
 * 当SQL节点下所有的 SqlNode 完成解析后，我们就可以从 DynamicContext 中获取一条动态生产的、完整的SQL语句
 *
 * @author Clinton Begin
 */
public class DynamicContext {

    public static final String PARAMETER_OBJECT_KEY = "_parameter";

    public static final String DATABASE_ID_KEY = "_databaseId";

    static {
        //TODO OgnlRuntime
        //定义属性->getter方法映射，ContextMap到ContextAccessor的映射，注册到ognl运行时
        //参考 http://commons.apache.org/proper/commons-ognl/developer-guide.html

        OgnlRuntime.setPropertyAccessor(ContextMap.class, new ContextAccessor());

        //将传入的参数对象统一封装为ContextMap对象（继承了HashMap对象），
        //然后Ognl运行时环境在动态计算sql语句时，
        //会按照ContextAccessor中描述的Map接口的方式来访问和读取ContextMap对象，获取计算过程中需要的参数。
        //ContextMap对象内部可能封装了一个普通的POJO对象，也可以是直接传递的Map对象，
        // 当然从外部是看不出来的，因为都是使用Map的接口来读取数据。
    }

    /**
     * 2个键值对
     * key _parameter对应入参数 parameterObject
     * key _databaseId 则对应 configuration.getDatabaseId()
     *
     * 动态sql中的表达式就是使用这个
     */
    private final ContextMap bindings;

    /**
     * 当执行一个方法的时候，就会根据用户传入的参数来计算动态标签，然后对所有的sql进行拼接，这个对象就是拼接的结果
     */
    private final StringBuilder sqlBuilder = new StringBuilder();

    private int uniqueNumber = 0;

    /**
     * //在DynamicContext的构造函数中，根据传入的参数对象是否为Map类型，有两个不同构造ContextMap的方式。
     * //而ContextMap作为一个继承了HashMap的对象，作用就是用于统一参数的访问方式：用Map接口方法来访问数据。
     * //具体来说，当传入的参数对象不是Map类型时，Mybatis会将传入的POJO对象用MetaObject对象来封装，
     * //当动态计算sql过程需要获取数据时，用Map接口的get方法包装 MetaObject对象的取值过程。
     *
     * @param configuration 配置
     * @param parameterObject 入参数
     */
    public DynamicContext(Configuration configuration, Object parameterObject) {
        //绝大多数调用的地方parameterObject为null
        boolean paramIsNotMap = !(parameterObject instanceof Map);

        if (parameterObject != null && paramIsNotMap) {
            //如果是map型  ??  这句是 如果不是map型
            MetaObject metaObject = configuration.newMetaObject(parameterObject);
            bindings = new ContextMap(metaObject);
        } else {
            bindings = new ContextMap(null);
        }
        //上面只是根据传入的测试初始化 bindings 一个 ContextMap：key:_parameter
        bindings.put(PARAMETER_OBJECT_KEY, parameterObject);

        String databaseId = configuration.getDatabaseId();
        bindings.put(DATABASE_ID_KEY, databaseId);
    }

    public Map<String, Object> getBindings() {
        return bindings;
    }

    public void bind(String name, Object value) {
        bindings.put(name, value);
    }

    public void appendSql(String sql) {
        sqlBuilder.append(sql);//SELECT id, firstName, lastName FROM person WHERE id = #{id}
        sqlBuilder.append(" ");
    }

    public String getSql() {
        return sqlBuilder.toString().trim();
    }

    public int getUniqueNumber() {
        return uniqueNumber++;
    }

    //上下文map，静态内部类
    static class ContextMap extends HashMap<String, Object> {

        private static final long serialVersionUID = 2977601501966151582L;

        private MetaObject parameterMetaObject;

        public ContextMap(MetaObject parameterMetaObject) {
            this.parameterMetaObject = parameterMetaObject;
        }

        @Override
        public Object get(Object key) {
            String strKey = (String) key;
            //先去map里找
            if (super.containsKey(strKey)) {
                return super.get(strKey);
            }

            //如果没找到，再用ognl表达式去取值
            //如person[0].birthDate.year
            if (parameterMetaObject != null) {
                // issue #61 do not modify the context when reading
                return parameterMetaObject.getValue(strKey);
            }
            return null;
        }
    }

    //上下文访问器，静态内部类,实现OGNL的PropertyAccessor
    static class ContextAccessor implements PropertyAccessor {

        @Override
        public Object getProperty(Map context, Object target, Object name)
                throws OgnlException {
            Map map = (Map) target;

            Object result = map.get(name);
            if (result != null) {
                return result;
            }

            Object parameterObject = map.get(PARAMETER_OBJECT_KEY);
            if (parameterObject instanceof Map) {
                return ((Map) parameterObject).get(name);
            }

            return null;
        }

        @Override
        public void setProperty(Map context, Object target, Object name, Object value)
                throws OgnlException {
            Map<Object, Object> map = (Map<Object, Object>) target;
            map.put(name, value);
        }

        @Override
        public String getSourceAccessor(OgnlContext arg0, Object arg1, Object arg2) {
            return null;
        }

        @Override
        public String getSourceSetter(OgnlContext arg0, Object arg1, Object arg2) {
            return null;
        }
    }
}
