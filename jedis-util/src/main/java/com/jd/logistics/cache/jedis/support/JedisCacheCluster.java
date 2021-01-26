package com.jd.logistics.cache.jedis.support;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.ScanResult;

import java.util.List;
import java.util.Set;

/**
 * 以下方法, 在集群模式下不支持
 * <p> {@link #blpop(String...)}
 * <p> {@link #brpop(String...)}
 * <p> {@link #watch(String...)}
 * <p> {@link #unwatch()}
 * <p> {@link #randomKey()}
 * <p> {@link #scan(int)}
 * <p> {@link #scan(String)}
 * <p> {@link #eval(String)}
 * <p> {@link #evalsha(String)}
 * <p> {@link #scriptExists(String)}
 * <p> {@link #scriptExists(String...)}
 * <p> {@link #scriptLoad(String)}
 *
 * @author Y.Y.Zhao
 */
public class JedisCacheCluster extends JedisCluster implements JedisCacheCommon {
    public JedisCacheCluster(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout, int maxAttempts, String password, GenericObjectPoolConfig poolConfig) {
        super(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, password, poolConfig);
    }

    public JedisCacheCluster(Set<HostAndPort> nodes, int timeout, GenericObjectPoolConfig poolConfig) {
        super(nodes, timeout, poolConfig);
    }

    @Override
    public List<String> blpop(String... keys) {
        return null;
    }

    @Override
    public List<String> brpop(String... args) {
        return null;
    }

    @Override
    public String watch(String... keys) {
        return null;
    }

    @Override
    public String unwatch() {
        return null;
    }

    @Override
    public String randomKey() {
        return null;
    }

    @Override
    @Deprecated
    public ScanResult<String> scan(int cursor) {
        return null;
    }

    @Override
    public ScanResult<String> scan(String cursor) {
        return null;
    }

    @Override
    public Object eval(String script) {
        return null;
    }

    @Override
    public Object evalsha(String sha1) {
        return null;
    }

    @Override
    public Boolean scriptExists(String sha1) {
        return null;
    }

    @Override
    public List<Boolean> scriptExists(String... sha1) {
        return null;
    }

    @Override
    public String scriptLoad(String script) {
        return null;
    }
}
