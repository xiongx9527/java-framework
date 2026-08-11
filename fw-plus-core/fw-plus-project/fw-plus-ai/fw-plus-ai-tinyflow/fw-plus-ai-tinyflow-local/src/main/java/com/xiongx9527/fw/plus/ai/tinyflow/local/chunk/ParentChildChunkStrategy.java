package com.xiongx9527.fw.plus.ai.tinyflow.local.chunk;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunk;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowChunkStrategy;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowParsedDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ParentChildChunkStrategy implements TinyflowChunkStrategy {

	@Override
	public List<TinyflowChunk> chunk(TinyflowParsedDocument parsed, ChunkOptions options) {
		NormalChunkStrategy.require(parsed, options);
		int parentSize = NormalChunkStrategy.integer(options.getOptions(), "parentSize", 1600);
		int childSize = NormalChunkStrategy.integer(options.getOptions(), "childSize", 400);
		if (parentSize <= 0 || childSize <= 0 || childSize >= parentSize) {
			throw new IllegalArgumentException("parentSize must be greater than childSize and both must be positive");
		}
		List<TinyflowParsedDocument.Section> sections = parsed.getSections().isEmpty()
				? Collections.singletonList(new TinyflowParsedDocument.Section(parsed.getContent(), null,
						Collections.<String, Object>emptyMap())) : parsed.getSections();
		List<TinyflowChunk> chunks = new ArrayList<TinyflowChunk>();
		int position = 0;
		for (TinyflowParsedDocument.Section section : sections) {
			String content = section.getContent();
			for (int parentStart = 0; parentStart < content.length(); parentStart += parentSize) {
				String parentContent = content.substring(parentStart,
						Math.min(content.length(), parentStart + parentSize)).trim();
				if (parentContent.isEmpty()) {
					continue;
				}
				Map<String, Object> parentMetadata = new LinkedHashMap<String, Object>();
				parentMetadata.put("chunkRole", "parent");
				TinyflowChunk parent = NormalChunkStrategy.buildChunk(options, parsed, section,
						parentContent, "parent", position++, null, parentMetadata);
				chunks.add(parent);
				for (int childStart = 0; childStart < parentContent.length(); childStart += childSize) {
					String childContent = parentContent.substring(childStart,
							Math.min(parentContent.length(), childStart + childSize)).trim();
					if (!childContent.isEmpty()) {
						Map<String, Object> childMetadata = new LinkedHashMap<String, Object>();
						childMetadata.put("chunkRole", "child");
						chunks.add(NormalChunkStrategy.buildChunk(options, parsed, section,
								childContent, "child", position++, parent.getChunkId(), childMetadata));
					}
				}
			}
		}
		return chunks;
	}
}
