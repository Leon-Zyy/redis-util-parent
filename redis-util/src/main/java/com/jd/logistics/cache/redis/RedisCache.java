package com.jd.logistics.cache.redis;

import com.jd.logistics.cache.redis.support.RedisCacheExt;
import com.jd.logistics.cache.redis.support.RedisCacheSync;

/**
 * 基于lettuce包装的redis缓存实现
 * <p>
 * 封装了资源池获取和释放, 可直接使用api
 *
 * @author Y.Y.Zhao
 */
public interface RedisCache extends RedisCacheSync, RedisCacheExt {
}