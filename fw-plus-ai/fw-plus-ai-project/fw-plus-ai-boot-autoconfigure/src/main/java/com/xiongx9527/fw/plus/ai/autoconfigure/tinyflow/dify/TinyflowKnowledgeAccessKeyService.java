package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeAccessKeyRepository;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

public class TinyflowKnowledgeAccessKeyService {

	private static final String VERSION = "tfk1";
	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int ITERATIONS = 120000;
	private static final int KEY_LENGTH_BITS = 256;
	private static final int PREFIX_BYTES = 9;
	private static final int SALT_BYTES = 16;
	private static final int SECRET_BYTES = 32;

	private final TinyflowKnowledgeAccessKeyRepository repository;
	private final SecureRandom secureRandom;

	public TinyflowKnowledgeAccessKeyService(TinyflowKnowledgeAccessKeyRepository repository) {
		this(repository, new SecureRandom());
	}

	TinyflowKnowledgeAccessKeyService(TinyflowKnowledgeAccessKeyRepository repository, SecureRandom secureRandom) {
		if (repository == null || secureRandom == null) {
			throw new IllegalArgumentException("Access key repository and random source must not be null");
		}
		this.repository = repository;
		this.secureRandom = secureRandom;
	}

	public CreatedAccessKey create(String tenantId, String knowledgeId) {
		byte[] prefixBytes = randomBytes(PREFIX_BYTES);
		byte[] saltBytes = randomBytes(SALT_BYTES);
		byte[] secretBytes = randomBytes(SECRET_BYTES);
		char[] secret = null;
		byte[] derived = null;
		try {
			String prefix = encode(prefixBytes);
			String salt = encode(saltBytes);
			String secretPart = encode(secretBytes);
			secret = secretPart.toCharArray();
			derived = derive(secret, saltBytes, ITERATIONS);
			String accessKeyHash = encode(derived);
			String accessKeyId = UUID.randomUUID().toString();
			repository.save(new TinyflowKnowledgeAccessKeyRepository.AccessKey(accessKeyId, tenantId, knowledgeId,
					prefix, ALGORITHM, salt, ITERATIONS, accessKeyHash, true));
			return new CreatedAccessKey(accessKeyId, VERSION + "." + prefix + "." + salt + "." + secretPart);
		} finally {
			Arrays.fill(prefixBytes, (byte) 0);
			Arrays.fill(saltBytes, (byte) 0);
			Arrays.fill(secretBytes, (byte) 0);
			if (secret != null) {
				Arrays.fill(secret, '\0');
			}
			if (derived != null) {
				Arrays.fill(derived, (byte) 0);
			}
		}
	}

	public TinyflowKnowledgeAccessKeyRepository.AccessKey authenticate(String token) {
		TokenParts parts = parse(token);
		char[] secret = parts.secret.toCharArray();
		byte[] saltBytes = parts.saltBytes;
		byte[] derived = null;
		try {
			derived = derive(secret, saltBytes, ITERATIONS);
			String hash = encode(derived);
			TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey = repository.findByHash(hash);
			if (accessKey == null || !accessKey.isEnabled()
					|| !ALGORITHM.equals(accessKey.getHashAlgorithm())
					|| accessKey.getCost() != ITERATIONS
					|| !constantTimeEquals(parts.prefix, accessKey.getPrefix())
					|| !constantTimeEquals(parts.salt, accessKey.getSalt())
					|| !constantTimeEquals(hash, accessKey.getAccessKeyHash())) {
				throw new AuthenticationException();
			}
			return accessKey;
		} finally {
			Arrays.fill(secret, '\0');
			Arrays.fill(saltBytes, (byte) 0);
			if (derived != null) {
				Arrays.fill(derived, (byte) 0);
			}
		}
	}

	private TokenParts parse(String token) {
		if (token == null || token.length() > 256) {
			throw new AuthenticationException();
		}
		String[] parts = token.split("\\.", -1);
		if (parts.length != 4 || !VERSION.equals(parts[0]) || !encoded(parts[1])
				|| !encoded(parts[2]) || !encoded(parts[3])) {
			throw new AuthenticationException();
		}
		byte[] prefixBytes = decode(parts[1]);
		byte[] saltBytes = decode(parts[2]);
		byte[] secretBytes = decode(parts[3]);
		try {
			if (prefixBytes.length != PREFIX_BYTES || saltBytes.length != SALT_BYTES
					|| secretBytes.length != SECRET_BYTES) {
				throw new AuthenticationException();
			}
			return new TokenParts(parts[1], parts[2], parts[3], saltBytes);
		} finally {
			Arrays.fill(prefixBytes, (byte) 0);
			Arrays.fill(secretBytes, (byte) 0);
		}
	}

	private byte[] randomBytes(int length) {
		byte[] value = new byte[length];
		secureRandom.nextBytes(value);
		return value;
	}

	private byte[] derive(char[] secret, byte[] salt, int iterations) {
		PBEKeySpec spec = new PBEKeySpec(secret, salt, iterations, KEY_LENGTH_BITS);
		try {
			return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Access key derivation is unavailable", exception);
		} finally {
			spec.clearPassword();
		}
	}

	private boolean constantTimeEquals(String first, String second) {
		byte[] firstBytes = first == null ? new byte[0] : first.getBytes(StandardCharsets.UTF_8);
		byte[] secondBytes = second == null ? new byte[0] : second.getBytes(StandardCharsets.UTF_8);
		try {
			return MessageDigest.isEqual(firstBytes, secondBytes);
		} finally {
			Arrays.fill(firstBytes, (byte) 0);
			Arrays.fill(secondBytes, (byte) 0);
		}
	}

	private boolean encoded(String value) {
		return value != null && value.length() > 0 && value.matches("[A-Za-z0-9_-]+");
	}

	private String encode(byte[] value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	private byte[] decode(String value) {
		try {
			return Base64.getUrlDecoder().decode(value);
		} catch (IllegalArgumentException exception) {
			throw new AuthenticationException();
		}
	}

	public static final class CreatedAccessKey {
		private final String accessKeyId;
		private final String token;

		CreatedAccessKey(String accessKeyId, String token) {
			this.accessKeyId = accessKeyId;
			this.token = token;
		}

		public String getAccessKeyId() { return accessKeyId; }
		public String getToken() { return token; }
	}

	static final class AuthenticationException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	private static final class TokenParts {
		private final String prefix;
		private final String salt;
		private final String secret;
		private final byte[] saltBytes;

		private TokenParts(String prefix, String salt, String secret, byte[] saltBytes) {
			this.prefix = prefix;
			this.salt = salt;
			this.secret = secret;
			this.saltBytes = saltBytes;
		}
	}
}
