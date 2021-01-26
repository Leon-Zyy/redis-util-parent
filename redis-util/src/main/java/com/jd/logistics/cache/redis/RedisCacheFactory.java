package com.jd.logistics.cache.redis;

import com.jd.logistics.cache.redis.support.DefaultRedisCacheExt;
import com.jd.logistics.cache.redis.support.RedisCacheExt;
import com.jd.logistics.cache.redis.support.RedisConnectionPool;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.internal.HostAndPort;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RedisCache静态工厂, 用于配置生成RedisCache对象
 *
 * @author Y.Y.Zhao
 */
public final class RedisCacheFactory {
    private static final String REDIS_CLUSTER = "redis-cluster";

    /**
     * 单例模式RedisCache
     *
     * @param address    连接地址, ip:port
     * @param password   redis密码
     * @param timeout    毫秒
     * @param poolConfig 连接池配置
     */
    public static RedisCache redisCacheStandalone(String address, String password, long timeout, GenericObjectPoolConfig poolConfig) {
        HostAndPort hp = HostAndPort.parse(address);
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(hp.getHostText())
                .withPort(hp.getPort());
        if (password != null && !"".equals(password.trim())) {
            builder.withPassword(password);
        }
        RedisClient client = RedisClient.create(builder.build());
        client.setDefaultTimeout(Duration.ofMillis(timeout));
        RedisConnectionPool pool = new RedisConnectionPool(client, poolConfig);
        return redisCache(pool);
    }

    /**
     * 哨兵模式RedisCache
     *
     * @param addresses  sentinel地址列表, [ip1:port1, ip2:port2, ...]
     * @param masterName sentinel masterName
     * @param password   redis密码
     * @param timeout    毫秒
     * @param poolConfig 连接池配置
     */
    public static RedisCache redisCacheSentinel(List<String> addresses, String masterName, String password, long timeout, GenericObjectPoolConfig poolConfig) {
        RedisURI.Builder builder = RedisURI.builder()
                .withSentinelMasterId(masterName);
        if (password != null && !"".equals(password.trim())) {
            builder.withPassword(password);
        }
        for (String address : addresses) {
            HostAndPort hp = HostAndPort.parse(address);
            builder.withSentinel(hp.getHostText(), hp.getPort());
        }
        RedisClient client = RedisClient.create(builder.build());
        client.setDefaultTimeout(Duration.ofMillis(timeout));
        RedisConnectionPool pool = new RedisConnectionPool(client, poolConfig);
        return redisCache(pool);
    }

    /**
     * 根据URI创建RedisCache, 仅支持单例或者哨兵模式
     * <p>单例模式: redis://password@localhost:6379</p>
     * <p>哨兵模式: redis-sentinel://password@localhost:26379,localhost:26380#mymaster</p>
     * <p>集群模式: redis-cluster://password@localhost:6379,localhost:6380</p>
     *
     * @param uri        redis连接字符串
     * @param timeout    毫秒
     * @param poolConfig 连接池配置
     */
    public static RedisCache redisCacheFromURI(String uri, long timeout, GenericObjectPoolConfig poolConfig) {
        URI rUri = URI.create(uri.trim());
        if (REDIS_CLUSTER.equalsIgnoreCase(rUri.getScheme())) {
            String authority = rUri.getAuthority();
            String addr = authority, password = null;
            int idx = authority.lastIndexOf('@');
            if (idx >= 0) {
                password = authority.substring(0, idx);
                addr = authority.substring(idx + 1);
            }
            return redisCacheCluster(Arrays.asList(addr.split(",")), password, timeout, poolConfig);
        } else {
            RedisClient client = RedisClient.create(uri.trim());
            client.setDefaultTimeout(Duration.ofMillis(timeout));
            RedisConnectionPool pool = new RedisConnectionPool(client, poolConfig);
            return redisCache(pool);
        }
    }

    /**
     * 集群模式RedisCache
     *
     * @param addresses  cluster地址列表, [ip1:port1, ip2:port2, ...]
     * @param password   redis密码
     * @param timeout    毫秒
     * @param poolConfig 连接池配置
     */
    public static RedisCache redisCacheCluster(List<String> addresses, String password, long timeout, GenericObjectPoolConfig poolConfig) {
        List<RedisURI> uris = addresses.stream().map(addr -> {
            HostAndPort hp = HostAndPort.parse(addr);
            RedisURI uri = RedisURI.create(hp.getHostText(), hp.getPort());
            if (password != null && !"".equals(password.trim())) {
                uri.setPassword(password);
            }
            return uri;
        }).collect(Collectors.toList());
        RedisClusterClient clusterClient = RedisClusterClient.create(uris);
        clusterClient.setDefaultTimeout(Duration.ofMillis(timeout));
        RedisConnectionPool pool = new RedisConnectionPool(clusterClient, poolConfig);
        return redisCache(pool);
    }

    private static RedisCache redisCache(RedisConnectionPool pool) {
        return (RedisCache) Proxy.newProxyInstance(
                RedisCacheFactory.class.getClassLoader(),
                new Class[]{RedisCache.class},
                new RedisInvocationHandler(pool)
        );
    }

    private static class RedisInvocationHandler implements InvocationHandler {
        private RedisConnectionPool redisPool;
        private RedisCacheExt defaultRedisCacheExt;

        private RedisInvocationHandler(RedisConnectionPool redisPool) {
            this.redisPool = redisPool;
            this.defaultRedisCacheExt = new DefaultRedisCacheExt(redisPool);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass().equals(RedisCacheExt.class)) {
                return method.invoke(defaultRedisCacheExt, args);
            } else {
                return redisPool.sync(cmd -> {
                    try {
                        return method.invoke(cmd, args);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }
        }
    }
}