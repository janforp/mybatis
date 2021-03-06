package org.apache.ibatis.builder.xml;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * XML配置构建器，建造者模式,继承BaseBuilder
 * 解析配置文件
 *
 * @author Clinton Begin
 */
public class XMLConfigBuilder extends BaseBuilder {

    //是否已解析，XPath解析器,环境
    private boolean parsed;

    private XPathParser parser;

    /**
     * 当前默认的环境名称
     */
    private String environment;

    //以下3个一组
    public XMLConfigBuilder(Reader reader) {
        this(reader, null, null);
    }

    public XMLConfigBuilder(Reader reader, String environment) {
        this(reader, environment, null);
    }

    /**
     * 构造函数，转换成XPathParser再去调用构造函数
     *
     * @param reader 配置文件
     * @param environment 环境，可以空
     * @param props 属性，可以空
     */
    public XMLConfigBuilder(Reader reader, String environment, Properties props) {
        //构造一个需要验证，XMLMapperEntityResolver的XPathParser。
        this(new XPathParser(reader, true, props, new XMLMapperEntityResolver()),
                environment,
                props);
    }

    //以下3个一组
    public XMLConfigBuilder(InputStream inputStream) {
        this(inputStream, null, null);
    }

    public XMLConfigBuilder(InputStream inputStream, String environment) {
        this(inputStream, environment, null);
    }

    public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
        this(new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
    }

