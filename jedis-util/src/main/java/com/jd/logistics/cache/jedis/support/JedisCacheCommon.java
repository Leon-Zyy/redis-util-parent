package com.jd.logistics.cache.jedis.support;

import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.MultiKeyCommands;
import redis.clients.jedis.ScriptingCommands;

/**
 * JedisCacheCommon
 *
 * @author Y.Y.Zhao
 */
public interface JedisCacheCommon extends JedisCommands, MultiKeyCommands, ScriptingCommands {
}
