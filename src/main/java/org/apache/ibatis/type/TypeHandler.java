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

package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * java类型 跟 sql类型的转换
 * 也就是dao的入参数跟出参数
 *
 * @author Clinton Begin
 */

/**
 * 类型处理器
 */
public interface TypeHandler<T> {

    /**
     * 设置 sql 语句参数值，定义当前数据如何保存到数据库中
     *
     * @param parameterIndex 参数的下标
     * @param parameter 参数
     * @param jdbcType 参数的类型
     */
    void setParameter(PreparedStatement ps, int parameterIndex, T parameter, JdbcType jdbcType) throws SQLException;

    /**
     * 从数据库中取出数据
     *
     * @param rs
     * @param columnName 按列mc获取
     * @return
     * @throws SQLException
     */
    T getResult(ResultSet rs, String columnName) throws SQLException;

    //取得结果,供普通select用
    T getResult(ResultSet rs, int columnIndex) throws SQLException;

    //取得结果,供SP用
    T getResult(CallableStatement cs, int columnIndex) throws SQLException;

}
