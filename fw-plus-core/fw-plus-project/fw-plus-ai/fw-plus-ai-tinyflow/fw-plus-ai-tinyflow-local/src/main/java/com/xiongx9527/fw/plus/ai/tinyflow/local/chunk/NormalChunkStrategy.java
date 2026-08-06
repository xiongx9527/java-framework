package com.xiongx9527.fw.plus.ai.tinyflow.local.chunk;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunkStrategy;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowParsedDocument;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NormalChunkStrategy implements TinyflowChunkStrategy {

	@Override
	public List<TinyflowChunk> chunk(TinyflowParsedDocument parsed, ChunkOptions options) {
		require(parsed, options);
		int chunkSize = integer(options.getOptions(), "chunkSize", 800);
		int overlap = integer(options.getOptions(), "overlap", 100);
		if (chunkSize <= 0 || overlap < 0 || overlap >= chunkSize) {
			throw new IllegalArgumentException("chunkSize must be positive and overlap smaller than chunkSize");
		}
		List<TinyflowParsedDocument.Section> sections = parsed.getSections().isEmpty()
				? Collections.singletonList(new TinyflowParsedDocument.Section(parsed.getContent(), null,
						Collections.<String, Object>emptyMap())) : parsed.getSections();
		List<TinyflowChunk> chunks = new ArrayList<TinyflowChunk>();
		int position = 0;
		for (TinyflowParsedDocument.Section section : sections) {
			String content = section.getContent();
			for (int start = 0; start < content.length(); start += chunkSize - overlap) {
				int end = Math.min(content.length(), start + chunkSize);
				String value = content.substring(start, end).trim();
				if (!value.isEmpty()) {
					chunks.add(buildChunk(options, parsed, section, value, "normal", position++, null, null));
				}
				if (end == content.length()) {
					break;
				}
			}
		}
		return chunks;
	}

	static TinyflowChunk buildChunk(ChunkOptions options, TinyflowParsedDocument parsed,
			TinyflowParsedDocument.Section section, String content, String strategy, int position,
			String parentChunkId, Map<String, Object> additionalMetadata) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.putAll(parsed.getMetadata());
		if (section != null) {
			metadata.putAll(section.getMetadata());
		}
		if (additionalMetadata != null) {
			metadata.putAll(additionalMetadata);
		}
		String chunkId = options.getDocumentId() + "-" + strategy + "-" + position + "-"
				+ digest(content + "|" + (section == null ? "" : section.getLocation()));
		return TinyflowChunk.builder().tenantId(options.getTenantId()).knowledgeId(options.getKnowledgeId())
				.sourceId(options.getSourceId()).sourceType(options.getSourceType())
				.documentId(options.getDocumentId()).chunkId(chunkId).content(content)
				.sourceUri(string(options.getOptions().get("sourceUri")))
				.sourceVersion(string(options.getOptions().get("sourceVersion")))
				.location(section == null ? null : section.getLocation()).position(position)
				.parentChunkId(parentChunkId).tokenCount(tokenCount(content)).metadata(metadata).build();
	}

	static int integer(Map<String, Object> options, String key, int defaultValue) {
		Object value = options.get(key);
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(value));
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(key + " must be an integer");
		}
	}

	static void require(TinyflowParsedDocument parsed, ChunkOptions options) {
		if (parsed == null) {
			throw new IllegalArgumentException("parsed must not be null");
		}
		if (options == null) {
			throw new IllegalArgumentException("options must not be null");
		}
	}

	private static int tokenCount(String content) {
		String value = content.trim();
		return value.isEmpty() ? 0 : value.split("\\s+").length;
	}

	private static String string(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private static String digest(String value) {
		try {
			byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder result = new StringBuilder();
			for (int index = 0; index < 6; index++) {
				result.append(String.format("%02x", bytes[index] & 0xff));
			}
			return result.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}
}
