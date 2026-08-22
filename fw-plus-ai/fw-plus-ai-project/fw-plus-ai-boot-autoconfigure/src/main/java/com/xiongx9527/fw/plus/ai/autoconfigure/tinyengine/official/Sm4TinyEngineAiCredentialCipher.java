package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiProperties;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Base64;

public class Sm4TinyEngineAiCredentialCipher implements TinyEngineAiCredentialCipher {

	public static final String TOKEN_PREFIX = "EKEY_";
	private static final String PROVIDER = "BC";
	private static final String ALGORITHM = "SM4";
	private static final String TRANSFORMATION = "SM4/ECB/PKCS5Padding";
	private final String base64Key;

	static {
		if (Security.getProvider(PROVIDER) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	public Sm4TinyEngineAiCredentialCipher(String base64Key) {
		this.base64Key = base64Key;
	}

	public static TinyEngineAiCredentialCipher from(TinyEngineAiProperties.Official official) {
		String configuredKey = official == null ? null : official.getSm4Key();
		return new Sm4TinyEngineAiCredentialCipher(isBlank(configuredKey) ? System.getenv("SM4KEY") : configuredKey);
	}

	@Override
	public String encrypt(String plainText) {
		if (plainText == null) {
			throw new IllegalArgumentException("TinyEngine AI credential plainText must not be null");
		}
		return TOKEN_PREFIX + Base64.getEncoder().encodeToString(crypt(Cipher.ENCRYPT_MODE,
				plainText.getBytes(StandardCharsets.UTF_8)));
	}

	@Override
	public String decrypt(String token) {
		if (token == null || !token.startsWith(TOKEN_PREFIX)) {
			throw new IllegalArgumentException("TinyEngine AI credential token must start with EKEY_");
		}
		String encryptedBase64 = token.substring(TOKEN_PREFIX.length());
		if (encryptedBase64.length() == 0) {
			throw new IllegalArgumentException("TinyEngine AI credential token must contain encrypted payload after EKEY_");
		}
		try {
			byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);
			return new String(crypt(Cipher.DECRYPT_MODE, encrypted), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("TinyEngine AI credential token payload must be valid Base64", ex);
		}
	}

	private byte[] crypt(int mode, byte[] input) {
		try {
			SecretKeySpec keySpec = new SecretKeySpec(decodeKey(), ALGORITHM);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION, PROVIDER);
			cipher.init(mode, keySpec);
			return cipher.doFinal(input);
		} catch (GeneralSecurityException ex) {
			throw new IllegalArgumentException("TinyEngine AI SM4 credential operation failed", ex);
		}
	}

	private byte[] decodeKey() {
		if (isBlank(base64Key)) {
			throw new IllegalStateException("TinyEngine AI SM4 key is required; configure fw.plus.tiny-engine.ai.official.sm4-key or SM4KEY");
		}
		byte[] key;
		try {
			key = Base64.getDecoder().decode(base64Key);
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("TinyEngine AI SM4 key must be valid Base64", ex);
		}
		if (key.length != 16) {
			throw new IllegalArgumentException("TinyEngine AI SM4 key must decode to exactly 16 bytes");
		}
		return key;
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
