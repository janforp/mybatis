package org.apache.ibatis.reflection;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.invoker.SetFieldInvoker;
import org.apache.ibatis.reflection.property.PropertyNamer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ReflectPermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a cached set of class definition information that
 * allows for easy mapping between property names and getter/setter methods.
 *
 * 反射器, 属性->getter/setter的映射器，而且加了缓存
 * 可参考ReflectorTest来理解这个类的用处
 * 其实就是一个缓存，一个类型有一个 Reflector 缓存实例
 *
 * @author Clinton Begin
 */
public class Reflector {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    //这里用ConcurrentHashMap，多线程支持，作为一个缓存
    private static final Map<Class<?>, Reflector> REFLECTOR_MAP = new ConcurrentHashMap<Class<?>, Reflector>();

    /**
     * 是否开启 class 缓存
     */
    private static boolean classCacheEnabled = true;

    /**
     * 每一个class都有一个Reflector
     */
    private Class<?> type;

    //getter的属性列表
    private String[] readablePropertyNames = EMPTY_STRING_ARRAY;

    //setter的属性列表
    private String[] writeablePropertyNames = EMPTY_STRING_ARRAY;

    //setter的方法列表
    private Map<String, Invoker> setMethods = new HashMap<String, Invoker>();

    //getter的方法列表
    private Map<String, Invoker> getMethods = new HashMap<String, Invoker>();

    //setter的类型列表
    private Map<String, Class<?>> setTypes = new HashMap<String, Class<?>>();

    //get函数的返回类型
    private Map<String, Class<?>> getTypes = new HashMap<String, Class<?>>();

    //构造函数
    private Constructor<?> defaultConstructor;

    private Map<String, String> caseInsensitivePropertyMap = new HashMap<String, String>();

    private Reflector(Class<?> clazz) {
        type = clazz;
        //解析获取构造函数，并且赋值给 defaultConstructor 属性
        addDefaultConstructor(clazz);
        //加入getter
        addGetMethods(clazz);
        //加入setter
        addSetMethods(clazz);
        //加入字段
        addFields(clazz);
        readablePropertyNames = getMethods.keySet().toArray(new String[0]);
        writeablePropertyNames = setMethods.keySet().toArray(new String[0]);
        for (String propName : readablePropertyNames) {
            //这里为了能找到某一个属性，就把他变成大写作为map的key。。。
            caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
        }
        for (String propName : writeablePropertyNames) {
            caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
        }
    }

    private static boolean canAccessPrivateMethods() {
        try {
            SecurityManager securityManager = System.getSecurityManager();
            if (null != securityManager) {
                securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
            }
        } catch (SecurityException e) {
            return false;
        }
        return true;
    }

    /*
     * Gets an instance of ClassInfo for the specified class.
     * 得到某个类的反射器，是静态方法，而且要缓存，又要多线程，所以REFLECTOR_MAP是一个ConcurrentHashMap
     *
     * @param clazz The class for which to lookup the method cache.
     * @return The method cache for the class
     */
    public static Reflector forClass(Class<?> clazz) {
        //如果开启来缓存，则先走缓存逻辑
        if (classCacheEnabled) {
            // synchronized (clazz) removed see issue #461
            //对于每个类来说，我们假设它是不会变的，这样可以考虑将这个类的信息(构造函数，getter,setter,字段)加入缓存，以提高速度
            Reflector cached = REFLECTOR_MAP.get(clazz);
            if (cached == null) {
                cached = new Reflector(clazz);
                REFLECTOR_MAP.put(clazz, cached);
            }
            return cached;
        } else {
            //如果关闭来缓存，则每次都创建一个新实例
            return new Reflector(clazz);
        }
    }

    public static boolean isClassCacheEnabled() {
        return classCacheEnabled;
    }

    public static void setClassCacheEnabled(boolean classCacheEnabled) {
        Reflector.classCacheEnabled = classCacheEnabled;
    }

