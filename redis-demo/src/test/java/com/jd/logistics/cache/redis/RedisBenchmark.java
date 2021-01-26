package com.jd.logistics.cache.redis;

import com.jd.logistics.cache.jedis.JedisCache;
import com.jd.logistics.cache.jedis.JedisCacheFactory;
import com.jd.logistics.cache.jedis.support.Lua;
import com.jd.logistics.cache.redis.support.LockState;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark
 *
 * @author Y.Y.Zhao
 */
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode({Mode.AverageTime})
@Warmup(iterations = 1, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
@Threads(3)
public class RedisBenchmark {
    private static final String uri_redis = "redis://192.168.2.18:6379";
    private static final RedisCache redisCache = RedisCacheFactory.redisCacheFromURI(uri_redis, 1000, new GenericObjectPoolConfig());
    private static final JedisCache jedisCache = JedisCacheFactory.jedisCacheFromURI(uri_redis, 1000, new GenericObjectPoolConfig());
    private static final Lua LUA_MSET_EX = new Lua("for i,k in pairs(KEYS) do redis.call('setex',k,ARGV[1],ARGV[i+1]) end return 'OK'");

    private static final String KEY = "benchmark:a";
    private static final String LOCK_KEY = "benchmark:lock:a";
    private static final String[] LOCK_MULTI_KEYS = new String[]{"benchmark:lock:b", "benchmark:lock:c", "benchmark:lock:d", "benchmark:lock:e", "benchmark:lock:f"};
    private static final Map<String, String> map;

    static {
        map = new HashMap<>();
        for (int i = 0; i < 500; i++) {
            map.put("benchmark:map:" + i, "benchmark:value:" + i);
        }
    }

    @Benchmark
    public void msetExLettuce() throws Exception {
        redisCache.msetEx(map, 10);
    }

    @Benchmark
    public String getByLettuce() {
        return redisCache.get(KEY);
    }

    @Benchmark
    public boolean lockOneLettuce() {
        try (LockState result = redisCache.tryLock(LOCK_KEY)) {
            return result.isSuccess();
        }
    }

    @Benchmark
    public boolean lockMultiLettuce() {
        try (LockState result = redisCache.tryLock(LOCK_MULTI_KEYS)) {
            return result.isSuccess();
        }
    }

    @Benchmark
    public String getByJedis() {
        return jedisCache.get(KEY);
    }

    @Benchmark
    public void msetExByJedis() {
        jedisCache.msetEx(map, 10);
    }

    @Benchmark
    public void msetExByLua() {
        List<String> keys = new ArrayList<>(map.size());
        List<String> values = new ArrayList<>(map.size() + 1);
        values.add("10");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            keys.add(entry.getKey());
            values.add(entry.getValue());
        }
        jedisCache.evalsha(LUA_MSET_EX.getSha(), keys, values);
    }

    @Benchmark
    public boolean lockOneJedis() {
        try (com.jd.logistics.cache.jedis.support.LockState result = jedisCache.tryLock(LOCK_KEY)) {
            return result.isSuccess();
        }
    }

    @Benchmark
    public boolean lockMultiJedis() {
        try (com.jd.logistics.cache.jedis.support.LockState result = jedisCache.tryLock(LOCK_MULTI_KEYS)) {
            return result.isSuccess();
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(RedisBenchmark.class.getSimpleName())
                .forks(1)
                .build();
        new Runner(opt).run();
    }
}

