#### 1. Spring项目引入方式

##### 1.1 pom文件中添加依赖
```
<dependency>
    <groupId>com.jd.logistics</groupId>
    <artifactId>redis-util</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
##### 1.2 初始化RedisCache
> 通过URI配置
> * 单实例模式: redis://[password@]host:port
> * 哨兵模式: redis-sentinel://[password@]host:port[,host2:port2]/#sentinelMasterName
> * 集群模式: redis-cluster://[password@]host:port[,host2:port2]
   
* application.properties文件
```
# redis连接池配置
redis.maxTotal=300
redis.minIdle=1
redis.maxWaitMillis=30000
redis.maxIdle=10
redis.testOnBorrow=true
redis.testOnReturn=true
redis.testWhileIdle=true
redis.timeout=1000
# 单实例模式配置
# redis.cache.uri=redis://192.168.2.18:6379
# 哨兵模式配置
redis.cache.uri=redis-sentinel://192.168.2.18:27380,192.168.1.7:27380,192.168.2.4:27380/#aps
```
* spring-redis.xml文件
```
<beans>
    <bean id="redisPoolConfig" class="org.apache.commons.pool2.impl.GenericObjectPoolConfig">
        <property name="maxTotal" value="${redis.maxTotal}"/>
        <property name="minIdle" value="${redis.minIdle}"/>
        <property name="maxWaitMillis" value="${redis.maxWaitMillis}"/>
        <property name="maxIdle" value="${redis.maxIdle}"/>
        <property name="testOnBorrow" value="${redis.testOnBorrow}"/>
        <property name="testOnReturn" value="${redis.testOnReturn}"/>
        <property name="testWhileIdle" value="${redis.testWhileIdle}"/>
    </bean>
    <bean id="redisCache" class="RedisCacheFactory"
            factory-method="redisCacheFromURI">
        <constructor-arg index="0" value="${redis.cache.uri}"/>
        <constructor-arg index="1" value="${redis.timeout}" type="long"/>
        <constructor-arg index="2" ref="redisPoolConfig"/>
    </bean>
</beans>
```
#### 2. 代码示例
```
public class RedisTest{
    // 注入工具类
    @Resource
    private RedisCache redisCache;

    @Test
    public void testSetGet() {
        // 参考redis各种命令
        redisCache.set("test:key:1", "test value");
        String s = redisCache.get("test:key:1");
        Assert.assertEquals(s, "test value");
        redisCache.del("test:key:1");
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
}
```