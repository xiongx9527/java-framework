package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

final class TinyflowKnowledgeResourceNameCodec {

	private static final String VERSION = "tfk1";

	private TinyflowKnowledgeResourceNameCodec() {
	}

	static String encode(List<String> identity) {
		if (identity == null || identity.isEmpty()) {
			throw new IllegalArgumentException("resource identity must not be empty");
		}
		StringBuilder result = new StringBuilder(VERSION);
		Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
		for (String part : identity) {
			if (part == null) throw new IllegalArgumentException("resource identity part must not be null");
			result.append('.').append(encoder.encodeToString(part.getBytes(StandardCharsets.UTF_8)));
		}
		return result.toString();
	}

	static List<String> decode(String resourceName) {
		if (resourceName == null) throw new IllegalArgumentException("resource name must not be null");
		String[] parts = resourceName.split("\\.", -1);
		if (parts.length < 2 || !VERSION.equals(parts[0])) {
			throw new IllegalArgumentException("unsupported resource name");
		}
		Base64.Decoder decoder = Base64.getUrlDecoder();
		List<String> identity = new ArrayList<String>(parts.length - 1);
		for (int index = 1; index < parts.length; index++) {
			identity.add(new String(decoder.decode(parts[index]), StandardCharsets.UTF_8));
		}
		return identity;
	}
}
