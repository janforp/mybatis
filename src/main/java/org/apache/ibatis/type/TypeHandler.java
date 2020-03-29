package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 类型处理器
 * java类型 跟 sql类型的转换
 * 也就是dao的入参数跟出参数
 *
 * 我们知道java有java的数据类型，数据库有数据库的数据类型，
 * 那么我们在往数据库中插入数据的时候是如何把java类型当做数据库类型插入数据库，
 * 在从数据库读取数据的时候又是如何把数据库类型当做java类型来处理呢？这中间必然要经过一个类型转换。
 * 在Mybatis中我们可以定义一个叫做TypeHandler类型处理器的东西，通过它可以实现Java类型跟数据库类型的相互转换
 *
 *
 * 建立了自己的TypeHandler之后就需要把它注册到Mybatis的配置文件中，让Mybatis能够识别并使用它。注册TypeHandler主要有两种方式，
 * 一种是通过在Mybatis配置文件中定义typeHandlers元素的子元素typeHandler来注册；另一种是通过在Mybatis配置文件中定义typeHandlers元素的子元素package来注册。
 * 使用typeHandler子元素注册时一次只能注册一个TypeHandler，而使用package子元素注册时，Mybatis会把指定包里面的所有TypeHandler都注册为TypeHandler。
 * 使用typeHandler子元素注册时我们需要通过它的handler属性来指明当前要注册的TypeHandler的全名称，这个属性是必须要的。另外还有两个附加属性可以指定，一个是javaType，
 * 用以指定对应的java类型；另一个是jdbcType，用以指定对应的jdbc类型。使用package子元素注册时需要我们通过它的name属性来指定要扫描的包，
 * 如果这个时候我们也需要指定对应TypeHandler的javaType和jdbcType的话就需要我们在TypeHandler类上使用注解来定义了。Mybatis注册TypeHandler最基本的方式就是建立一个javaType、
 * jdbcType和TypeHandler的对应关系。在使用typeHandler子元素进行注册的时候，有三种类型的注册方式：
 *
 * 1.如果我们指定了javaType和jdbcType，那么Mybatis会注册一个对应javaType和jdbcType的TypeHandler。
 *
 * 2.如果我们只指定了javaType属性，那么这个时候又分两种情况：
 *
 * （1）如果我们通过注解的形式在TypeHandler类上用@MappedJdbcTypes指定了对应的jdbcType，那么Mybatis会一一注册指定的javaType、jdbcType和TypeHandler的组合，
 * 也包括使用这种形式指定了jdbcType为null的情况。现假设我们有如下这样一个StringArrayTypeHandler：
 *
 *
 * 注意：
 * 1.要注意 MyBatis 不会通过检测数据库元信息来决定使用哪种类型，所以你必须在参数和结果映射中指明字段是 VARCHAR 类型， 以使其能够绑定到正确的类型处理器上。这是因为 MyBatis 直到语句被执行时才清楚数据类型。
 * 2.通过类型处理器的泛型，MyBatis 可以得知该类型处理器处理的 Java 类型，不过这种行为可以通过两种方法改变：
 *
 * 在类型处理器的配置元素（typeHandler 元素）上增加一个 javaType 属性（比如：javaType="String"）；
 * 在类型处理器的类上增加一个 @MappedTypes 注解指定与其关联的 Java 类型列表。 如果在 javaType 属性中也同时指定，则注解上的配置将被忽略。
 * 可以通过两种方式来指定关联的 JDBC 类型：
 *
 * 在类型处理器的配置元素上增加一个 jdbcType 属性（比如：jdbcType="VARCHAR"）；
 * 在类型处理器的类上增加一个 @MappedJdbcTypes 注解指定与其关联的 JDBC 类型列表。 如果在 jdbcType 属性中也同时指定，则注解上的配置将被忽略。
 *
 * @param <T> 是值该处理器处理的java类型 javaType
 * @author Clinton Begin
 */
public interface TypeHandler<T> {

    /**
     * 用于定义在Mybatis设置参数时该如何把Java类型的参数转换为对应的数据库类型
     *
     * @param preparedStatement 当前的PreparedStatement对象
     * @param parameterIndex 当前参数的位置
     * @param parameter 当前参数的Java对象
     * @param jdbcType 当前参数的数据库类型
     * @throws SQLException 异常
     */
    void setParameter(PreparedStatement preparedStatement, int parameterIndex, T parameter, JdbcType jdbcType) throws SQLException;

    /**
     * 用于在Mybatis获取数据结果集时如何把数据库类型转换为对应的Java类型
     *
     * @param rs 当前的结果集
     * @param columnName 当前的字段名称
     * @return 转换后的Java对象
     * @throws SQLException 异常
     */
    T getResult(ResultSet rs, String columnName) throws SQLException;

    /**
     * 用于在Mybatis通过字段位置获取字段数据时把数据库类型转换为对应的Java类型
     *
     * @param rs 当前的结果集
     * @param columnIndex 当前字段的位置
     * @return 转换后的Java对象
     * @throws SQLException 异常
     */
    T getResult(ResultSet rs, int columnIndex) throws SQLException;

    /**
     * 取得结果,供SP用
     * 用于Mybatis在调用存储过程后把数据库类型的数据转换为对应的Java类型
     *
     * @param cs 当前的CallableStatement执行后的CallableStatement
     * @param columnIndex 当前输出参数的位置
     * @return 结果
     * @throws SQLException 异常
     */
    T getResult(CallableStatement cs, int columnIndex) throws SQLException;
}
