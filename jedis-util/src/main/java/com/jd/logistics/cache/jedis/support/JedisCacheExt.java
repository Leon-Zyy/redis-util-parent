package com.jd.logistics.cache.jedis.support;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.util.Pool;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 一些redis扩展
 * <p>
 * 推荐使用{@link #newLock}及其重载方法调用分布式锁
 *
 * @author Y.Y.Zhao
 */
public interface JedisCacheExt {
    /**
     * 批量获取, 返回Map仅包含有值的key
     */
    Map<String, String> mgetMap(String... keys);

    /**
     * 批量获取
     * <p>removeNullValue:true 移除所有value不存在key
     * <p>removeNullValue:false 保留value不存在的key, map中value值为null
     */
    Map<String, String> mgetMap(boolean removeNullValue, String... keys);

    /**
     * 同时设置多值及过期时间
     *
     * @param map           key value
     * @param expireSeconds 秒
     */
    void msetEx(Map<String, String> map, int expireSeconds);

    /**
     * 在pipeline中执行请求
     *
     * @param doInPipelined 创建管道后调用,可批量执行redis命令
     * @return 每个redis命令对应的返回值
     */
    List<Object> pipelined(Consumer<Pipeline> doInPipelined);

    /**
     * 尝试获取分布式锁, 必须手动调用{@link LockState#close()}
     *
     * @param keys 尝试获取锁keys
     * @return 锁结果, 使用isSuccess判断是否成功获取锁, 调用close释放锁
     */
    LockState tryLock(String... keys);

    /**
     * 尝试获取分布式锁, 必须手动调用{@link LockState#close()}
     *
     * @param waitMilliseconds 毫秒,尝试等待获取锁时间,小于等于0时不等待,如果设置建议大于50
     * @param keys             尝试获取锁keys
     * @return 锁结果, 使用isSuccess判断是否成功获取锁, 调用close释放锁
     */
    LockState tryLock(long waitMilliseconds, String... keys);

    /**
     * 尝试获取分布式锁, 必须手动调用{@link LockState#close()}
     *
     * @param waitMilliseconds 毫秒,尝试等待获取锁时间,小于等于0时不等待,如果设置建议大于50
     * @param expireSeconds    秒,锁过期时间
     * @param keys             尝试获取锁keys
     * @return 锁结果, 使用isSuccess判断是否成功获取锁, 调用close释放锁
     */
    LockState tryLock(long waitMilliseconds, int expireSeconds, String... keys);

    /**
     * 创建锁对象
     *
     * @param keys 尝试获取锁keys
     * @return 锁对象, 使用lock方法开始获取锁
     */
    Lock newLock(String... keys);

    /**
     * 创建锁对象
     *
     * @param waitMilliseconds 毫秒,尝试等待获取锁时间,小于等于0时不等待,如果设置建议大于50
     * @param keys             尝试获取锁keys
     * @return 锁对象, 使用lock方法开始获取锁
     */
    Lock newLock(long waitMilliseconds, String... keys);

    /**
     * 创建锁对象
     *
     * @param waitMilliseconds 毫秒,尝试等待获取锁时间,小于等于0时不等待,如果设置建议大于50
     * @param expireSeconds    秒,锁过期时间
     * @param keys             尝试获取锁keys
     * @return 锁对象, 使用lock方法开始获取锁
     */
    Lock newLock(long waitMilliseconds, int expireSeconds, String... keys);

    /**
     * 执行Lua脚本
     */
    <T> T evalLua(Lua lua, String[] keys, String... values);

    /**
     * 内部RedisCachePool实例
     */
    Pool<Jedis> getJedisPool();
}