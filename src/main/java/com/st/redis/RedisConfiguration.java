package com.st.redis;

import com.st.redis.cache.RedisCacheManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;


@Configuration
public class RedisConfiguration {

	@Value("${spring.redis.host}")
	private String host;

	@Value("${spring.redis.port}")
	private Integer port;

	@Value("${spring.redis.database}")
	private Integer database;

	@Value("${spring.redis.password}")
	private String password;

	@Value("${spring.redis.pool.max-active}")
	private Integer maxActive;

	@Value("${spring.redis.pool.max-wait}")
	private Integer maxWait;

	@Value("${spring.redis.pool.max-idle}")
	private Integer maxIdle;

	@Value("${spring.redis.pool.min-idle}")
	private Integer minIdle;

	@Value("${spring.redis.timeout:#{null}}")
	private Integer timeout;

	@Bean
	public LettuceConnectionFactory redisConnectionFactory() {
		RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(host, port);
		serverConfig.setDatabase(database);
		if (StringUtils.isNotBlank(password)) {
			serverConfig.setPassword(password);
		}

		GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
		poolConfig.setMaxIdle(maxIdle);
		poolConfig.setMinIdle(minIdle);
		poolConfig.setMaxTotal(maxActive);
		poolConfig.setMaxWaitMillis(maxWait);

		LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder clientConfigBuilder = LettucePoolingClientConfiguration.builder();
		if (timeout != null) {
			clientConfigBuilder.commandTimeout(Duration.ofMillis(timeout));
		}
		clientConfigBuilder.poolConfig(poolConfig);

		LettuceConnectionFactory redisConnectionFactory = new LettuceConnectionFactory(serverConfig, clientConfigBuilder.build());
		return redisConnectionFactory;
	}

	@Bean
	public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, String> redisTemplate = new StringRedisTemplate();
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		return redisTemplate;
	}

	@Bean
	public RedisCacheManager redisCacheManager(RedisTemplate<String, String> redisTemplate) {
		return new RedisCacheManager(redisTemplate);
	}

}
