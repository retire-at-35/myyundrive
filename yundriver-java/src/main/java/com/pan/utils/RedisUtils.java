package com.pan.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.Collections;
import java.util.Set;

@Component
public class RedisUtils<V> {

    private static final Logger logger = LoggerFactory.getLogger(RedisUtils.class);

    @Resource
    private RedisTemplate<String, V> redisTemplate;

    /**
     * 设置一个 Redis 键值对
     * 
     * @param key Redis 键
     * @param value Redis 值
     */
    public void set(String key, V value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            logger.info("成功设置 Redis 键：{}，值：{}", key, value);
        } catch (Exception e) {
            logger.error("设置 Redis 键：{}，值：{} 失败", key, value, e);
        }
    }

    /**
     * 获取 Redis 中指定键的值
     * 
     * @param key Redis 键
     * @return Redis 值
     */
    public V get(String key) {
        try {
            V value = redisTemplate.opsForValue().get(key);
            logger.info("成功获取 Redis 键：{}，值：{}", key, value);
            return value;
        } catch (Exception e) {
            logger.error("获取 Redis 键：{} 失败", key, e);
            return null;
        }
    }

    /**
     * 设置一个 Redis 键值对，并设置过期时间（单位：秒）
     * 
     * @param key Redis 键
     * @param value Redis 值
     * @param timeout 过期时间，单位秒
     */
    public void setex(String key, V value, long timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
            logger.info("成功设置 Redis 键：{}，值：{}，过期时间：{} 秒", key, value, timeout);
        } catch (Exception e) {
            logger.error("设置 Redis 键：{}，值：{}，过期时间：{} 秒失败", key, value, timeout, e);
        }
    }

    /**
     * 设置一个 Redis 键值对，并设置过期时间
     * 
     * @param key Redis 键
     * @param value Redis 值
     * @param timeout 过期时间
     * @param timeUnit 过期时间单位
     */
    public void setex(String key, V value, long timeout, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
            logger.info("成功设置 Redis 键：{}，值：{}，过期时间：{} {}", key, value, timeout, timeUnit);
        } catch (Exception e) {
            logger.error("设置 Redis 键：{}，值：{}，过期时间：{} {} 失败", key, value, timeout, timeUnit, e);
        }
    }

    /**
     * 判断 Redis 中是否存在指定的键
     * 
     * @param key Redis 键
     * @return 是否存在
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            logger.info("检查 Redis 键是否存在：{}，结果：{}", key, exists);
            return exists != null && exists;
        } catch (Exception e) {
            logger.error("检查 Redis 键是否存在失败：{}", key, e);
            return false;
        }
    }

    /**
     * 删除 Redis 中指定的键
     * 
     * @param key Redis 键
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            logger.info("成功删除 Redis 键：{}", key);
        } catch (Exception e) {
            logger.error("删除 Redis 键失败：{}", key, e);
        }
    }

    /**
     * 获取 Redis 中所有匹配指定模式的键
     * 
     * @param pattern 键的匹配模式
     * @return 匹配的键列表
     */
    public Set<String> keys(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            logger.info("成功获取匹配模式：{} 的 Redis 键：{}", pattern, keys);
            return keys;
        } catch (Exception e) {
            logger.error("获取匹配模式：{} 的 Redis 键失败", pattern, e);
            return Collections.emptySet();
        }
    }
}