    private void addDefaultConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterTypes().length == 0) {
                if (canAccessPrivateMethods()) {
                    try {
                        constructor.setAccessible(true);
                    } catch (Exception e) {
                        // Ignored. This is only a final precaution, nothing we can do.
                    }
                }
                if (constructor.isAccessible()) {
                    this.defaultConstructor = constructor;
                }
            }
        }
    }

    private void addGetMethods(Class<?> cls) {
        //有冲突的，因为子类跟父类的关系
        Map<String, List<Method>> conflictingGetters = new HashMap<String, List<Method>>();
        //这里getter和setter都调用了getClassMethods，有点浪费效率了。不妨把addGetMethods,addSetMethods合并成一个方法叫addMethods
        Method[] methods = getClassMethods(cls);
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith("get") && name.length() > 3) {//从一个方法的名称判断是否为get方法的基本条件：以get开头，并且get后还有其他字符
                if (method.getParameterTypes().length == 0) {//从一个方法的名称判断是否为get方法的必要条件：get方法是无参数的
                    name = PropertyNamer.methodToProperty(name);//方法转属性，如 getId -> id
                    addMethodConflict(conflictingGetters, name, method);//把该 name method 对放进 Map<String, List<Method>> conflictingGetters
                }
            } else if (name.startsWith("is") && name.length() > 2) {
                //如果为 boolean 类型的属性
                if (method.getParameterTypes().length == 0) {
                    name = PropertyNamer.methodToProperty(name);
                    addMethodConflict(conflictingGetters, name, method);
                }
            }
        }
        //删除冲突的方法
        resolveGetterConflicts(conflictingGetters);
    }

    /**
     * 解决冲突
     *
     * @param conflictingGetters key:属性名称,value:该属性对应的所有get方法集合
     */
    private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
        Set<String> propertyNameSet = conflictingGetters.keySet();
        for (String propName : propertyNameSet) {
            List<Method> getters = conflictingGetters.get(propName);
            Iterator<Method> iterator = getters.iterator();
            Method firstMethod = iterator.next();
            if (getters.size() == 1) {
                //如果只有一个，则直接添加
                addGetMethod(propName, firstMethod);
            } else {
                //如果一个属性有多个 get 函数，则：
                Method getter = firstMethod;
                Class<?> getterType = firstMethod.getReturnType();
                while (iterator.hasNext()) {
                    Method method = iterator.next();
                    Class<?> methodType = method.getReturnType();
                    if (methodType.equals(getterType)) {
                        throw new ReflectionException("Illegal overloaded getter method with ambiguous type for property "
                                + propName + " in class " + firstMethod.getDeclaringClass()
                                + ".  This breaks the JavaBeans " + "specification and can cause unpredicatble results.");
                    } else if (methodType.isAssignableFrom(getterType)) {
                        // OK getter type is descendant
                    } else if (getterType.isAssignableFrom(methodType)) {
                        getter = method;
                        getterType = methodType;
                    } else {
                        throw new ReflectionException("Illegal overloaded getter method with ambiguous type for property "
                                + propName + " in class " + firstMethod.getDeclaringClass()
                                + ".  This breaks the JavaBeans " + "specification and can cause unpredicatble results.");
                    }
                }
                addGetMethod(propName, getter);
            }
        }
    }

    private void addGetMethod(String name, Method method) {
        if (isValidPropertyName(name)) {
            getMethods.put(name, new MethodInvoker(method));
            getTypes.put(name, method.getReturnType());
        }
    }

    private void addSetMethods(Class<?> cls) {
        Map<String, List<Method>> conflictingSetters = new HashMap<String, List<Method>>();
        Method[] methods = getClassMethods(cls);
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith("set") && name.length() > 3) {
                if (method.getParameterTypes().length == 1) {
                    name = PropertyNamer.methodToProperty(name);
                    addMethodConflict(conflictingSetters, name, method);
                }
            }
        }
        resolveSetterConflicts(conflictingSetters);
    }

    /**
     * 把name - method 对塞入 conflictingMethods
     *
     * @param conflictingMethods 最终需要的
     * @param name 属性名称
     * @param method 属性对应的方法
     */
    private void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
        List<Method> list = conflictingMethods.get(name);
        if (list == null) {
            list = new ArrayList<Method>();
            conflictingMethods.put(name, list);
        }
        list.add(method);
    }

    private void resolveSetterConflicts(Map<String, List<Method>> conflictingSetters) {
        for (String propName : conflictingSetters.keySet()) {
            List<Method> setters = conflictingSetters.get(propName);
            Method firstMethod = setters.get(0);
            if (setters.size() == 1) {
                addSetMethod(propName, firstMethod);
            } else {
                Class<?> expectedType = getTypes.get(propName);
                if (expectedType == null) {
                    throw new ReflectionException("Illegal overloaded setter method with ambiguous type for property "
                            + propName + " in class " + firstMethod.getDeclaringClass() + ".  This breaks the JavaBeans " +
                            "specification and can cause unpredicatble results.");
                } else {
                    Iterator<Method> methods = setters.iterator();
                    Method setter = null;
                    while (methods.hasNext()) {
                        Method method = methods.next();
                        if (method.getParameterTypes().length == 1
                                && expectedType.equals(method.getParameterTypes()[0])) {
                            setter = method;
                            break;
                        }
                    }
                    if (setter == null) {
                        throw new ReflectionException("Illegal overloaded setter method with ambiguous type for property "
                                + propName + " in class " + firstMethod.getDeclaringClass() + ".  This breaks the JavaBeans " +
                                "specification and can cause unpredicatble results.");
                    }
                    addSetMethod(propName, setter);
                }
            }
        }
    }

    private void addSetMethod(String name, Method method) {
        if (isValidPropertyName(name)) {
            setMethods.put(name, new MethodInvoker(method));
            setTypes.put(name, method.getParameterTypes()[0]);
        }
    }

    private void addFields(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (canAccessPrivateMethods()) {
                try {
                    field.setAccessible(true);
                } catch (Exception e) {
                    // Ignored. This is only a final precaution, nothing we can do.
                }
            }
            if (field.isAccessible()) {
                if (!setMethods.containsKey(field.getName())) {
                    // issue #379 - removed the check for final because JDK 1.5 allows
                    // modification of final fields through reflection (JSR-133). (JGB)
                    // pr #16 - final static can only be set by the classloader
                    int modifiers = field.getModifiers();
                    if (!(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))) {
                        //final/static的方法不行
                        addSetField(field);
                    }
                }
                if (!getMethods.containsKey(field.getName())) {
                    addGetField(field);
                }
            }
        }
        if (clazz.getSuperclass() != null) {
            addFields(clazz.getSuperclass());
        }
    }

    private void addSetField(Field field) {
        if (isValidPropertyName(field.getName())) {
            setMethods.put(field.getName(), new SetFieldInvoker(field));
            setTypes.put(field.getName(), field.getType());
        }
    }

    private void addGetField(Field field) {
        if (isValidPropertyName(field.getName())) {
            getMethods.put(field.getName(), new GetFieldInvoker(field));
            getTypes.put(field.getName(), field.getType());
        }
    }

    private boolean isValidPropertyName(String name) {
        return !(name.startsWith("$") || "serialVersionUID".equals(name) || "class".equals(name));
    }

    /*
     * This method returns an array containing all methods
     * declared in this class and any superclass.
     * We use this method, instead of the simpler Class.getMethods(),
     * because we want to look for private methods as well.
     * 得到所有方法，包括private方法，包括父类方法.包括接口方法
     *
     * @param cls The class
     * @return An array containing all methods in this class
     */
    private Method[] getClassMethods(Class<?> cls) {
        //解决冲突之后，一个属性就只有一个方法
        Map<String, Method> uniqueMethods = new HashMap<String, Method>();
        Class<?> currentClass = cls;
        while (currentClass != null) {
            //当前class中申明的方法
            Method[] declaredMethods = currentClass.getDeclaredMethods();
            addUniqueMethods(uniqueMethods, declaredMethods);
            // we also need to look for interface methods -
            // because the class may be abstract
            Class<?>[] interfaces = currentClass.getInterfaces();
            for (Class<?> anInterface : interfaces) {
                //往 uniqueMethods 中添加方法
                addUniqueMethods(uniqueMethods, anInterface.getMethods());
            }
            //遍历他的父类型
            currentClass = currentClass.getSuperclass();
        }
        Collection<Method> methods = uniqueMethods.values();
        return methods.toArray(new Method[0]);
    }

    private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
        for (Method currentMethod : methods) {
            //是否是范型桥接函数/方法
            boolean bridge = currentMethod.isBridge();
            if (!bridge) {
                //获取方法签名，包括返回，方法名称，参数名称组成的字符串
                String signature = getSignature(currentMethod);
                // check to see if the method is already known
                // if it is known, then an extended class must have
                // overridden a method
                if (!uniqueMethods.containsKey(signature)) {
                    if (canAccessPrivateMethods()) {
                        try {
                            currentMethod.setAccessible(true);
                        } catch (Exception e) {
                            // Ignored. This is only a final precaution, nothing we can do.
                        }
                    }
                    uniqueMethods.put(signature, currentMethod);
                }
            }
        }
    }

    /**
     * 获取方法签名，包括返回，方法名称，参数名称
     *
     * @param method 方法
     * @return 方法签名
     */
    private String getSignature(Method method) {
        StringBuilder stringBuilder = new StringBuilder();
        Class<?> returnType = method.getReturnType();
        if (returnType != null) {
            stringBuilder.append(returnType.getName()).append('#');
        }
        stringBuilder.append(method.getName());
        //方法参数类型
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i == 0) {
                stringBuilder.append(':');
            } else {
                stringBuilder.append(',');
            }
            //当前参数类型名称
            String name = parameters[i].getName();
            stringBuilder.append(name);
        }
        return stringBuilder.toString();
    }

    /*
     * Gets the name of the class the instance provides information for
     *
     * @return The class name
     */
    public Class<?> getType() {
        return type;
    }

    public Constructor<?> getDefaultConstructor() {
        if (defaultConstructor != null) {
            return defaultConstructor;
        } else {
            throw new ReflectionException("There is no default constructor for " + type);
        }
    }

    public boolean hasDefaultConstructor() {
        return defaultConstructor != null;
    }

    public Invoker getSetInvoker(String propertyName) {
        Invoker method = setMethods.get(propertyName);
        if (method == null) {
            throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
        }
        return method;
    }

    public Invoker getGetInvoker(String propertyName) {
        Invoker method = getMethods.get(propertyName);
        if (method == null) {
            throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
        }
        return method;
    }

    /*
     * Gets the type for a property setter
     *
     * @param propertyName - the name of the property
     * @return The Class of the propery setter
     */
    public Class<?> getSetterType(String propertyName) {
        Class<?> clazz = setTypes.get(propertyName);
        if (clazz == null) {
            throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
        }
        return clazz;
    }

    /*
     * Gets the type for a property getter
     *
     * @param propertyName - the name of the property
     * @return The Class of the propery getter
     */
    public Class<?> getGetterType(String propertyName) {
        Class<?> clazz = getTypes.get(propertyName);
        if (clazz == null) {
            throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
        }
        return clazz;
    }

    /*
     * Gets an array of the readable properties for an object
     *
     * @return The array
     */
    public String[] getGetablePropertyNames() {
        return readablePropertyNames;
    }

    /*
     * Gets an array of the writeable properties for an object
     *
     * @return The array
     */
    public String[] getSetablePropertyNames() {
        return writeablePropertyNames;
    }

    /*
     * Check to see if a class has a writeable property by name
     *
     * @param propertyName - the name of the property to check
     * @return True if the object has a writeable property by the name
     */
    public boolean hasSetter(String propertyName) {
        return setMethods.keySet().contains(propertyName);
    }

    /*
     * Check to see if a class has a readable property by name
     *
     * @param propertyName - the name of the property to check
     * @return True if the object has a readable property by the name
     */
    public boolean hasGetter(String propertyName) {
        return getMethods.keySet().contains(propertyName);
    }

    public String findPropertyName(String name) {
        return caseInsensitivePropertyMap.get(name.toUpperCase(Locale.ENGLISH));
    }
}
