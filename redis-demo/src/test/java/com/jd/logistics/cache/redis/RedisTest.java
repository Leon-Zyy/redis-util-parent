package com.jd.logistics.cache.redis;

import com.jd.logistics.cache.redis.support.LockState;
import io.lettuce.core.KeyValue;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RedisTests
 *
 * @author Y.Y.Zhao
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring/spring-main.xml")
public class RedisTest {
    @Resource
    private RedisCache redisCache;

    @Resource
    private Pool<Jedis> redisPool;

    private final Map<String, String> map;

    {
        map = new HashMap<>(10);
        for (int i = 0; i < 300; i++) {
            map.put("benchmark:map:" + i, "benchmark:map:" + i);
        }
    }

    @Test
    public void testSetGet() {
        redisCache.set("test:key:1", "test value");
        String s = redisCache.get("test:key:1");
        Assert.assertEquals(s, "test value");
        redisCache.del("test:key:1");
    }

    @Test
    public void testSetGetPool() {
        try (Jedis jedis = redisPool.getResource()) {
            jedis.set("test:key:1", "test value");
            String s = jedis.get("test:key:1");
            Assert.assertEquals(s, "test value");
            jedis.del("test:key:1");
        }
    }

    @Test
    public void testExt() {
        // 扩展mset,支持同时设置过期时间
        redisCache.msetEx(map, 10);

        // 扩展mget,返回map中仅包含有数据的key
        Map<String, String> rst = redisCache.mgetMap("test:key:1", "test:key:2");

        // 分布式锁封装, 推荐使用第一种
        redisCache.newLock("test:lock:key:1").lock(
                state -> System.out.println("lock success"),
                state -> System.out.println("lock failed")
        );
        try (LockState state = redisCache.tryLock("test:lock:key:1")) {
            if (state.isSuccess()) {
                System.out.println("lock success");
            }
        }
    }

    @Test
    public void testMsetEx() {
        redisCache.msetEx(map, 10);
        List<KeyValue<String, String>> mget = redisCache.mget(map.keySet().toArray(new String[0]));
        assert mget.size() == map.size();
        for (KeyValue<String, String> kv : mget) {
            assert kv.getKey().equals(kv.getValue());
        }

        Map<String, String> map = redisCache.mgetMap("benchmark:map:0", "benchmark:map:1", "benchmark:map:-1", "benchmark:map:0");
        System.out.println(map);
        map = redisCache.mgetMap(false, "benchmark:map:0", "benchmark:map:1", "benchmark:map:-1", "benchmark:map:0");
        System.out.println(map);
    }

    @Test
    public void testLock() {
        String lockA = "redis:test:lock:a";
        String lockB = "redis:test:lock:b";

        // 第一种使用方式
        try (LockState lock = redisCache.tryLock(lockA)) {
            if (lock.isSuccess()) {
                System.out.println("1:获取锁成功");
                // TODO: 业务代码
            }
        }

        // 第二种使用方式
        LockState lockResult = redisCache.newLock(lockA).lock(
                result -> {
                    System.out.println("2:获取锁成功");
                    // TODO: 业务代码
                    result.setData("=有数据");
                },
                result -> {
                    System.out.println("2:获取锁失败");
                    // TODO: 业务代码
                    result.setData("=没有数据");
                }
        );

        Assert.assertEquals("=有数据", lockResult.getData());

        redisCache.setex(lockA, 1, "abc");
        try (LockState lock = redisCache.tryLock(lockA)) {
            Assert.assertFalse(lock.isSuccess());
        }
        redisCache.newLock(lockA).lock(
                result -> System.out.println("3:获取锁成功"),
                result -> System.out.println("3:获取锁失败")
        );
        try (LockState lock = redisCache.tryLock(lockA, lockB)) {
            Assert.assertFalse(lock.isSuccess());
        }
        try (LockState lock = redisCache.tryLock(1000, lockA)) {
            Assert.assertTrue(lock.isSuccess());
        }
        try (LockState lock = redisCache.tryLock(lockA, lockB)) {
            Assert.assertTrue(lock.isSuccess());
        }
    }
}
