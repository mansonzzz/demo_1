package com.st.redis.cache;

import com.st.json.JsonMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;


public class RedisCacheManager implements CacheManager{

	private final static JsonMapper jsonMapper = JsonMapper.INSTANCE;

	private final RedisTemplate<String, String> redisTemplate;

	public RedisCacheManager(RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public String get(String key) {
		return redisTemplate.opsForValue().get(key);
	}


	public <T> T get(String key, Class<T> clazz) {
		String json = redisTemplate.opsForValue().get(key);
		if (StringUtils.isBlank(json)) {
			return null;
		}
		return jsonMapper.fromJson(json, clazz);
	}

	@Override
	public void put(String key, Object value) {
		String valueStr = getValue(value);
		redisTemplate.opsForValue().set(key, valueStr);
	}

	private String getValue(Object value) {
		String valueStr;
		if (value instanceof String) {
			valueStr = (String) value;
		} else {
			valueStr = jsonMapper.toJson(value);
		}
		return valueStr;
	}

	@Override
	public void put(String key, Object value, long duration, TimeUnit timeUnit) {
		String valueStr = getValue(value);
		redisTemplate.opsForValue().set(key, valueStr, duration, timeUnit);
	}

	public Boolean putIfAbsent(String key, Object value) {
		String valueStr = getValue(value);
		return redisTemplate.opsForValue().setIfAbsent(key, valueStr);
	}

	public Boolean putIfAbsent(String key, Object value, long duration, TimeUnit timeUnit) {
		String valueStr = getValue(value);
		return redisTemplate.opsForValue().setIfAbsent(key, valueStr, duration, timeUnit);
	}

	public void delete(String key) {
		redisTemplate.delete(key);
	}

}