    //上面6个构造函数最后都合流到这个函数，传入XPathParser
    private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
        //首先调用父类初始化Configuration
        super(new Configuration());
        //错误上下文设置成SQL Mapper Configuration(XML文件配置),以便后面出错了报错用吧
        ErrorContext.instance().resource("SQL Mapper Configuration");
        //将Properties全部设置到Configuration里面去
        this.configuration.setVariables(props);
        this.parsed = false;
        this.environment = environment;
        this.parser = parser;
    }

    //解析配置
    public Configuration parse() {
        //如果已经解析过了，报错
        if (parsed) {
            throw new BuilderException("Each XMLConfigBuilder can only be used once.");
        }
        parsed = true;
        //  <?xml version="1.0" encoding="UTF-8" ?>
        //  <!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        //  "http://mybatis.org/dtd/mybatis-3-config.dtd">
        //  <configuration>
        //  <environments default="development">
        //  <environment id="development">
        //  <transactionManager type="JDBC"/>
        //  <dataSource type="POOLED">
        //  <property name="driver" value="${driver}"/>
        //  <property name="url" value="${url}"/>
        //  <property name="username" value="${username}"/>
        //  <property name="password" value="${password}"/>
        //  </dataSource>
        //  </environment>
        //  </environments>
        //  <mappers>
        //  <mapper resource="org/mybatis/example/BlogMapper.xml"/>
        //  </mappers>
        //  </configuration>

        //根节点是configuration
        XNode rootNode = parser.evalNode("/configuration");
        parseConfiguration(rootNode);
        return configuration;
    }

    /**
     * 解析配置
     *
     * @param configurationRootNode configuration 根下面的所有配置
     */
    private void parseConfiguration(XNode configurationRootNode) {
        try {
            //分步骤解析
            //issue #117 read properties first
            //1.properties
            XNode properties = configurationRootNode.evalNode("properties");
            propertiesElement(properties);

            //2.类型别名，并且注册到别名注册器
            XNode typeAliases = configurationRootNode.evalNode("typeAliases");
            typeAliasesElement(typeAliases);

            //3.插件
            XNode plugins = configurationRootNode.evalNode("plugins");
            pluginElement(plugins);

            //4.对象工厂
            XNode objectFactory = configurationRootNode.evalNode("objectFactory");
            objectFactoryElement(objectFactory);

            //5.对象包装工厂
            XNode objectWrapperFactory = configurationRootNode.evalNode("objectWrapperFactory");
            objectWrapperFactoryElement(objectWrapperFactory);

            //6.设置,类似一些开关，配置
            XNode settings = configurationRootNode.evalNode("settings");
            settingsElement(settings);

            // read it after objectFactory and objectWrapperFactory issue #631
            //7.环境
            XNode environments = configurationRootNode.evalNode("environments");
            environmentsElement(environments);

            //8.databaseIdProvider
            XNode databaseIdProvider = configurationRootNode.evalNode("databaseIdProvider");
            databaseIdProviderElement(databaseIdProvider);

            //9.类型处理器
            XNode typeHandlers = configurationRootNode.evalNode("typeHandlers");
            typeHandlerElement(typeHandlers);

            //10.映射器
            XNode mappers = configurationRootNode.evalNode("mappers");
            mapperElement(mappers);
        } catch (Exception e) {
            throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
        }
    }

    //2.类型别名
    //<typeAliases>
    //  <typeAlias alias="Author" type="domain.blog.Author"/>
    //  <typeAlias alias="Blog" type="domain.blog.Blog"/>
    //  <typeAlias alias="Comment" type="domain.blog.Comment"/>
    //  <typeAlias alias="Post" type="domain.blog.Post"/>
    //  <typeAlias alias="Section" type="domain.blog.Section"/>
    //  <typeAlias alias="Tag" type="domain.blog.Tag"/>
    //</typeAliases>
    //or
    //<typeAliases>
    //  <package name="domain.blog"/>
    //</typeAliases>
    private void typeAliasesElement(XNode aliasNode) {
        if (aliasNode != null) {
            List<XNode> typeAliasNodeList = aliasNode.getChildren();
            for (XNode child : typeAliasNodeList) {
                if ("package".equals(child.getName())) {
                    //如果是package
                    String typeAliasPackage = child.getStringAttribute("name");
                    //（一）调用TypeAliasRegistry.registerAliases，去包下找所有类,然后注册别名(有@Alias注解则用，没有则取类的simpleName)
                    configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
                } else {
                    //如果是typeAlias <typeAlias alias="Person" type="org.apache.ibatis.submitted.force_flush_on_select.Person"/>
                    String alias = child.getStringAttribute("alias");
                    //类名称
                    String typeClassName = child.getStringAttribute("type");
                    try {
                        Class<?> clazz = Resources.classForName(typeClassName);
                        //根据Class名字来注册类型别名
                        //（二）调用TypeAliasRegistry.registerAlias
                        if (alias == null) {
                            //alias可以省略，使用小写的类名称
                            typeAliasRegistry.registerAlias(clazz);
                        } else {
                            typeAliasRegistry.registerAlias(alias, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
                    }
                }
            }
        }
    }

    //3.插件
    //MyBatis 允许你在某一点拦截已映射语句执行的调用。默认情况下,MyBatis 允许使用插件来拦截方法调用
    //<plugins>
    //  <plugin interceptor="org.mybatis.example.ExamplePlugin">
    //    <property name="someProperty" value="100"/>
    //  </plugin>
    //</plugins>
    private void pluginElement(XNode pluginsNode) throws Exception {
        if (pluginsNode != null) {
            List<XNode> children = pluginsNode.getChildren();
            for (XNode pluginNode : children) {
                //拦截器类全名称
                String interceptorFullClassName = pluginNode.getStringAttribute("interceptor");
                //拿到 name - value
                Properties properties = pluginNode.getChildrenAsProperties();
                //拦截器对象
                Interceptor interceptorInstance = (Interceptor) resolveClass(interceptorFullClassName).newInstance();
                interceptorInstance.setProperties(properties);
                //调用InterceptorChain.addInterceptor
                configuration.addInterceptor(interceptorInstance);
            }
        }
    }

    //4.对象工厂,可以自定义对象创建的方式,比如用对象池？
    //<objectFactory type="org.mybatis.example.ExampleObjectFactory">
    //  <property name="someProperty" value="100"/>
    //</objectFactory>
    private void objectFactoryElement(XNode objectFactoryNode) throws Exception {
        if (objectFactoryNode != null) {
            //<objectFactory type="org.mybatis.example.ExampleObjectFactory">
            String fullClassName = objectFactoryNode.getStringAttribute("type");
            //<property name="someProperty" value="100"/>
            Properties properties = objectFactoryNode.getChildrenAsProperties();
            ObjectFactory factory = (ObjectFactory) resolveClass(fullClassName).newInstance();
            factory.setProperties(properties);
            configuration.setObjectFactory(factory);
        }
    }

    //5.对象包装工厂
    private void objectWrapperFactoryElement(XNode objectWrapperFactoryNode) throws Exception {
        if (objectWrapperFactoryNode != null) {
            String fullClassName = objectWrapperFactoryNode.getStringAttribute("type");
            ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(fullClassName).newInstance();
            configuration.setObjectWrapperFactory(factory);
        }
    }

    //1.properties
    //<properties resource="org/mybatis/example/config.properties">
    //    <property name="username" value="dev_user"/>
    //    <property name="password" value="F2Fa3!33TYyg"/>
    //</properties>
    private void propertiesElement(XNode propertiesElementNode) throws Exception {
        if (propertiesElementNode != null) {
            //如果在这些地方,属性多于一个的话,MyBatis 按照如下的顺序加载它们:

            //1.在 properties 元素体内指定的属性首先被读取。
            //2.从类路径下资源或 properties 元素的 url 属性中加载的属性第二被读取,它会覆盖已经存在的完全一样的属性。
            //3.作为方法参数传递的属性最后被读取, 它也会覆盖任一已经存在的完全一样的属性,这些属性可能是从 properties 元素体内和资源/url 属性中加载的。
            //传入方式是调用构造函数时传入，public XMLConfigBuilder(Reader reader, String environment, Properties props)

            //1.XNode.getChildrenAsProperties函数方便得到孩子所有Properties
            //把name-value解析到 Properties
            Properties defaults = propertiesElementNode.getChildrenAsProperties();
            //2.然后查找resource或者url,加入前面的Properties
            //<properties resourceUrl="org/mybatis/example/config.properties">
            //  <property name="username" value="dev_user"/>
            //  <property name="password" value="F2Fa3!33TYyg"/>
            //</properties>
            String resourceUrl = propertiesElementNode.getStringAttribute("resource");
            String url = propertiesElementNode.getStringAttribute("url");
            if (resourceUrl != null && url != null) {
                //url,resources二选一
                throw new BuilderException("The properties element cannot specify both a URL and a resourceUrl based property file reference.  Please specify one or the other.");
            }
            if (resourceUrl != null) {
                Properties resourceAsProperties = Resources.getResourceAsProperties(resourceUrl);
                defaults.putAll(resourceAsProperties);
            } else if (url != null) {
                defaults.putAll(Resources.getUrlAsProperties(url));
            }
            //3.Variables也全部加入Properties
            Properties vars = configuration.getVariables();
            if (vars != null) {
                defaults.putAll(vars);
            }
            parser.setVariables(defaults);
            //所有的配置都丢进 configuration
            configuration.setVariables(defaults);
        }
    }

    //6.设置
    //这些是极其重要的调整, 它们会修改 MyBatis 在运行时的行为方式
    //<settings>
    //  <setting name="cacheEnabled" value="true"/>
    //  <setting name="lazyLoadingEnabled" value="true"/>
    //  <setting name="multipleResultSetsEnabled" value="true"/>
    //  <setting name="useColumnLabel" value="true"/>
    //  <setting name="useGeneratedKeys" value="false"/>
    //  <setting name="enhancementEnabled" value="false"/>
    //  <setting name="defaultExecutorType" value="SIMPLE"/>
    //  <setting name="defaultStatementTimeout" value="25000"/>
    //  <setting name="safeRowBoundsEnabled" value="false"/>
    //  <setting name="mapUnderscoreToCamelCase" value="false"/>
    //  <setting name="localCacheScope" value="SESSION"/>
    //  <setting name="jdbcTypeForNull" value="OTHER"/>
    //  <setting name="lazyLoadTriggerMethods" value="equals,clone,hashCode,toString"/>
    //</settings>
    private void settingsElement(XNode settingsNode) throws Exception {
        if (settingsNode != null) {
            //<setting name="useColumnLabel" value="true"/> name - value
            Properties settingsNameToValueMap = settingsNode.getChildrenAsProperties();
            // Check that all settings are known to the configuration class
            //检查下是否在Configuration类里都有相应的setter方法（没有拼写错误）
            MetaClass metaConfig = MetaClass.forClass(Configuration.class);
            // name
            Set<Object> settingNameSet = settingsNameToValueMap.keySet();
            for (Object name : settingNameSet) {
                //保证配置没有拼写错误
                if (!metaConfig.hasSetter(String.valueOf(name))) {
                    //配置文件中拼写错误
                    throw new BuilderException("The setting " + name + " is not known.  Make sure you spelled it correctly (case sensitive).");
                }
            }

            //下面非常简单，一个个设置属性
            //如何自动映射列到字段/ 属性
            configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(settingsNameToValueMap.getProperty("autoMappingBehavior", "PARTIAL")));
            //缓存
            configuration.setCacheEnabled(booleanValueOf(settingsNameToValueMap.getProperty("cacheEnabled"), true));
            //proxyFactory (CGLIB | JAVASSIST)
            //延迟加载的核心技术就是用代理模式，CGLIB/JAVASSIST两者选一
            configuration.setProxyFactory((ProxyFactory) createInstance(settingsNameToValueMap.getProperty("proxyFactory")));
            //延迟加载
            configuration.setLazyLoadingEnabled(booleanValueOf(settingsNameToValueMap.getProperty("lazyLoadingEnabled"), false));
            //延迟加载时，每种属性是否还要按需加载
            configuration.setAggressiveLazyLoading(booleanValueOf(settingsNameToValueMap.getProperty("aggressiveLazyLoading"), true));
            //允不允许多种结果集从一个单独 的语句中返回
            configuration.setMultipleResultSetsEnabled(booleanValueOf(settingsNameToValueMap.getProperty("multipleResultSetsEnabled"), true));
            //使用列标签代替列名
            configuration.setUseColumnLabel(booleanValueOf(settingsNameToValueMap.getProperty("useColumnLabel"), true));
            //允许 JDBC 支持生成的键
            configuration.setUseGeneratedKeys(booleanValueOf(settingsNameToValueMap.getProperty("useGeneratedKeys"), false));
            //配置默认的执行器
            configuration.setDefaultExecutorType(ExecutorType.valueOf(settingsNameToValueMap.getProperty("defaultExecutorType", "SIMPLE")));
            //超时时间
            configuration.setDefaultStatementTimeout(integerValueOf(settingsNameToValueMap.getProperty("defaultStatementTimeout"), null));
            //是否将DB字段自动映射到驼峰式Java属性（A_COLUMN-->aColumn）
            configuration.setMapUnderscoreToCamelCase(booleanValueOf(settingsNameToValueMap.getProperty("mapUnderscoreToCamelCase"), false));
            //嵌套语句上使用RowBounds
            configuration.setSafeRowBoundsEnabled(booleanValueOf(settingsNameToValueMap.getProperty("safeRowBoundsEnabled"), false));
            //默认用session级别的缓存
            configuration.setLocalCacheScope(LocalCacheScope.valueOf(settingsNameToValueMap.getProperty("localCacheScope", "SESSION")));
            //为null值设置jdbctype
            configuration.setJdbcTypeForNull(JdbcType.valueOf(settingsNameToValueMap.getProperty("jdbcTypeForNull", "OTHER")));
            //Object的哪些方法将触发延迟加载
            configuration.setLazyLoadTriggerMethods(stringSetValueOf(settingsNameToValueMap.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
            //使用安全的ResultHandler
            configuration.setSafeResultHandlerEnabled(booleanValueOf(settingsNameToValueMap.getProperty("safeResultHandlerEnabled"), true));
            //动态SQL生成语言所使用的脚本语言
            configuration.setDefaultScriptingLanguage(resolveClass(settingsNameToValueMap.getProperty("defaultScriptingLanguage")));
            //当结果集中含有Null值时是否执行映射对象的setter或者Map对象的put方法。此设置对于原始类型如int,boolean等无效。
            configuration.setCallSettersOnNulls(booleanValueOf(settingsNameToValueMap.getProperty("callSettersOnNulls"), false));
            //logger名字的前缀
            configuration.setLogPrefix(settingsNameToValueMap.getProperty("logPrefix"));
            //显式定义用什么log框架，不定义则用默认的自动发现jar包机制
            configuration.setLogImpl(resolveClass(settingsNameToValueMap.getProperty("logImpl")));
            //配置工厂
            configuration.setConfigurationFactory(resolveClass(settingsNameToValueMap.getProperty("configurationFactory")));
        }
    }

    //7.环境
    //	<environments default="development">
    //	  <environment id="development">
    //	    <transactionManager type="JDBC">
    //	      <property name="..." value="..."/>
    //	    </transactionManager>
    //	    <dataSource type="POOLED">
    //	      <property name="driver" value="${driver}"/>
    //	      <property name="url" value="${url}"/>
    //	      <property name="username" value="${username}"/>
    //	      <property name="password" value="${password}"/>
    //	    </dataSource>
    //	  </environment>
    //	</environments>
    private void environmentsElement(XNode environmentsNode) throws Exception {
        if (environmentsNode != null) {
            if (environment == null) {
                //<environments default="development">
                environment = environmentsNode.getStringAttribute("default");
            }
            //可以配置多套数据库环境
            List<XNode> children = environmentsNode.getChildren();
            for (XNode child : children) {
                //<environment id="development">
                String id = child.getStringAttribute("id");
                //循环比较id是否就是指定的environment
                if (isSpecifiedEnvironment(id)) {
                    //7.1事务管理器
                    //<transactionManager type="JDBC">
                    //  <property name="..." value="..."/>
                    //</transactionManager>
                    XNode transactionManagerNode = child.evalNode("transactionManager");
                    TransactionFactory txFactory = transactionManagerElement(transactionManagerNode);
                    //7.2数据源
                    //<dataSource type="POOLED">
                    //  <property name="driver" value="${driver}"/>
                    //  <property name="url" value="${url}"/>
                    //  <property name="username" value="${username}"/>
                    //  <property name="password" value="${password}"/>
                    //</dataSource>
                    XNode dataSourceNode = child.evalNode("dataSource");
                    DataSourceFactory dsFactory = dataSourceElement(dataSourceNode);
                    DataSource dataSource = dsFactory.getDataSource();
                    Environment.Builder environmentBuilder = new Environment.Builder(id)
                            .transactionFactory(txFactory)
                            .dataSource(dataSource);
                    Environment environment = environmentBuilder.build();
                    configuration.setEnvironment(environment);
                }
            }
        }
    }

    //8.databaseIdProvider
    //可以根据不同数据库执行不同的SQL，sql要加databaseId属性
    //这个功能感觉不是很实用，真要多数据库支持，那SQL工作量将会成倍增长，用mybatis以后一般就绑死在一个数据库上了。但也是一个不得已的方法吧
    //可以参考org.apache.ibatis.submitted.multidb包里的测试用例
    //	<databaseIdProvider type="VENDOR">
    //	  <property name="SQL Server" value="sqlserver"/>
    //	  <property name="DB2" value="db2"/>
    //	  <property name="Oracle" value="oracle" />
    //	</databaseIdProvider>
    private void databaseIdProviderElement(XNode context) throws Exception {
        DatabaseIdProvider databaseIdProvider = null;
        if (context != null) {
            String type = context.getStringAttribute("type");
            // awful patch to keep backward compatibility
            //与老版本兼容
            if ("VENDOR".equals(type)) {
                type = "DB_VENDOR";
            }
            Properties properties = context.getChildrenAsProperties();
            //"DB_VENDOR"-->VendorDatabaseIdProvider
            databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
            databaseIdProvider.setProperties(properties);
        }
        Environment environment = configuration.getEnvironment();
        if (environment != null && databaseIdProvider != null) {
            //得到当前的databaseId，可以调用DatabaseMetaData.getDatabaseProductName()得到诸如"Oracle (DataDirect)"的字符串，
            //然后和预定义的property比较,得出目前究竟用的是什么数据库
            String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
            configuration.setDatabaseId(databaseId);
        }
    }

    //7.1事务管理器
    //<transactionManager type="JDBC">
    //  <property name="..." value="..."/>
    //</transactionManager>
    private TransactionFactory transactionManagerElement(XNode transactionManagerNode) throws Exception {
        if (transactionManagerNode != null) {
            String type = transactionManagerNode.getStringAttribute("type");
            //<property name="..." value="..."/>
            Properties props = transactionManagerNode.getChildrenAsProperties();
            //根据type="JDBC"解析返回适当的TransactionFactory,JDBC 别名已经被注册到别名注册器
            TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
            factory.setProperties(props);
            return factory;
        }
        throw new BuilderException("Environment declaration requires a TransactionFactory.");
    }

    //7.2数据源
    //<dataSource type="POOLED">
    //  <property name="driver" value="${driver}"/>
    //  <property name="url" value="${url}"/>
    //  <property name="username" value="${username}"/>
    //  <property name="password" value="${password}"/>
    //</dataSource>
    private DataSourceFactory dataSourceElement(XNode context) throws Exception {
        if (context != null) {
            String type = context.getStringAttribute("type");
            Properties props = context.getChildrenAsProperties();
            //根据type="POOLED"解析返回适当的DataSourceFactory
            Class<?> resolveClass = resolveClass(type);
            DataSourceFactory factory = (DataSourceFactory) resolveClass.newInstance();
            factory.setProperties(props);
            return factory;
        }
        throw new BuilderException("Environment declaration requires a DataSourceFactory.");
    }

    //9.类型处理器
    //	<typeHandlers>
    //	  <typeHandler handler="org.mybatis.example.ExampleTypeHandler"/>
    //	</typeHandlers>
    //or
    //	<typeHandlers>
    //	  <package name="org.mybatis.example"/>
    //	</typeHandlers>
    private void typeHandlerElement(XNode typeHandlersNode) throws Exception {
        if (typeHandlersNode != null) {
            //<typeHandler handler="org.mybatis.example.ExampleTypeHandler"/>
            //<package name="org.mybatis.example"/>
            List<XNode> children = typeHandlersNode.getChildren();
            for (XNode child : children) {
                //如果是package
                //<package name="org.mybatis.example"/>
                if ("package".equals(child.getName())) {
                    String typeHandlerPackage = child.getStringAttribute("name");
                    //（一）调用TypeHandlerRegistry.register，去包下找所有类
                    typeHandlerRegistry.register(typeHandlerPackage);
                } else {
                    //<typeHandler handler="org.mybatis.example.ExampleTypeHandler"/>
                    //如果是typeHandler
                    String javaTypeName = child.getStringAttribute("javaType");
                    String jdbcTypeName = child.getStringAttribute("jdbcType");
                    String fullClassName = child.getStringAttribute("handler");
                    Class<?> javaTypeClass = resolveClass(javaTypeName);
                    JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
                    Class<?> typeHandlerClass = resolveClass(fullClassName);
                    //（二）调用TypeHandlerRegistry.register(以下是3种不同的参数形式)
                    if (javaTypeClass != null) {
                        if (jdbcType == null) {
                            typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
                        } else {
                            typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
                        }
                    } else {
                        typeHandlerRegistry.register(typeHandlerClass);
                    }
                }
            }
        }
    }

    //10.映射器
    //	10.1使用类路径
    //	<mappers>
    //	  <mapper resource="org/mybatis/builder/AuthorMapper.xml"/>
    //	  <mapper resource="org/mybatis/builder/BlogMapper.xml"/>
    //	  <mapper resource="org/mybatis/builder/PostMapper.xml"/>
    //	</mappers>
    //
    //	10.2使用绝对url路径
    //	<mappers>
    //	  <mapper url="file:///var/mappers/AuthorMapper.xml"/>
    //	  <mapper url="file:///var/mappers/BlogMapper.xml"/>
    //	  <mapper url="file:///var/mappers/PostMapper.xml"/>
    //	</mappers>
    //
    //	10.3使用java类名
    //	<mappers>
    //	  <mapper class="org.mybatis.builder.AuthorMapper"/>
    //	  <mapper class="org.mybatis.builder.BlogMapper"/>
    //	  <mapper class="org.mybatis.builder.PostMapper"/>
    //	</mappers>
    //
    //	10.4自动扫描包下所有映射器
    //	<mappers>
    //	  <package name="org.mybatis.builder"/>
    //	</mappers>
    private void mapperElement(XNode mappersNode) throws Exception {
        if (mappersNode == null) {
            return;
        }
        List<XNode> children = mappersNode.getChildren();
        for (XNode child : children) {
            if ("package".equals(child.getName())) {
                //10.4自动扫描包下所有映射器
                String mapperPackage = child.getStringAttribute("name");
                //TODO
                configuration.addMappers(mapperPackage);
                continue;
            }


            //<mapper resource="org/mybatis/builder/AuthorMapper.xml"/>
            String resource = child.getStringAttribute("resource");
            //<mapper url="file:///var/mappers/AuthorMapper.xml"/>
            String url = child.getStringAttribute("url");
            //<mapper class="org.mybatis.builder.AuthorMapper"/>
            String mapperClass = child.getStringAttribute("class");
            Map<String, XNode> sqlFragments = configuration.getSqlFragments();

            if (resource != null && url == null && mapperClass == null) {
                //10.1使用类路径
                //<mapper resource="org/apache/ibatis/submitted/force_flush_on_select/Person.xml"/>
                ErrorContext.instance().resource(resource);
                InputStream inputStream = Resources.getResourceAsStream(resource);
                //映射器比较复杂，调用XMLMapperBuilder
                //注意在for循环里每个mapper都重新new一个XMLMapperBuilder，来解析
                XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, sqlFragments);
                //解析mapper文件
                mapperParser.parse();
                return;
            }

            if (resource == null && url != null && mapperClass == null) {
                //10.2使用绝对url路径
                ErrorContext.instance().resource(url);
                InputStream inputStream = Resources.getUrlAsStream(url);
                //映射器比较复杂，调用XMLMapperBuilder
                XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, sqlFragments);
                mapperParser.parse();
                return;
            }

            if (resource == null && url == null && mapperClass != null) {
                //10.3使用java类名
                Class<?> mapperInterface = Resources.classForName(mapperClass);
                //直接把这个映射加入配置
                configuration.addMapper(mapperInterface);
                return;
            }

            throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
        }
    }

    //比较id和environment是否相等
    private boolean isSpecifiedEnvironment(String id) {
        if (environment == null) {
            throw new BuilderException("No environment specified.");
        } else if (id == null) {
            throw new BuilderException("Environment requires an id attribute.");
        } else if (environment.equals(id)) {
            return true;
        }
        return false;
    }

}
