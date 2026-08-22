package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

public interface TinyEngineAiCredentialCipher {
	String encrypt(String plainText);
	String decrypt(String token);
}
