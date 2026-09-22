package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.mcp.transport.session.McpSessionData;
import com.xiongx9527.fw.plus.ai.mcp.transport.session.McpSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

/**
 * Redis 实现的 {@link McpSessionStore}。
 *
 * <p>使用 {@link RedisTemplate} 存储序列化后的会话 JSON，Key 为 {@code mcp:session:{sessionId}}。
 * TTL 与配置的 {@code session-timeout-seconds} 一致。
 *
 * @author xiongxiao
 * @date 2026-09-22
 */
public class RedisMcpSessionStore implements McpSessionStore {

	private static final Logger logger = LoggerFactory.getLogger(RedisMcpSessionStore.class);
	private static final String KEY_PREFIX = "mcp:session:";

	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;
	private final long sessionTimeoutSeconds;

	public RedisMcpSessionStore(RedisTemplate<String, String> redisTemplate,
			ObjectMapper objectMapper, long sessionTimeoutSeconds) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.sessionTimeoutSeconds = sessionTimeoutSeconds;
	}

	@Override
	public void save(String sessionId, McpSessionData sessionData) {
		if (sessionId == null || sessionData == null) {
			return;
		}
		try {
			String json = objectMapper.writeValueAsString(sessionData);
			ValueOperations<String, String> ops = redisTemplate.opsForValue();
			if (sessionTimeoutSeconds > 0) {
				ops.set(buildKey(sessionId), json, sessionTimeoutSeconds, TimeUnit.SECONDS);
			} else {
				ops.set(buildKey(sessionId), json);
			}
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize session {} for Redis", sessionId, e);
		}
	}

	@Override
	public McpSessionData load(String sessionId) {
		if (sessionId == null) {
			return null;
		}
		try {
			String json = redisTemplate.opsForValue().get(buildKey(sessionId));
			if (json == null) {
				return null;
			}
			return objectMapper.readValue(json, McpSessionData.class);
		} catch (Exception e) {
			logger.error("Failed to deserialize session {} from Redis", sessionId, e);
			return null;
		}
	}

	@Override
	public void remove(String sessionId) {
		if (sessionId != null) {
			redisTemplate.delete(buildKey(sessionId));
		}
	}

	@Override
	public void touch(String sessionId) {
		// Redis TTL 在 save 时自动续期，无需额外操作
	}

	private String buildKey(String sessionId) {
		return KEY_PREFIX + sessionId;
	}
}
