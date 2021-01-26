package com.jd.logistics.cache.redis.support;

import java.util.function.Consumer;

/**
 * 锁对象, 使用{@link #lock}方法开始获取锁
 *
 * @author Y.Y.Zhao
 */
public interface Lock {
    /**
     * 获取锁
     *
     * @param successConsumer 获取锁成功后执行
     * @return 锁结果, 不需要手动释放锁, 用于通过getData()获取数据
     */
    LockState lock(Consumer<LockState> successConsumer);

    /**
     * 获取锁
     *
     * @param successConsumer 获取锁成功后执行
     * @param failedConsumer  获取锁失败后执行
     * @return 锁结果, 不需要手动释放锁, 用于通过getData()获取数据
     */
    LockState lock(Consumer<LockState> successConsumer, Consumer<LockState> failedConsumer);
}