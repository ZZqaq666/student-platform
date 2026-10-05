package com.student.platform.qa.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 *
 * 功能说明：
 * - 封装Redis常用操作方法
 * - 支持字符串、哈希、列表、集合、有序集合等数据结构
 * - 提供数据备份和恢复功能
 *
 * 使用场景：
 * - 用户会话管理
 * - AI回答缓存
 * - 学习进度缓存
 * - 热点数据缓存
 * - 分布式锁
 *
 * @author student-platform
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    /**
     * Redis操作模板
     * 
     * 已配置JSON序列化，支持对象存储
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置字符串值
     *
     * @param key 键
     * @param value 值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置字符串值（带过期时间）
     *
     * @param key 键
     * @param value 值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取字符串值
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除指定键
     *
     * @param key 键
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除键
     *
     * @param keys 键集合
     * @return 删除的键数量
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 按模式查找键
     *
     * @param pattern 模式（支持通配符 * 和 ?）
     * @return 匹配的键集合
     */
    public Set<String> keys(String pattern) {
        Set<String> result = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                result.add(cursor.next());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to scan Redis keys by pattern: " + pattern, e);
        }

        return result;
    }

    /**
     * 数值递减（减1）
     *
     * @param key 键
     * @return 递减后的值
     */
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 数值递减（减指定值）
     *
     * @param key 键
     * @param delta 递减量
     * @return 递减后的值
     */
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    /**
     * 设置哈希字段值
     *
     * @param key 键
     * @param hashKey 哈希字段
     * @param value 值
     */
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 批量设置哈希字段值
     *
     * @param key 键
     * @param map 键值对集合
     */
    public void hSetAll(String key, Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 获取哈希字段值
     *
     * @param key 键
     * @param hashKey 哈希字段
     * @return 值
     */
    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 获取哈希所有字段和值
     *
     * @param key 键
     * @return 所有键值对
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 检查哈希字段是否存在
     *
     * @param key 键
     * @param hashKey 哈希字段
     * @return 是否存在
     */
    public Boolean hHasKey(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(key, hashKey);
    }

    /**
     * 删除哈希字段
     *
     * @param key 键
     * @param hashKeys 哈希字段（可变参数）
     */
    public void hDelete(String key, Object... hashKeys) {
        redisTemplate.opsForHash().delete(key, hashKeys);
    }

    /**
     * 列表左侧插入
     *
     * @param key 键
     * @param value 值
     */
    public void lPush(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 列表右侧插入
     *
     * @param key 键
     * @param value 值
     */
    public void rPush(String key, Object value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 列表左侧弹出
     *
     * @param key 键
     * @return 弹出的值
     */
    public Object lPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 列表右侧弹出
     *
     * @param key 键
     * @return 弹出的值
     */
    public Object rPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 获取列表范围元素
     *
     * @param key 键
     * @param start 起始索引
     * @param end 结束索引
     * @return 元素列表
     */
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取列表长度
     *
     * @param key 键
     * @return 长度
     */
    public Long lSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    /**
     * 集合添加元素
     *
     * @param key 键
     * @param values 值（可变参数）
     */
    public void sAdd(String key, Object... values) {
        redisTemplate.opsForSet().add(key, values);
    }

    /**
     * 获取集合所有元素
     *
     * @param key 键
     * @return 元素集合
     */
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 检查元素是否在集合中
     *
     * @param key 键
     * @param value 值
     * @return 是否存在
     */
    public Boolean sIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 获取集合大小
     *
     * @param key 键
     * @return 大小
     */
    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * 集合移除元素
     *
     * @param key 键
     * @param values 值（可变参数）
     * @return 移除的数量
     */
    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

    /**
     * 有序集合添加元素
     *
     * @param key 键
     * @param value 值
     * @param score 分数
     * @return 是否添加成功
     */
    public Boolean zAdd(String key, Object value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 获取有序集合范围元素（按分数升序）
     *
     * @param key 键
     * @param start 起始索引
     * @param end 结束索引
     * @return 元素集合
     */
    public Set<Object> zRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 获取有序集合范围元素（按分数降序）
     *
     * @param key 键
     * @param start 起始索引
     * @param end 结束索引
     * @return 元素集合
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 获取有序集合大小
     *
     * @param key 键
     * @return 大小
     */
    public Long zSize(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    /**
     * 获取有序集合元素分数
     *
     * @param key 键
     * @param value 值
     * @return 分数
     */
    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    /**
     * 有序集合移除元素
     *
     * @param key 键
     * @param values 值（可变参数）
     * @return 移除的数量
     */
    public Long zRemove(String key, Object... values) {
        return redisTemplate.opsForZSet().remove(key, values);
    }
    
    /**
     * 备份指定前缀的Redis数据到文件
     * @param prefix 键前缀
     * @param filePath 备份文件路径
     * @return 备份的键数量
     */
    public int backupDataByPrefix(String prefix, String filePath) {
        try {
            Set<String> keys = keys(prefix + "*");
            if (keys.isEmpty()) {
                return 0;
            }
            
            Map<String, Object> data = new HashMap<>();
            for (String key : keys) {
                data.put(key, redisTemplate.opsForValue().get(key));
            }
            
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(new File(filePath), data);
            return keys.size();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * 从文件恢复Redis数据
     * @param filePath 备份文件路径
     * @return 恢复的键数量
     */
    public int restoreDataFromFile(String filePath) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> data = objectMapper.readValue(new File(filePath), new TypeReference<Map<String, Object>>() {});
            
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                redisTemplate.opsForValue().set(entry.getKey(), entry.getValue());
            }
            return data.size();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * 备份所有AI相关数据
     * @param filePath 备份文件路径
     * @return 备份的键数量
     */
    public int backupAllAiData(String filePath) {
        return backupDataByPrefix("ai:", filePath);
    }
}
