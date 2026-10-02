package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 父子分段切分器（设计 §2.3）：大粒度 recursive 切父段（仅落表，不入向量库），
 * 小粒度 recursive 切子段（嵌入，metadata 带 parent_id）。
 *
 * <p>父段大小取数据集 {@code segment_max_length}（默认 1000 字符），子段大小取其 1/4（下限 200 字符），
 * 重叠按同比例取 1/4（下限 50）。{@code segment_delimiter} 非空时先按其预切再递归切分。</p>
 */
public class ParentChildSplitter {

	/** 一次切分结果：父段文本 + 父段序号 + 子段文本（子段序号在父段内）。 */
	public static class Piece {

		private final int parentPosition;

		private final String parentContent;

		private final int childPosition;

		private final String childContent;

		public Piece(int parentPosition, String parentContent, int childPosition, String childContent) {
			this.parentPosition = parentPosition;
			this.parentContent = parentContent;
			this.childPosition = childPosition;
			this.childContent = childContent;
		}

		public int getParentPosition() {
			return parentPosition;
		}

		public String getParentContent() {
			return parentContent;
		}

		public int getChildPosition() {
			return childPosition;
		}

		public String getChildContent() {
			return childContent;
		}
	}

	private final int parentSize;

	private final int parentOverlap;

	private final int childSize;

	private final int childOverlap;

	public ParentChildSplitter(int segmentMaxLength, int segmentOverlap) {
		int max = segmentMaxLength > 0 ? segmentMaxLength : 1000;
		int overlap = segmentOverlap >= 0 ? segmentOverlap : max / 5;
		this.parentSize = max;
		this.parentOverlap = Math.min(overlap, Math.max(0, max / 5));
		// 子段粒度恒小于父段（约 1/2，下限 80 字符），保证父子结构有意义
		this.childSize = Math.max(80, max / 2);
		this.childOverlap = Math.max(20, Math.min(overlap, childSize / 4));
	}

	/**
	 * 切分：{@code delimiter} 非空时先按其预切（保留片段完整性），每片再切父子两级。
	 */
	public List<Piece> split(String text, String delimiter) {
		List<Piece> pieces = new ArrayList<>();
		if (!StringUtils.hasText(text)) {
			return pieces;
		}
		List<String> blocks = StringUtils.hasText(delimiter)
				? splitByDelimiter(text, delimiter)
				: java.util.Collections.singletonList(text);
		int parentPosition = 0;
		for (String block : blocks) {
			for (String parent : recursive(block, parentSize, parentOverlap)) {
				int childPosition = 0;
				for (String child : recursive(parent, childSize, childOverlap)) {
					pieces.add(new Piece(parentPosition, parent, childPosition++, child));
				}
				parentPosition++;
			}
		}
		return pieces;
	}

	private List<String> recursive(String text, int maxSegmentSize, int maxOverlapSize) {
		DocumentSplitter splitter = DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize);
		List<TextSegment> segments = splitter.split(Document.document(text));
		List<String> result = new ArrayList<>(segments.size());
		for (TextSegment segment : segments) {
			if (StringUtils.hasText(segment.text())) {
				result.add(segment.text());
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
		return blocks.isEmpty() ? java.util.Collections.singletonList(text) : blocks;
	}
}
