/*
 *    Copyright 2009-2012 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.apache.ibatis.io;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>ResolverUtil is used to locate classes that are available in the/a class path and meet
 * arbitrary conditions. The two most common conditions are that a class implements/extends
 * another class, or that is it annotated with a specific annotation. However, through the use
 * of the {@link Test} class it is possible to search using arbitrary conditions.</p>
 * <p/>
 * <p>A ClassLoader is used to locate all locations (directories and jar files) in the class
 * path that contain classes within certain packages, and then to load those classes and
 * check them. By default the ClassLoader returned by
 * {@code Thread.currentThread().getContextClassLoader()} is used, but this can be overridden
 * by calling {@link #setClassLoader(ClassLoader)} prior to invoking any of the {@code find()}
 * methods.</p>
 * <p/>
 * <p>General searches are initiated by calling the
 * {@link #find(org.apache.ibatis.io.ResolverUtil.Test, String)} ()} method and supplying
 * a package name and a Test instance. This will cause the named package <b>and all sub-packages</b>
 * to be scanned for classes that meet the test. There are also utility methods for the common
 * use cases of scanning multiple packages for extensions of particular classes, or classes
 * annotated with a specific annotation.</p>
 * <p/>
 * <p>The standard usage pattern for the ResolverUtil class is as follows:</p>
 * <p/>
 * <pre>
 * ResolverUtil&lt;ActionBean&gt; resolver = new ResolverUtil&lt;ActionBean&gt;();
 * resolver.findImplementation(ActionBean.class, pkg1, pkg2);
 * resolver.find(new CustomTest(), pkg1);
 * resolver.find(new CustomTest(), pkg2);
 * Collection&lt;ActionBean&gt; beans = resolver.getClasses();
 * </pre>
 *
 * 找一个package下满足条件的所有类
 *
 * 扫描包用，能够获取到一个满足指定条件的类的 set 集合
 *
 * @author Tim Fennell
 */
public class ResolverUtil<T> {

    /*
     * An instance of Log to use for logging in this class.
     */
    private static final Log log = LogFactory.getLog(ResolverUtil.class);

    /**
     * The set of matches being accumulated.
     * 满足指定条件的 class 集合
     */
    private Set<Class<? extends T>> matches = new HashSet<Class<? extends T>>();

    /**
     * The ClassLoader to use when looking for classes. If null then the ClassLoader returned
     * by Thread.currentThread().getContextClassLoader() will be used.
     */
    private ClassLoader classloader;

    public static void main(String[] args) {
        String fullClassName = "com/janita/controller/TestController.class";
        System.out.println(fullClassName.substring(0, fullClassName.indexOf('.')).replace('/', '.'));
    }

    /**
     * Provides access to the classes discovered so far. If no calls have been made to
     * any of the {@code find()} methods, this set will be empty.
     *
     * @return the set of classes that have been discovered.
     */
    public Set<Class<? extends T>> getClasses() {
        return matches;
    }

    /**
     * Returns the classloader that will be used for scanning for classes. If no explicit
     * ClassLoader has been set by the calling, the context class loader will be used.
     *
     * @return the ClassLoader that will be used to scan for classes
     */
    public ClassLoader getClassLoader() {
        return classloader == null ? Thread.currentThread().getContextClassLoader() : classloader;
    }

    /**
     * Sets an explicit ClassLoader that should be used when scanning for classes. If none
     * is set then the context classloader will be used.
     *
     * @param classloader a ClassLoader to use when scanning for classes
     */
    public void setClassLoader(ClassLoader classloader) {
        this.classloader = classloader;
    }

    /**
     * Attempts to discover classes that are assignable to the type provided. In the case
     * that an interface is provided this method will collect implementations. In the case
     * of a non-interface class, subclasses will be collected.  Accumulated classes can be
     * accessed by calling {@link #getClasses()}.
     *
     * @param parent the class of interface to find subclasses or implementations of
     * @param packageNames one or more package names to scan (including subpackages) for classes
     */
    public ResolverUtil<T> findImplementations(Class<?> parent, String... packageNames) {
        if (packageNames == null) {
            return this;
        }

        Test test = new IsA(parent);
        for (String pkg : packageNames) {
            //把匹配到到数据就保存在该对象到一个属性，然后返回该对象
            find(test, pkg);
        }
        //然后返回该对象，调用者直接get即可
        return this;
    }

    /**
     * Attempts to discover classes that are annotated with the annotation. Accumulated
     * classes can be accessed by calling {@link #getClasses()}.
     *
     * @param annotation the annotation that should be present on matching classes
     * @param packageNames one or more package names to scan (including subpackages) for classes
     */
    public ResolverUtil<T> findAnnotated(Class<? extends Annotation> annotation, String... packageNames) {
        if (packageNames == null) {
            return this;
        }
        Test test = new AnnotatedWith(annotation);
        for (String pkg : packageNames) {
            find(test, pkg);
        }
        return this;
    }

