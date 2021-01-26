package com.jd.logistics.cache.redis.support;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * RedisPoolConfig
 *
 * @author Y.Y.Zhao
 */
public class RedisPoolConfig extends GenericObjectPoolConfig {
    public RedisPoolConfig() {
        setTestWhileIdle(true);
        setMinEvictableIdleTimeMillis(60000);
        setTimeBetweenEvictionRunsMillis(30000);
        setNumTestsPerEvictionRun(-1);
    }
}