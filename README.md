> 跟运维沟通, 以后会有sentinel和cluster两种redis方案供研发使用, 大家可根据实际情况选择迁移方式 
### 1. 迁移方式一
> 优点: 迁移成本较低, 基本仅需修改spring-redis.xml配置文件  
> 缺点: 以后无法迁移至cluster模式
#### 1.1. 增加配置工厂类
> 增加工厂类以方便兼容有无密码的情况
```
public class JedisPoolFactory {
    public static Pool<Jedis> jedisPoolFromURI(String uri, int timeout, GenericObjectPoolConfig poolConfig) {
        URI rUri = URI.create(uri.trim());
        String authority = rUri.getAuthority();
        String addr = authority, password = null;
        int idx = authority.lastIndexOf('@');
        if (idx >= 0) {
            password = authority.substring(0, idx);
            addr = authority.substring(idx + 1);
        }
        if ("redis-sentinel".equalsIgnoreCase(rUri.getScheme())) {
            String masterName = rUri.getFragment();
            Set<String> addrSet = new HashSet<>(Arrays.asList(addr.split(",")));
            return password == null || "".equals(password.trim())
                    ? new JedisSentinelPool(masterName, addrSet, poolConfig, timeout)
                    : new JedisSentinelPool(masterName, addrSet, poolConfig, timeout, password);
        } else {
            HostAndPort hostAndPort = HostAndPort.parseString(addr);
            return password == null || "".equals(password.trim())
                    ? new JedisPool(poolConfig, hostAndPort.getHost(), hostAndPort.getPort(), timeout)
                    : new JedisPool(poolConfig, hostAndPort.getHost(), hostAndPort.getPort(), timeout, password);
        }
    }
}
```
#### 1.2. 修改spring-redis.xml文件
> 相关变量请自行设置替换
> * 单实例模式: redis://[password@]host:port
> * 哨兵模式: redis-sentinel://[password@]host:port[,host2:port2]/#sentinelMasterName
```
<bean id="redisPool" class="JedisPoolFactory"
      factory-method="jedisPoolFromURI">
    <constructor-arg index="0" value="redis-sentinel://192.168.2.18:27380,192.168.1.7:27380,192.168.2.4:27380/#aps"/>
    <constructor-arg index="1" value="1000" type="int"/>
    <constructor-arg index="2" ref="redisPoolConfig"/>
</bean>
```
#### 1.3. 可能的代码修改
> 如果代码中有直接使用redis.clients.jedis.JedisPool类的代码, 替换成redis.clients.util.Pool&lt;Jedis&gt;

### 2. 迁移方式二
> 优点: 以后可无缝迁移至cluster模式  
> 缺点: 迁移成本较高, 改动较大, 需调整原来使用redis的代码

#### 2.1. Spring项目引入方式

##### 2.1.1. pom文件中添加依赖
```
<dependency>
    <groupId>com.jd.logistics</groupId>
    <artifactId>redis-util</artifactId>
    <version>1.0.2</version>
</dependency>
```
##### 2.1.2. 初始化RedisCache
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

#### 2.2. Springboot项目引入方式

##### 2.2.1. pom文件中添加依赖
```
<dependency>
    <groupId>com.jd.logistics</groupId>
    <artifactId>redis-util-spring-boot-starter</artifactId>
    <version>1.0.2</version>
</dependency>
```

##### 2.2.2. Springboot配置文件
```
meicai.redis.pool.maxTotal=200
meicai.redis.pool.minIdle=1
meicai.redis.pool.maxWaitMillis=5000
meicai.redis.pool.maxIdle=10
meicai.redis.pool.testOnBorrow=false
meicai.redis.pool.testOnReturn=false
meicai.redis.pool.testWhileIdle=true
meicai.redis.timeout=1000
meicai.redis.url=redis://127.0.0.1:6379
```

#### 2.3. 代码示例
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