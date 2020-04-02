package org.apache.ibatis.binding;

import lombok.Getter;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * DAO/MAPPER.class 中定义的函数
 *
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 */
public class MapperMethod {

    /**
     * 一个静态内部类
     *
     * sql命令--一旦赋值，就永远是该值。
     */
    private final SqlCommand command;

    /**
     * 一个静态内部类
     *
     * 方法签名--一旦赋值，就永远是该值。
     */
    private final MethodSignature method;

    /**
     * 实例化
     *
     * @param mapperInterface mapper接口
     * @param method mapper接口的一个方法
     * @param config 配置
     */
    public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
        this.command = new SqlCommand(config, mapperInterface, method);
        this.method = new MethodSignature(config, method);
    }

    /**
     * 如果是如果mapper操作的数据库，最终都会通过该函数
     *
     * @param sqlSession sqlSession
     * @param args 参数
     * @return 执行结果
     */
    public Object execute(SqlSession sqlSession, Object[] args) {
        Object result;
        //可以看到执行时就是4种情况，insert|update|delete|select，分别调用SqlSession的4大类方法
        SqlCommandType sqlCommandType = command.getType();
        String commandName = command.getName();
        if (SqlCommandType.INSERT == sqlCommandType) {

            //sql 执行的参数
            Object param = method.convertArgsToSqlCommandParam(args);
            int affectedRowCount = sqlSession.insert(commandName, param);
            //没什么逻辑
            result = rowCountResult(affectedRowCount);

        } else if (SqlCommandType.UPDATE == sqlCommandType) {

            Object param = method.convertArgsToSqlCommandParam(args);
            int update = sqlSession.update(commandName, param);
            //没什么逻辑
            result = rowCountResult(update);

        } else if (SqlCommandType.DELETE == sqlCommandType) {

            Object param = method.convertArgsToSqlCommandParam(args);
            int delete = sqlSession.delete(commandName, param);
            result = rowCountResult(delete);

        } else if (SqlCommandType.SELECT == sqlCommandType) {

            if (method.returnsVoid() && method.hasResultHandler()) {
                //如果有结果处理器
                executeWithResultHandler(sqlSession, args);
                result = null;

            } else if (method.returnsMany()) {
                //如果结果有多条记录
                result = executeForMany(sqlSession, args);

            } else if (method.returnsMap()) {
                //如果结果是map
                result = executeForMap(sqlSession, args);

            } else {
                //否则就是一条记录

                //查询需要的入参数
                Object param = method.convertArgsToSqlCommandParam(args);
                result = sqlSession.selectOne(commandName, param);
            }

        } else {
            throw new BindingException("Unknown execution method for: " + commandName);
        }
        //如果结果是null,但是方法用原始类型接收，则报错
        if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
            throw new BindingException("Mapper method '" + commandName
                    + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
        }
        return result;
    }

    //这个方法对返回值的类型进行了一些检查，使得更安全
    private Object rowCountResult(int affectedRowCount) {
        final Object result;

        //方法返回类型
        Class<?> methodReturnType = method.getReturnType();
        if (method.returnsVoid()) {
            result = null;

        } else if (Integer.class.equals(methodReturnType) || Integer.TYPE.equals(methodReturnType)) {
            //如果返回值是大int或小int
            result = affectedRowCount;

        } else if (Long.class.equals(methodReturnType) || Long.TYPE.equals(methodReturnType)) {
            //如果返回值是大long或小long
            result = (long) affectedRowCount;

        } else if (Boolean.class.equals(methodReturnType) || Boolean.TYPE.equals(methodReturnType)) {
            //如果返回值是大boolean或小boolean
            result = affectedRowCount > 0;
        } else {

            throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: " + methodReturnType);
        }
        return result;
    }

    /**
     * 结果处理器
     */
    private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
        //如：org.apache.ibatis.submitted.sptests.SPMapper.adderAsSelect
        String statement = command.getName();
        //从映射中拿到，拿不到则报错
        MappedStatement mappedStatement = sqlSession.getConfiguration().getMappedStatement(statement);
        List<ResultMap> resultMapList = mappedStatement.getResultMaps();
        ResultMap resultMap = resultMapList.get(0);
        //resultMap的映射类型
        Class<?> type = resultMap.getType();
        //TODO ？
        //resultMap的映射类型不能为空，就是必须要配置 type
        if (void.class.equals(type)) {
            throw new BindingException("method " + statement
                    + " needs either a @ResultMap annotation, a @ResultType annotation,"
                    + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
        }
        Object param = method.convertArgsToSqlCommandParam(args);

        //根据入参数下标找到结果处理器，这个参数是接口传进去的参数，跟分页参数类似的获取方式
        ResultHandler paramOfResultHandler = method.extractResultHandler(args);
        if (method.hasRowBounds()) {
            //根据入参数看是否需要分页，如果需要则返回分页参数，否则返回null
            RowBounds paramOfRowBounds = method.extractRowBounds(args);
            //调用分页接口
            sqlSession.select(statement, param, paramOfRowBounds, paramOfResultHandler);
        } else {
            //不分页
            sqlSession.select(statement, param, paramOfResultHandler);
        }
    }

    //多条记录
    private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
        List<E> result;
        //如：org.apache.ibatis.submitted.sptests.SPMapper.adderAsSelect
        Object param = method.convertArgsToSqlCommandParam(args);
        //如：org.apache.ibatis.submitted.sptests.SPMapper.adderAsSelect
        String commandName = command.getName();
        if (method.hasRowBounds()) {
            RowBounds rowBounds = method.extractRowBounds(args);
            result = sqlSession.selectList(commandName, param, rowBounds);
        } else {
            result = sqlSession.selectList(commandName, param);
        }
        // issue #510 Collections & arrays support
        //如果查询结果类型跟方法返回类型不一样
        if (!method.getReturnType().isAssignableFrom(result.getClass())) {
            if (method.getReturnType().isArray()) {//数组
                return convertToArray(result);
            } else {
                return convertToDeclaredCollection(sqlSession.getConfiguration(), result);//集合
            }
        }
        return result;
    }

    private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
        ObjectFactory objectFactory = config.getObjectFactory();
        Class<?> returnType = method.getReturnType();
        Object collection = objectFactory.create(returnType);
        MetaObject metaObject = config.newMetaObject(collection);
        metaObject.addAll(list);
        return collection;
    }

    @SuppressWarnings("unchecked")
    private <E> E[] convertToArray(List<E> list) {
        //method.getReturnType().isArray() 的时候进来这里
        E[] array = (E[]) Array.newInstance(method.getReturnType().getComponentType(), list.size());
        array = list.toArray(array);
        return array;
    }

    private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
        Map<K, V> result;
        Object param = method.convertArgsToSqlCommandParam(args);
        if (method.hasRowBounds()) {
            RowBounds rowBounds = method.extractRowBounds(args);
            result = sqlSession.selectMap(command.getName(), param, method.getMapKey(), rowBounds);
        } else {
            result = sqlSession.selectMap(command.getName(), param, method.getMapKey());
        }
        return result;
    }

    //参数map，静态内部类,更严格的get方法，如果没有相应的key，报错
    public static class ParamMap<V> extends HashMap<String, V> {

        private static final long serialVersionUID = -2212268410512043556L;

        @Override
        public V get(Object key) {
            if (!super.containsKey(key)) {
                throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
            }
            return super.get(key);
        }
    }

    //SQL命令，静态内部类
    public static class SqlCommand {

        /**
         * 如：org.apache.ibatis.submitted.sptests.SPMapper.adderAsSelect
         * 大部分情况就是 statement，
         * 具体到 namespace + methodName 是唯一的
         */
        @Getter
        private final String name;

        /**
         * UNKNOWN, INSERT, UPDATE, DELETE, SELECT
         */
        @Getter
        private final SqlCommandType type;

        /**
         * 接口 mapperInterface 的方法 method 对应的 sql 命令
         * 把该方法对应的  statement 准备好，放到配置对象 configuration
         *
         * @param configuration 配置
         * @param mapperInterface DAO
         * @param method DAO方法
         */
        public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
            String statementName = mapperInterface.getName() + "." + method.getName();//org.apache.ibatis.submitted.sptests.SPMapper.adderAsSelect
            //创建 映射的语句
            MappedStatement mappedStatement = null;
            if (configuration.hasStatement(statementName)) {//加载配置的时候就进去了
                //创建并返回 mappedStatement
                mappedStatement = configuration.getMappedStatement(statementName);
            } else if (!mapperInterface.equals(method.getDeclaringClass().getName())) { // issue #35
                //如果不是这个mapper接口的方法，再去查父类
                String parentStatementName = method.getDeclaringClass().getName() + "." + method.getName();
                if (configuration.hasStatement(parentStatementName)) {
                    mappedStatement = configuration.getMappedStatement(parentStatementName);
                }
            }
            if (mappedStatement == null) {
                throw new BindingException("Invalid bound statement (not found): " + statementName);
            }
            name = mappedStatement.getId();//org.apache.ibatis.submitted.sptests.SPMapper.adderAsSelect
            type = mappedStatement.getSqlCommandType();//SELECT
            if (type == SqlCommandType.UNKNOWN) {
                throw new BindingException("Unknown execution method for: " + name);
            }
        }
    }

    /**
     * 方法签名，静态内部类
     */
    public static class MethodSignature {

        private final boolean returnsMany;

        private final boolean returnsMap;

        private final boolean returnsVoid;

        @Getter
        private final Class<?> returnType;

        @Getter
        private final String mapKey;

        private final Integer resultHandlerIndex;

        private final Integer rowBoundsIndex;

        /**
         * 是否有 @Param 注解
         */
        private final boolean hasNamedParameters;

        /**
         * 该方法的参数下标跟名称或者序号的映射
         */
        private final SortedMap<Integer, String> paramsIndexMap;

        public MethodSignature(Configuration configuration, Method method) {
            this.returnType = method.getReturnType();
            this.returnsVoid = void.class.equals(this.returnType);
            this.returnsMany = (configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray());
            //如果返回是 map, 使用什么作为 map 的 key
            this.mapKey = getMapKey(method);
            this.returnsMap = (this.mapKey != null);
            //是否有 @Param 注解
            this.hasNamedParameters = hasNamedParams(method);
            //以下重复循环2遍调用getUniqueParamIndex，是不是降低效率了
            //记下RowBounds是第几个参数
            this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
            //记下ResultHandler是第几个参数
            this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);

            //该方法的参数下标跟名称或者序号的映射
            //{0:"subjectQuery",1:"bodyQuery"}
            //key:下标。value:名称或者下标
            SortedMap<Integer, String> params = getParamsIndexMap(method, this.hasNamedParameters);
            this.paramsIndexMap = Collections.unmodifiableSortedMap(params);
        }

        /**
         * 多个参数返回 map
         * 这个内部类是对映射器接口中的方法的封装，
         * 其核心功能就是convertArgsToSqlCommandParam(Object[] args)方法，用于将方法中的参数转换成为SQL脚本命令中的参数形式，
         * 其实就是将参数位置作为键，具体的参数作为值保存到一个Map集合中，这样在SQL脚本命令中用键#{1}通过集合就能得到具体的参数。
         *
         * 返回：null/一个具体的值如：1或者字符串张三/一个map如：{"subjectQuery":"%a%","bodyQuery":"%a%","param1":"%a%","param2":"%a%"}
         *
         * @param args 入参数
         * @return 多个参数返回
         */
        public Object convertArgsToSqlCommandParam(Object[] args) {
            //该方法参数数量，不包括分页参数跟结果处理器参数
            final int paramCount = paramsIndexMap.size();
            if (args == null || paramCount == 0) {
                //如果没参数
                return null;
            } else if (!hasNamedParameters && paramCount == 1) {//没有 @Param 注解，并且只有唯一的参数
                //如果只有一个参数
                Set<Integer> indexSet = paramsIndexMap.keySet();
                Integer firstParamIndex = indexSet.iterator().next();
                return args[firstParamIndex];
            } else {
                //否则，返回一个ParamMap，修改参数名，参数名就是其位置

                //根据 key get 不到值就会抛出异常
                final Map<String, Object> finalParamMap = new ParamMap<Object>();
                int i = 0;
                Set<Map.Entry<Integer, String>> entries = paramsIndexMap.entrySet();
                for (Map.Entry<Integer, String> entry : entries) {
                    //参数在方法中的位置，从0开始
                    Integer paramIndex = entry.getKey();
                    //如果不是 @Param 参数，则与 paramIndex 相同
                    String paramIndexOrParamName = entry.getValue();

                    //真正的参数值
                    Object argValue = args[paramIndex];
                    //1.先加一个#{0},#{1},#{2}...参数
                    finalParamMap.put(paramIndexOrParamName, argValue);
                    // issue #71, add finalParamMap names as param1, param2...but ensure backward compatibility

                    //下面的逻辑已经不用了
                    final String genericParamName = "finalParamMap" + (i + 1);
                    if (!finalParamMap.containsKey(genericParamName)) {
                        //2.再加一个#{param1},#{param2}...参数
                        //你可以传递多个参数给一个映射器方法。如果你这样做了,
                        //默认情况下它们将会以它们在参数列表中的位置来命名,比如:#{param1},#{param2}等。
                        //如果你想改变参数的名称(只在多参数情况下) ,那么你可以在参数上使用@Param(“paramName”)注解。
                        //此处为了向老版本兼容，故在map中保存着两份参数，一份是老版的与#{0}为键的参数另一种为以#{param1}为键的参数。
                        finalParamMap.put(genericParamName, argValue);
                    }
                    i++;
                }
                //{"subjectQuery":"%a%","bodyQuery":"%a%","param1":"%a%","param2":"%a%"}
                return finalParamMap;
            }
        }

        public boolean hasRowBounds() {
            return rowBoundsIndex != null;
        }

        public RowBounds extractRowBounds(Object[] args) {
            //根据入参数的下标找到分页参数
            RowBounds rowBounds = (RowBounds) args[rowBoundsIndex];
            //如果不分页返回null
            return hasRowBounds() ? rowBounds : null;
        }

        public boolean hasResultHandler() {
            return resultHandlerIndex != null;
        }

        public ResultHandler extractResultHandler(Object[] args) {
            //根据入参数的下标找到
            ResultHandler resultHandler = (ResultHandler) args[resultHandlerIndex];
            return hasResultHandler() ? resultHandler : null;
        }

        public boolean returnsMany() {
            return returnsMany;
        }

        public boolean returnsMap() {
            return returnsMap;
        }

        public boolean returnsVoid() {
            return returnsVoid;
        }

        /**
         * 根据参数类型获取指定参数的下标，一般用来获取分页参数，结果处理器的下标，
         * 将来在 sqlSession 查询的时候需要使用这些参数，就可以通过下标来获取指定类型的参数了，
         * 这里可以看出，一个mapper方法不能传2个分页参数，也不能传2个结果处理器
         *
         * @param method 方法
         * @param paramType 参数类型
         * @return 获取指定参数的下标
         */
        private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
            Integer index = null;
            final Class<?>[] argTypes = method.getParameterTypes();
            for (int i = 0; i < argTypes.length; i++) {
                if (paramType.isAssignableFrom(argTypes[i])) {
                    if (index == null) {
                        index = i;
                    } else {
                        //参数类型重复？
                        throw new BindingException(method.getName() + " cannot have multiple " + paramType.getSimpleName() + " parameters");
                    }
                }
            }
            return index;
        }

        private String getMapKey(Method method) {
            String mapKey = null;
            if (Map.class.isAssignableFrom(method.getReturnType())) {
                //如果返回类型是map类型的，查看该method是否有MapKey注解。如果有这个注解，将这个注解的值作为map的key
                final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
                if (mapKeyAnnotation != null) {
                    mapKey = mapKeyAnnotation.value();
                }
            }
            return mapKey;
        }

        /**
         * 得到所有参数，并且把他们放到有序映射
         *
         * @param method 方法
         * @param hasNamedParameters 是否 @Param
         * @return 得到所有参数
         */
        private SortedMap<Integer, String> getParamsIndexMap(Method method, boolean hasNamedParameters) {
            //用一个TreeMap,这样就保证还是按参数的先后顺序
            final SortedMap<Integer, String> params = new TreeMap<Integer, String>();
            //该方法的入参数类型数组
            final Class<?>[] argTypes = method.getParameterTypes();
            for (int i = 0; i < argTypes.length; i++) {
                //是否不是RowBounds/ResultHandler类型的参数，意思就是过滤这2个类型的参数
                if (!RowBounds.class.isAssignableFrom(argTypes[i]) && !ResultHandler.class.isAssignableFrom(argTypes[i])) {
                    //参数名字默认为0,1,2，这就是为什么xml里面可以用#{1}这样的写法来表示参数了

                    //如果是第一个参数，则 paraName = 0
                    String paramName = String.valueOf(params.size());
                    if (hasNamedParameters) {
                        //还可以用注解@Param来重命名参数
                        //获取 @Param("id") 中的 value id
                        paramName = getParamNameFromAnnotation(method, i, paramName);
                    }
                    //如果有注解则 paramName = id,否则 paramName = 0/1/2...
                    params.put(i, paramName);
                }
            }
            return params;
        }

        /**
         * 获取 @Param("id") 中的 value id
         *
         * @param method mapper中的方法
         * @param paramIndex 方法中参数的下标
         * @param paramName 参数名称
         * @return 获取 @Param("id") 中的 value id
         */
        private String getParamNameFromAnnotation(Method method, int paramIndex, String paramName) {
            Annotation[][] methodParameterAnnotations = method.getParameterAnnotations();

            //该参数的注解数组，如果没有注解，则可能为空数组
            final Object[] paramAnnos = methodParameterAnnotations[paramIndex];
            for (Object paramAnno : paramAnnos) {
                if (paramAnno instanceof Param) {
                    //拿到 value
                    paramName = ((Param) paramAnno).value();
                }
            }
            return paramName;
        }

        /**
         * 是否有@Param注解
         *
         * @param method mapper接口的一个方法
         * @return 是否有@Param注解
         */
        private boolean hasNamedParams(Method method) {
            boolean hasNamedParams = false;
            final Object[][] parameterAnnotations = method.getParameterAnnotations();//获取参数的注解
            for (Object[] paramAnnotation : parameterAnnotations) {
                for (Object aParamAnnotation : paramAnnotation) {
                    if (aParamAnnotation instanceof Param) {
                        //查找@Param注解,一般不会用注解吧，可以忽略
                        hasNamedParams = true;
                        break;
                    }
                }
            }
            return hasNamedParams;
        }
    }
}
