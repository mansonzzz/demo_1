package com.st.redis.cache;

import java.util.concurrent.TimeUnit;


public interface CacheManager {

	<T> T get(String key);

	void put(String key, Object value);

	void put(String key, Object value, long duration, TimeUnit timeUnit);


}
