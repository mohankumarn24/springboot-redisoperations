package net.projectsync.config;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStartupCleaner {

	private final RedisTemplate<String, String> redisTemplate;

	@PostConstruct
	public void flushRedisOnStartup() {
		redisTemplate.getConnectionFactory().getConnection().flushAll();
		log.info("Flushed all redis entries on startup");
	}
}
