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

package org.apache.ibatis.session;

/**
 * 结果处理器
 *
 * 具体用法：https://www.jianshu.com/p/8773d0e786d8
 *
 * 使用 ResultHandler 的时候需要注意以下两个限制：
 *
 * 使用带 ResultHandler 参数的方法时，收到的数据不会被缓存。
 *
 * queryStack++;
 * //先根据cachekey从localCache去查，如果有 resultHandler ，则无法使用缓存
 * list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
 * if (list != null) {
 * //若查到localCache缓存，处理localOutputParameterCache
 * handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
 * } else {
 * //从数据库查
 * list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
 * }
 *
 * 当使用高级的结果映射集（resultMap）时，MyBatis 很可能需要数行结果来构造一个对象。
 * 如果你使用了 ResultHandler，你可能会接收到关联（association）或者集合（collection）中尚未被完整填充的对象。
 *
 * @author Clinton Begin
 */
public interface ResultHandler {

    //处理结果，给一个结果上下文
    void handleResult(ResultContext context);
}
