package com.jd.logistics.cache.redis.support;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 连接池
 *
 * @author Y.Y.Zhao
 */
public class RedisConnectionPool {
    private final GenericObjectPool<StatefulConnection<String, String>> lettucePool;
    private final boolean cluster;

    public RedisConnectionPool(RedisClient client, GenericObjectPoolConfig config) {
        this.cluster = false;
        this.lettucePool = ConnectionPoolSupport.createGenericObjectPool(client::connect, config, false);
    }

    public RedisConnectionPool(RedisClusterClient clusterClient, GenericObjectPoolConfig config) {
        this.cluster = true;
        this.lettucePool = ConnectionPoolSupport.createGenericObjectPool(clusterClient::connect, config, false);
    }

    public boolean isCluster() {
        return cluster;
    }

    StatefulConnection<String, String> borrowConnection() {
        try {
            return lettucePool.borrowObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void returnConnection(StatefulConnection<String, String> conn) {
        lettucePool.returnObject(conn);
    }

    public <T> T connect(Function<StatefulConnection<String, String>, T> doInConnection) {
        StatefulConnection<String, String> conn = null;
        try {
            conn = this.lettucePool.borrowObject();
            return doInConnection.apply(conn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                lettucePool.returnObject(conn);
            }
        }
    }

    public void connect(Consumer<StatefulConnection<String, String>> doInConnection) {
        StatefulConnection<String, String> conn = null;
        try {
            conn = this.lettucePool.borrowObject();
            doInConnection.accept(conn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                lettucePool.returnObject(conn);
            }
        }
    }

    public <T> T sync(Function<RedisClusterCommands<String, String>, T> doInSync) {
        StatefulConnection<String, String> conn = null;
        try {
            conn = this.lettucePool.borrowObject();
            RedisClusterCommands<String, String> commands = getSyncCommands(conn, this.cluster);
            return doInSync.apply(commands);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                lettucePool.returnObject(conn);
            }
        }
    }

    public void sync(Consumer<RedisClusterCommands<String, String>> doInSync) {
        StatefulConnection<String, String> conn = null;
        try {
            conn = this.lettucePool.borrowObject();
            RedisClusterCommands<String, String> commands = getSyncCommands(conn, this.cluster);
            doInSync.accept(commands);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                lettucePool.returnObject(conn);
            }
        }
    }

    public static RedisClusterCommands<String, String> getSyncCommands(StatefulConnection<String, String> conn, boolean isCluster) {
        return isCluster
                ? ((StatefulRedisClusterConnection<String, String>) conn).sync()
                : ((StatefulRedisConnection<String, String>) conn).sync();
    }

    public static RedisClusterAsyncCommands<String, String> getAsyncCommands(StatefulConnection<String, String> conn, boolean isCluster) {
        return isCluster
                ? ((StatefulRedisClusterConnection<String, String>) conn).async()
                : ((StatefulRedisConnection<String, String>) conn).async();
    }
}