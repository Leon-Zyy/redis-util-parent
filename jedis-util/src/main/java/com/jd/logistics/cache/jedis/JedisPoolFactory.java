package com.jd.logistics.cache.jedis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.util.Pool;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * JedisFactory
 *
 * @author Y.Y.Zhao
 */
public class JedisPoolFactory {
    private static final String REDIS_SENTINEL = "redis-sentinel";

    /**
     * 单例模式
     *
     * @param address    连接地址, ip:port
     * @param password   redis密码
     * @param timeout    毫秒
     * @param poolConfig 连接池配置
     */
    public static Pool<Jedis> jedisPool(String address, String password,
                                        int timeout, GenericObjectPoolConfig poolConfig) {
        HostAndPort hostAndPort = HostAndPort.parseString(address);
        return password == null || "".equals(password.trim())
                ? new JedisPool(poolConfig, hostAndPort.getHost(), hostAndPort.getPort(), timeout)
                : new JedisPool(poolConfig, hostAndPort.getHost(), hostAndPort.getPort(), timeout, password);
    }

    /**
     * 哨兵模式
     *
     * @param addresses  sentinel地址列表, [ip1:port1, ip2:port2, ...]
     * @param masterName sentinel masterName
     * @param password   redis密码
     * @param timeout    毫秒
     * @param poolConfig 连接池配置
     */
    public static Pool<Jedis> jedisSentinelPool(List<String> addresses, String masterName, String password,
                                                int timeout, GenericObjectPoolConfig poolConfig) {
        return password == null || "".equals(password.trim())
                ? new JedisSentinelPool(masterName, new HashSet<>(addresses), poolConfig, timeout)
                : new JedisSentinelPool(masterName, new HashSet<>(addresses), poolConfig, timeout, password);
    }

    /**
     * 根据URI创建Pool&lt;Jedis&gt;, 仅支持单例或者哨兵模式
     * <p>单例模式: redis://password@localhost:6379</p>
     * <p>哨兵模式: redis-sentinel://password@localhost:26379,localhost:26380#mymaster</p>
     *
     * @param uri        redis连接字符串
     * @param timeout    毫秒
     * @param poolConfig 连接池配置
     */
    public static Pool<Jedis> jedisPoolFromURI(String uri, int timeout, GenericObjectPoolConfig poolConfig) {
        URI rUri = URI.create(uri.trim());
        String authority = rUri.getAuthority();
        String addr = authority, password = null;
        int idx = authority.lastIndexOf('@');
        if (idx >= 0) {
            password = authority.substring(0, idx);
            addr = authority.substring(idx + 1);
        }
        if (REDIS_SENTINEL.equalsIgnoreCase(rUri.getScheme())) {
            String masterName = rUri.getFragment();
            return jedisSentinelPool(Arrays.asList(addr.split(",")), masterName, password, timeout, poolConfig);
        } else {
            return jedisPool(addr, password, timeout, poolConfig);
        }
    }
}
