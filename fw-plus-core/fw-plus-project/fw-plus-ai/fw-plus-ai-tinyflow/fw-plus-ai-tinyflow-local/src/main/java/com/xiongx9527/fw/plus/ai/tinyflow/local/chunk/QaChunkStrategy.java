package com.xiongx9527.fw.plus.ai.tinyflow.local.chunk;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunkStrategy;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowParsedDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QaChunkStrategy implements TinyflowChunkStrategy {

	@Override
	public List<TinyflowChunk> chunk(TinyflowParsedDocument parsed, ChunkOptions options) {
		NormalChunkStrategy.require(parsed, options);
		String questionPrefix = string(options.getOptions().get("questionPrefix"), "Q:");
		String answerPrefix = string(options.getOptions().get("answerPrefix"), "A:");
		List<TinyflowChunk> chunks = new ArrayList<TinyflowChunk>();
		String question = null;
		StringBuilder answer = new StringBuilder();
		int position = 0;
		for (String rawLine : parsed.getContent().split("\\r?\\n")) {
			String line = rawLine.trim();
			if (line.startsWith(questionPrefix)) {
				if (question != null && answer.length() > 0) {
					chunks.add(create(parsed, options, question, answer.toString(), position++));
				}
				question = line.substring(questionPrefix.length()).trim();
				answer.setLength(0);
			} else if (line.startsWith(answerPrefix)) {
				answer.setLength(0);
				answer.append(line.substring(answerPrefix.length()).trim());
			} else if (question != null && !line.isEmpty()) {
				if (answer.length() > 0) {
					answer.append('\n');
				}
				answer.append(line);
			}
		}
		if (question != null && answer.length() > 0) {
			chunks.add(create(parsed, options, question, answer.toString(), position));
		}
		if (chunks.isEmpty()) {
			throw new IllegalArgumentException("Q&A content contains no complete question and answer pair");
		}
		return chunks;
	}

	private TinyflowChunk create(TinyflowParsedDocument parsed, ChunkOptions options,
			String question, String answer, int position) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("chunkRole", "qa");
		metadata.put("question", question);
		metadata.put("answer", answer);
		return NormalChunkStrategy.buildChunk(options, parsed, null,
				"Q: " + question + "\nA: " + answer, "qa", position, null, metadata);
	}

	private String string(Object value, String defaultValue) {
		return value == null ? defaultValue : String.valueOf(value);
	}
}
