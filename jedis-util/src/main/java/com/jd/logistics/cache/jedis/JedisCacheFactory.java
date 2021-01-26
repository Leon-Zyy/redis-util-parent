package com.jd.logistics.cache.jedis;

import com.jd.logistics.cache.jedis.support.JedisCacheExt;
import com.jd.logistics.cache.jedis.support.DefaultJedisCacheExt;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.util.Pool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * JedisCacheFactory
 *
 * @author Y.Y.Zhao
 */
public final class JedisCacheFactory {
    private static final String REDIS_SENTINEL = "redis-sentinel";

    /**
     * 单例模式JedisCache
     *
     * @param address    连接地址, ip:port
     * @param password   redis密码
     * @param timeout    毫秒
     * @param poolConfig 连接池配置
     */
    public static JedisCache jedisCacheStandalone(String address, String password, int timeout, GenericObjectPoolConfig poolConfig) {
        HostAndPort hostAndPort = HostAndPort.parseString(address);
        JedisPool pool = password == null || "".equals(password.trim())
                ? new JedisPool(poolConfig, hostAndPort.getHost(), hostAndPort.getPort(), timeout)
                : new JedisPool(poolConfig, hostAndPort.getHost(), hostAndPort.getPort(), timeout, password);
        return (JedisCache) Proxy.newProxyInstance(
                JedisCacheFactory.class.getClassLoader(),
                new Class[]{JedisCache.class},
                new JedisInvocationHandler(pool)
        );
    }

    /**
     * 哨兵模式JedisCache
     *
     * @param addresses  sentinel地址列表, [ip1:port1, ip2:port2, ...]
     * @param masterName sentinel masterName
     * @param password   redis密码
     * @param timeout    毫秒
     * @param poolConfig 连接池配置
     */
    public static JedisCache jedisCacheSentinel(List<String> addresses, String masterName, String password, int timeout, GenericObjectPoolConfig poolConfig) {
        JedisSentinelPool pool = password == null || "".equals(password.trim())
                ? new JedisSentinelPool(masterName, new HashSet<>(addresses), poolConfig, timeout)
                : new JedisSentinelPool(masterName, new HashSet<>(addresses), poolConfig, timeout, password);
        return (JedisCache) Proxy.newProxyInstance(
                JedisCacheFactory.class.getClassLoader(),
                new Class[]{JedisCache.class},
                new JedisInvocationHandler(pool)
        );
    }

    /**
     * 根据URI创建JedisCache, 仅支持单例或者哨兵模式
     * <p>单例模式: redis://password@localhost:6379</p>
     * <p>哨兵模式: redis-sentinel://password@localhost:26379,localhost:26380#mymaster</p>
     *
     * @param uri        redis连接字符串
     * @param timeout    毫秒
     * @param poolConfig 连接池配置
     */
    public static JedisCache jedisCacheFromURI(String uri, int timeout, GenericObjectPoolConfig poolConfig) {
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
            return jedisCacheSentinel(Arrays.asList(addr.split(",")), masterName, password, timeout, poolConfig);
        } else {
            return jedisCacheStandalone(addr, password, timeout, poolConfig);
        }
    }

    private static class JedisInvocationHandler implements InvocationHandler {
        private Pool<Jedis> jedisPool;
        private JedisCacheExt defaultJedisCacheExt;

        private JedisInvocationHandler(Pool<Jedis> jedisPool) {
            this.jedisPool = jedisPool;
            this.defaultJedisCacheExt = new DefaultJedisCacheExt(jedisPool);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass().equals(JedisCacheExt.class)) {
                return method.invoke(defaultJedisCacheExt, args);
            } else {
                try (Jedis jedis = jedisPool.getResource()) {
                    return method.invoke(jedis, args);
                }
            }
        }
    }
}
