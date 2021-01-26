package com.jd.logistics.cache.jedis.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisNoScriptException;
import redis.clients.util.Pool;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 基于RedisCache实现了分布式锁, 及一些redis扩展
 * <p>
 * 推荐使用{@link #newLock}及其重载方法调用分布式锁
 *
 * @author Y.Y.Zhao
 */
public final class DefaultJedisCacheExt implements JedisCacheExt {
    private static final Logger log = LoggerFactory.getLogger(DefaultJedisCacheExt.class);
    private static final String SUCCESS = "OK";
    private static final long DEFAULT_WAIT_MILLISECONDS = 0;
    private static final int DEFAULT_EXPIRE_SECONDS = 30;
    private static final long RETRY_INTERVAL_MILLISECONDS = 50;
    private final Pool<Jedis> jedisPool;


    public DefaultJedisCacheExt(Pool<Jedis> jedisPool) {
        this.jedisPool = jedisPool;
        preloadLua();
    }

    @Override
    public Map<String, String> mgetMap(String... keys) {
        return mgetMap(true, keys);
    }

    @Override
    public Map<String, String> mgetMap(boolean removeNullValue, String... keys) {
        if (keys == null || keys.length == 0) {
            return Collections.emptyMap();
        }
        List<String> values;
        try (Jedis jedis = jedisPool.getResource()) {
            values = jedis.mget(keys);
        }
        Map<String, String> map = new HashMap<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            if (!removeNullValue) {
                map.put(keys[i], values.get(i));
            } else if (values.get(i) != null) {
                map.put(keys[i], values.get(i));
            }
        }
        return map;
    }

    /** 内部RedisCachePool实例 */
    @Override
    public Pool<Jedis> getJedisPool() {
        return jedisPool;
    }

    /**
     * 同时设置多值及过期时间
     *
     * @param map           key value
     * @param expireSeconds 秒
     */
    @Override
    public void msetEx(Map<String, String> map, int expireSeconds) {
        if (expireSeconds < 1) {
            throw new IllegalArgumentException("parameter error");
        }
        if (map == null || map.size() == 0) {
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            try (Pipeline pipelined = jedis.pipelined()) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    pipelined.setex(entry.getKey(), expireSeconds, entry.getValue());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public List<Object> pipelined(Consumer<Pipeline> doInPipelined) {
        try (Jedis jedis = jedisPool.getResource()) {
            try (Pipeline pipelined = jedis.pipelined()) {
                doInPipelined.accept(pipelined);
                return pipelined.syncAndReturnAll();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 尝试获取分布式锁, 必须手动调用{@link DefaultLockState#close()}
     *
     * @param keys 尝试获取锁keys
     * @return 锁结果, 使用isSuccess判断是否成功获取锁, 调用close释放锁
     */
    @Override
    public DefaultLockState tryLock(String... keys) {
        return tryLock(DEFAULT_WAIT_MILLISECONDS, DEFAULT_EXPIRE_SECONDS, keys);
    }

    /**
     * 尝试获取分布式锁, 必须手动调用{@link DefaultLockState#close()}
     *
     * @param waitMilliseconds 毫秒,尝试等待获取锁时间,小于等于0时不等待,如果设置建议大于50
     * @param keys             尝试获取锁keys
     * @return 锁结果, 使用isSuccess判断是否成功获取锁, 调用close释放锁
     */
    @Override
    public DefaultLockState tryLock(long waitMilliseconds, String... keys) {
        return tryLock(waitMilliseconds, DEFAULT_EXPIRE_SECONDS, keys);
    }

    /**
     * 尝试获取分布式锁, 必须手动调用{@link DefaultLockState#close()}
     *
     * @param waitMilliseconds 毫秒,尝试等待获取锁时间,小于等于0时不等待,如果设置建议大于50
     * @param expireSeconds    秒,锁过期时间
     * @param keys             尝试获取锁keys
     * @return 锁结果, 使用isSuccess判断是否成功获取锁, 调用close释放锁
     */
    @Override
    public DefaultLockState tryLock(long waitMilliseconds, int expireSeconds, String... keys) {
        if (keys == null || keys.length == 0 || waitMilliseconds < 0 || expireSeconds < 1) {
            throw new IllegalArgumentException("parameter error");
        }
        String value = UUID.randomUUID().toString();
        long sleepMilli = waitMilliseconds;
        String rst;
        while (true) {
            try (Jedis jedis = jedisPool.getResource()) {
                if (keys.length > 1) {
                    rst = evalLua(jedis, LUA_LOCK_MULTI, keys, String.valueOf(expireSeconds), value);
                } else {
                    rst = jedis.set(keys[0], value, "nx", "ex", expireSeconds);
                }
            }

            if (SUCCESS.equals(rst)) {
                return new DefaultLockState(true, value, expireSeconds, keys);
            }
            sleepMilli -= RETRY_INTERVAL_MILLISECONDS;
            if (sleepMilli < 0) {
                return new DefaultLockState(keys);
            }
            try {
                Thread.sleep(RETRY_INTERVAL_MILLISECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * 创建锁对象
     *
     * @param keys 尝试获取锁keys
     * @return 锁对象, 使用lock方法开始获取锁
     */
    @Override
    public DefaultLock newLock(String... keys) {
        return newLock(DEFAULT_WAIT_MILLISECONDS, DEFAULT_EXPIRE_SECONDS, keys);
    }

    /**
     * 创建锁对象
     *
     * @param waitMilliseconds 毫秒,尝试等待获取锁时间,小于等于0时不等待,如果设置建议大于50
     * @param keys             尝试获取锁keys
     * @return 锁对象, 使用lock方法开始获取锁
     */
    @Override
    public DefaultLock newLock(long waitMilliseconds, String... keys) {
        return newLock(waitMilliseconds, DEFAULT_EXPIRE_SECONDS, keys);
    }

    /**
     * 创建锁对象
     *
     * @param waitMilliseconds 毫秒,尝试等待获取锁时间,小于等于0时不等待,如果设置建议大于50
     * @param expireSeconds    秒,锁过期时间
     * @param keys             尝试获取锁keys
     * @return 锁对象, 使用lock方法开始获取锁
     */
    @Override
    public DefaultLock newLock(long waitMilliseconds, int expireSeconds, String... keys) {
        return new DefaultLock(waitMilliseconds, expireSeconds, keys);
    }

    @Override
    public <T> T evalLua(Lua lua, String[] keys, String... values) {
        try (Jedis jedis = jedisPool.getResource()) {
            return evalLua(jedis, lua, keys, values);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T evalLua(Jedis jedis, Lua lua, String[] keys, String... values) {
        try {
            return (T) jedis.evalsha(lua.getSha(), Arrays.asList(keys), Arrays.asList(values));
        } catch (JedisNoScriptException ex) {
            log.warn("redis eval sha:{} not exists, reload from:{}", lua.getSha(), lua.getScript());
            return (T) jedis.eval(lua.getScript(), Arrays.asList(keys), Arrays.asList(values));
        }
    }

    /** 锁对象, 使用{@link #lock}方法开始获取锁 */
    public final class DefaultLock implements Lock {
        private long waitMilliSeconds;
        private int expireSeconds;
        private String[] keys;

        private DefaultLock(long waitMilliSeconds, int expireSeconds, String... keys) {
            if (keys == null || keys.length == 0 || waitMilliSeconds < 0 || expireSeconds < 1) {
                throw new IllegalArgumentException("parameter error");
            }
            this.waitMilliSeconds = waitMilliSeconds;
            this.expireSeconds = expireSeconds;
            this.keys = keys;
        }

        /**
         * 获取锁
         *
         * @param successConsumer 获取锁成功后执行
         * @return 锁结果, 不需要手动释放锁, 用于通过getData()获取数据
         */
        @Override
        public DefaultLockState lock(Consumer<LockState> successConsumer) {
            return lock(successConsumer, null);
        }

        /**
         * 获取锁
         *
         * @param successConsumer 获取锁成功后执行
         * @param failedConsumer  获取锁失败后执行
         * @return 锁结果, 不需要手动释放锁, 用于通过getData()获取数据
         */
        @Override
        public DefaultLockState lock(Consumer<LockState> successConsumer, Consumer<LockState> failedConsumer) {
            if (successConsumer == null) {
                throw new IllegalArgumentException("must have successConsumer");
            }
            try (DefaultLockState defaultLockResult = tryLock(this.waitMilliSeconds, this.expireSeconds, keys)) {
                if (defaultLockResult.success) {
                    successConsumer.accept(defaultLockResult);
                } else if (failedConsumer != null) {
                    failedConsumer.accept(defaultLockResult);
                }
                return defaultLockResult;
            }
        }
    }

    /**
     * 锁结果, 使用{@link #isSuccess}判断是否成功获取锁, 调用{@link #close}释放锁
     */
    public final class DefaultLockState implements LockState, Closeable {
        private boolean success;
        private String[] keys;
        private String value;
        private int expireSeconds;
        private AtomicBoolean unlocked = new AtomicBoolean(false);
        private volatile ScheduledFuture task;
        /** 存放用户数据 */
        private Object data;

        private DefaultLockState(String... keys) {
            this.success = false;
            this.keys = keys;
        }

        private DefaultLockState(boolean success, String value, int expireSeconds, String... keys) {
            this.success = success;
            this.keys = keys;
            this.value = value;
            this.expireSeconds = expireSeconds;
            renewExpireTask();
        }

        private void renewExpireTask() {
            if (this.success && !unlocked.get()) {
                synchronized (this) {
                    task = executor.schedule(new CheckRunnable(), Math.max(expireSeconds / 2, expireSeconds - 10), TimeUnit.SECONDS);
                }
            }
        }

        /** 是否成功获得锁 */
        @Override
        public boolean isSuccess() {
            return success;
        }

        /** 锁定的keys */
        @Override
        public String[] getKeys() {
            return keys;
        }

        /**
         * 在 {@link DefaultLock#lock(Consumer, Consumer)}后可获取数据
         *
         * @return consumer中设置的数据
         */
        @Override
        @SuppressWarnings("unchecked")
        public <T> T getData() {
            return (T) data;
        }

        /**
         * 在consumer中可设置数据
         *
         * @param data 需要设置的数据
         */
        @Override
        public <T> void setData(T data) {
            this.data = data;
        }

        /** 释放锁 */
        @Override
        public void close() {
            if (success && !unlocked.getAndSet(true)) {
                try {
                    evalLua(LUA_DEL, keys, value);
                } finally {
                    synchronized (this) {
                        if (task != null) {
                            task.cancel(false);
                            task = null;
                        }
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "LockResult{" +
                    "success=" + success +
                    ", keys=" + Arrays.toString(keys) +
                    ", value='" + value + '\'' +
                    '}';
        }

        private class CheckRunnable implements Runnable {
            @Override
            public void run() {
                synchronized (this) {
                    task = null;
                }
                if (success && !unlocked.get()) {
                    log.info("delay lock:{}", (Object) keys);
                    String rst = evalLua(LUA_EXPIRE, keys, value, String.valueOf(expireSeconds));
                    if (SUCCESS.equals(rst) && !unlocked.get()) {
                        renewExpireTask();
                    }
                }
            }
        }
    }

    private void preloadLua() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.scriptLoad(LUA_DEL.getScript());
            jedis.scriptLoad(LUA_EXPIRE.getScript());
            jedis.scriptLoad(LUA_LOCK_MULTI.getScript());
        }
    }

    private static final Lua LUA_LOCK_MULTI = new Lua("for i, v in pairs(redis.call('mget', unpack(KEYS))) do\n" +
            "    if v then\n" +
            "        return nil\n" +
            "    end\n" +
            "end\n" +
            "for i, k in pairs(KEYS) do\n" +
            "    redis.call('setex', k, ARGV[1], ARGV[2])\n" +
            "end\n" +
            "return 'OK'");
    private static final Lua LUA_DEL = new Lua("if redis.call('get', KEYS[1]) == ARGV[1] then\n" +
            "    redis.call('del', unpack(KEYS))\n" +
            "    return 'OK'\n" +
            "end\n" +
            "return nil");
    private static final Lua LUA_EXPIRE = new Lua("if redis.call('get', KEYS[1]) == ARGV[1] then\n" +
            "    for i, k in pairs(KEYS) do\n" +
            "        redis.call('expire', k, ARGV[2])\n" +
            "    end\n" +
            "    return 'OK'\n" +
            "end\n" +
            "return nil");
    private static final ScheduledThreadPoolExecutor executor;

    static {
        AtomicInteger ti = new AtomicInteger(1);
        executor = new ScheduledThreadPoolExecutor(2, r -> {
            Thread thread = new Thread(r);
            thread.setName("delay-lock-" + ti.getAndIncrement());
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
    }
}