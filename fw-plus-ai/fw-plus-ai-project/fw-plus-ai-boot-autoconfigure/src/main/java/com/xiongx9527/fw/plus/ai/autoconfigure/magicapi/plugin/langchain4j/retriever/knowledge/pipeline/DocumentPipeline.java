package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文档解析与通用切分（设计 §2.3）：easy-rag 传递依赖的 Tika DocumentParser 全格式解析；
 * 通用分段 {@code DocumentSplitters.recursive(maxSegmentSize, maxOverlap)} 字符版，
 * {@code segment_delimiter} 非空时先按其预切。父子模式见 {@link ParentChildSplitter}。
 */
public class DocumentPipeline {

	private final ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();

	/** 解析原始文档为纯文本（PDF/Word/Excel/HTML/MD/TXT 等全格式）。 */
	public String parse(InputStream content) {
		try {
			Document document = parser.parse(content);
			String text = document.text();
			return text == null ? "" : text;
		} catch (Exception e) {
			throw new IllegalStateException("文档解析失败: " + e.getMessage(), e);
		}
	}

	/** 通用分段：可选分隔符预切 + recursive 递归切分，返回分段文本（保序）。 */
	public List<String> splitGeneral(String text, int maxSegmentSize, int maxOverlapSize, String delimiter) {
		if (!StringUtils.hasText(text)) {
			return Collections.emptyList();
		}
		List<String> blocks = StringUtils.hasText(delimiter)
				? splitByDelimiter(text, delimiter)
				: Collections.singletonList(text);
		DocumentSplitter splitter = DocumentSplitters.recursive(
				maxSegmentSize > 0 ? maxSegmentSize : 1000,
				maxOverlapSize >= 0 ? maxOverlapSize : 200);
		List<String> result = new ArrayList<>();
		for (String block : blocks) {
			for (TextSegment segment : splitter.split(Document.document(block))) {
				if (StringUtils.hasText(segment.text())) {
					result.add(segment.text());
				}
			}
		}
		return result;
	}

	private static List<String> splitByDelimiter(String text, String delimiter) {
		List<String> blocks = new ArrayList<>();
		int start = 0;
		int index;
		while ((index = text.indexOf(delimiter, start)) >= 0) {
			if (index > start) {
				blocks.add(text.substring(start, index));
			}
			start = index + delimiter.length();
		}
		if (start < text.length()) {
			blocks.add(text.substring(start));
		}
		return blocks.isEmpty() ? Collections.singletonList(text) : blocks;
	}
}
