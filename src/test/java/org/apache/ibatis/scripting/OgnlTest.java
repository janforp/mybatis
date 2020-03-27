package org.apache.ibatis.scripting;

import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;
import org.junit.Assert;
import org.junit.Test;

/**
 * 类说明：
 *
 * @author zhucj
 * @since 20200326
 */
public class OgnlTest {

    //非根元素
    @Test
    public void testOgnl1() throws OgnlException {
        //创建一个Ognl上下文对象
        OgnlContext context = new OgnlContext();

        /**
         * 1.OgnlContext放入基本变量数据
         */
        //放入数据
        context.put("cn", "China");
        //获取数据（map）
        String value = (String) context.get("cn");
        Assert.assertEquals("China", value);
        /**
         * 2.OgnlContext放入对象数据
         */
        //创建对象，设置对象属性
        User user = new User();
        user.setId(100);
        user.setName("Jack");
        //【往非根元素放入数据，取值的时候表达式要用“#”】
        context.put("user", user);
        //获取对象属性
        //使用这种方式也可以获取
        Object s = context.get("user");
        Assert.assertTrue(s instanceof User);
        Assert.assertSame(s.getClass(), User.class);

        //使用Ognl表达式来获取
        //举例：例如标签<s:a value="#user.id">取值，实际上就是运行了下面的代码获取的
        //先构建一个Ognl表达式，再解析表达式
        Object ognl = Ognl.parseExpression("#user.id");//构建Ognl表达式
        Object contextRoot = context.getRoot();
        Object value1 = Ognl.getValue(ognl, context, contextRoot);//解析表达式
        Assert.assertEquals(100, (int) user.getId());
        User user1 = new User();
        user1.setId(200);
        user1.setName("Jack");
        context.setRoot(user1);
        Object ognl1 = Ognl.parseExpression("id");//构建Ognl表达式
        contextRoot = context.getRoot();
        Object value2 = Ognl.getValue(ognl1, context, contextRoot);//解析表达式
        Assert.assertEquals(200, value2);
    }

    //根元素，
    @Test
    public void testOgnl2() throws OgnlException {
        OgnlContext context = new OgnlContext();
        User user1 = new User();
        user1.setId(100);
        user1.setName("Jack");
        context.setRoot(user1);
        //根元素直接使用id,不需要加#号
        Object ognl1 = Ognl.parseExpression("id");//构建Ognl表达式
        Object value2 = Ognl.getValue(ognl1, context, context.getRoot());//解析表达式
        Assert.assertEquals(100, value2);
    }

    //ognl对静态方法调用的支持
    @Test
    public void testOgnl3() throws Exception {
        //创建一个Ognl上下文对象
        OgnlContext context = new OgnlContext();
        //Ognl表达式语言，调用类的静态方法
        //        Object ognl = Ognl.parseExpression("@Math@floor(10.9)");
        //由于Math类在开发中比较常用，所有也可以这样写
        Object ognl = Ognl.parseExpression("@@floor(10.9)");
        Object value = Ognl.getValue(ognl, context, context.getRoot());
        Assert.assertEquals(10.0, value);
    }
}
