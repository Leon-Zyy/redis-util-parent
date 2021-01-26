package com.jd.logistics.cache.redis.support;

import io.lettuce.core.api.async.*;

/**
 * 基于lettuce包装的redis缓存实现(异步)
 *
 * @author Y.Y.Zhao
 */
public interface RedisCacheAsync extends
        BaseRedisAsyncCommands<String, String>,
        RedisServerAsyncCommands<String, String>,
        RedisStreamAsyncCommands<String, String>,
        RedisGeoAsyncCommands<String, String>,
        RedisHashAsyncCommands<String, String>,
        RedisHLLAsyncCommands<String, String>,
        RedisKeyAsyncCommands<String, String>,
        RedisListAsyncCommands<String, String>,
        RedisScriptingAsyncCommands<String, String>,
        RedisSetAsyncCommands<String, String>,
        RedisSortedSetAsyncCommands<String, String>,
        RedisStringAsyncCommands<String, String> {
}