    /**
     * 主要的方法，找一个package下满足条件的所有类,被TypeHanderRegistry,MapperRegistry,TypeAliasRegistry调用
     *
     * Scans for classes starting at the package provided and descending into subpackages.
     * Each class is offered up to the Test as it is discovered, and if the Test returns
     * true the class is retained.  Accumulated classes can be fetched by calling
     * {@link #getClasses()}.
     *
     * @param test an instance of {@link Test} that will be used to filter classes 过滤器
     * @param packageName the name of the package from which to start scanning for 包
     * classes, e.g. {@code net.sourceforge.stripes}
     */
    public ResolverUtil<T> find(Test test, String packageName) {
        //com.janita.controller -> com/janita/controller
        String path = getPackagePath(packageName);
        try {
            //通过VFS来深入jar包里面去找一个class
            VFS vfs = VFS.getInstance();
            //子文件/目录
            List<String> children = vfs.list(path);
            for (String child : children) {
                if (child.endsWith(".class")) {
                    addIfMatching(test, child);
                }
            }
        } catch (IOException ioe) {
            log.error("Could not read package: " + packageName, ioe);
        }

        return this;
    }

    /**
     * Converts a Java package name to a path that can be looked up with a call to
     * {@link ClassLoader#getResources(String)}.
     *
     * @param packageName The Java package name to convert to a path
     */
    protected String getPackagePath(String packageName) {
        return packageName == null ? null : packageName.replace('.', '/');
    }

    /**
     * Add the class designated by the fully qualified class name provided to the set of
     * resolved classes if and only if it is approved by the Test supplied.
     *
     * @param test the test used to determine if the class matches
     * @param fullClassName the fully qualified name of a class
     */
    @SuppressWarnings("unchecked")
    protected void addIfMatching(Test test, String fullClassName) {
        try {
            //com/janita/controller/TestController.class -> com.janita.controller.TestController
            String externalName = fullClassName.substring(0, fullClassName.indexOf('.')).replace('/', '.');
            ClassLoader loader = getClassLoader();
            log.debug("Checking to see if class " + externalName + " matches criteria [" + test + "]");
            //加载出来class
            Class<?> type = loader.loadClass(externalName);
            if (test.matches(type)) {
                //若该类匹配
                matches.add((Class<T>) type);
            }
        } catch (Throwable t) {
            log.warn("Could not examine class '" + fullClassName + "'" + " due to a " +
                    t.getClass().getName() + " with message: " + t.getMessage());
        }
    }

    /**
     * A simple interface that specifies how to test classes to determine if they
     * are to be included in the results produced by the ResolverUtil.
     */
    public static interface Test {

        /**
         * 如果传入的class的类型是 type，则返回true
         *
         * 如：
         * Test t = new Test(Object.class);
         * 则:t.match(Car.class) 为 true
         *
         * @param type 父类型
         * @return boolean
         */
        boolean matches(Class<?> type);
    }

    /**
     * A Test that checks to see if each class is assignable to the provided class. Note
     * that this test will match the parent type itself if it is presented for matching.
     */
    public static class IsA implements Test {

        /**
         * 如果传入的class的类型是 type，则返回true
         *
         * 如：
         * Test t = new Test(Object.class);
         * 则:t.match(Car.class) 为 true
         */
        private Class<?> superType;

        /**
         * Constructs an IsA test using the supplied Class as the parent class/interface.
         */
        public IsA(Class<?> parentType) {
            this.superType = parentType;
        }

        /**
         * Returns true if type is assignable to the parent type supplied in the constructor.
         */
        @Override
        public boolean matches(Class<?> type) {
            return type != null && superType.isAssignableFrom(type);
        }

        @Override
        public String toString() {
            return "is assignable to " + superType.getSimpleName();
        }
    }

    /**
     * A Test that checks to see if each class is annotated with a specific annotation. If it
     * is, then the test returns true, otherwise false.
     */
    public static class AnnotatedWith implements Test {

        private Class<? extends Annotation> annotation;

        /**
         * Constructs an AnnotatedWith test for the specified annotation type.
         */
        public AnnotatedWith(Class<? extends Annotation> annotation) {
            this.annotation = annotation;
        }

        /**
         * Returns true if the type is annotated with the class provided to the constructor.
         */
        @Override
        public boolean matches(Class<?> type) {
            return type != null && type.isAnnotationPresent(annotation);
        }

        @Override
        public String toString() {
            return "annotated with @" + annotation.getSimpleName();
        }
    }
}