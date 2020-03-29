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
 * 处理结果也会存在该对象的一个属性中，处理的时候是循环结果记录处理，处理完之后通过该对象的属性get到最终的结果
 *
 * @author Clinton Begin
 */
public interface ResultHandler {

    /**
     * 处理结果，给一个结果上下文
     *
     * @param resultContext 结果上下文
     */
    void handleResult(ResultContext resultContext);
}
