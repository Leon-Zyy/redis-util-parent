package com.jd.logistics.cache.redis;

import com.jd.logistics.cache.redis.support.DefaultRedisCacheExt;
import com.jd.logistics.cache.redis.support.RedisCacheExt;
import com.jd.logistics.cache.redis.support.RedisConnectionPool;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import javassist.*;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Test;

import java.lang.reflect.Constructor;

/**
 * RTest
 *
 * @author Y.Y.Zhao
 */
public class JavassistTest {
    @Test
    public void t() throws Throwable {
        RedisCache redisCache = RedisCacheFactory.redisCacheStandalone("192.168.2.18:6379", null, 1000, new GenericObjectPoolConfig());
        Object list = new String[0];
        System.out.println(list.getClass().getName());
        System.out.println(list.getClass().getSimpleName());
        System.out.println(list.getClass().getCanonicalName());
        System.out.println(list.getClass().getTypeName());
        RedisCache redisCache1 = getRedisCache(redisCache.getRedisCachePool());
        redisCache1.get("kkk");
    }

    public static RedisCache getRedisCache(RedisConnectionPool rpool) {
        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass cc = pool.makeClass("com.jd.logistics.cache.redis.support.RedisCacheJavassistImpl");
            cc.addInterface(pool.get(RedisCache.class.getName()));
            CtClass redisPoolClass = pool.get(RedisConnectionPool.class.getName());
            CtClass extClass = pool.get(RedisCacheExt.class.getName());
            cc.addField(new CtField(redisPoolClass, "pool", cc));
            cc.addField(new CtField(extClass, "ext", cc));
            CtConstructor ctConstructor = new CtConstructor(new CtClass[]{redisPoolClass, extClass}, cc);
            ctConstructor.setBody("{this.pool=$1;this.ext=$2;}");
            cc.addConstructor(ctConstructor);

            CtMethod[] methods = cc.getMethods();
            for (CtMethod m : methods) {
                CtClass dc = m.getDeclaringClass();
                if (dc.isInterface()) {
                    CtMethod mc = CtNewMethod.copy(m, cc, null);
                    if (dc.getName().indexOf("RedisCacheExt") > 0) {
                        mc.setBody("return ext." + m.getName() + "($$);");
                    } else {
//                        System.out.println(mc.getName());
                        String code = "{" + StatefulConnection.class.getName() + " conn = pool.borrowConnection();\n" +
                                "try {\n" +
                                "    " + RedisClusterCommands.class.getName() + " cmd = " + RedisConnectionPool.class.getName() + ".getSyncCommands(conn, pool.isCluster());\n" +
                                "    return cmd." + m.getName() + "($$);\n" +
                                "} finally {\n" +
                                "    if (conn != null) {\n" +
                                "        pool.returnConnection(conn);\n" +
                                "    }\n" +
                                "}}";
//                        System.out.println(code);
                        mc.setBody(code);
                    }
                    cc.addMethod(mc);
                }
            }

//            byte[] bs = cc.toBytecode();
//            try (FileOutputStream stream = new FileOutputStream("d:\\aaa.class")) {
//                stream.write(bs);
//            }

            Class<?> aClass = cc.toClass();
            Constructor<?> constructor = aClass.getConstructor(RedisConnectionPool.class, RedisCacheExt.class);
            return (RedisCache) constructor.newInstance(rpool, new DefaultRedisCacheExt(rpool));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
