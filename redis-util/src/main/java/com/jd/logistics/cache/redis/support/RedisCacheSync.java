package com.jd.logistics.cache.redis.support;

import io.lettuce.core.api.sync.*;

/**
 * 基于lettuce包装的redis缓存实现(同步)
 *
 * @author Y.Y.Zhao
 */
public interface RedisCacheSync extends
        BaseRedisCommands<String, String>,
        RedisServerCommands<String, String>,
        RedisStreamCommands<String, String>,
        RedisGeoCommands<String, String>,
        RedisHashCommands<String, String>,
        RedisHLLCommands<String, String>,
        RedisKeyCommands<String, String>,
        RedisListCommands<String, String>,
        RedisScriptingCommands<String, String>,
        RedisSetCommands<String, String>,
        RedisSortedSetCommands<String, String>,
        RedisStringCommands<String, String> {
}