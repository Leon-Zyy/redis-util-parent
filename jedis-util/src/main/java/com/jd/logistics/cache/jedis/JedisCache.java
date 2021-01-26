package com.jd.logistics.cache.jedis;

import com.jd.logistics.cache.jedis.support.JedisCacheCommon;
import com.jd.logistics.cache.jedis.support.JedisCacheExt;

/**
 * 基于Jedis包装的redis缓存实现, 仅支持单实例和哨兵
 * <p>
 * 封装了资源池获取和释放, 可直接使用api
 *
 * @author Y.Y.Zhao
 */
public interface JedisCache extends JedisCacheCommon, JedisCacheExt {
}
