package com.jd.logistics.cache.jedis.support;

import java.io.Closeable;

/**
 * 锁结果, 使用{@link #isSuccess}判断是否成功获取锁, 调用{@link #close}释放锁
 *
 * @author Y.Y.Zhao
 */
public interface LockState extends Closeable {
    /**
     * 锁定的keys
     */
    String[] getKeys();

    /**
     * 在consumer中可设置数据
     *
     * @param data 需要设置的数据
     */
    <T> void setData(T data);

    /**
     * 在 {@link Lock#lock} 后可获取数据
     *
     * @return consumer中设置的数据
     */
    <T> T getData();

    /**
     * 是否成功获得锁
     */
    boolean isSuccess();

    /**
     * 释放锁
     */
    @Override
    void close();
}