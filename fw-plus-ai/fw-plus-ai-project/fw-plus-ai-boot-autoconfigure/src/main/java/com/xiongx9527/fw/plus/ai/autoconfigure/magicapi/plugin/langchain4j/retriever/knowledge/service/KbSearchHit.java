package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSegment;

import java.util.Map;

/**
 * 一次召回命中：分段内容 + score + 文档信息 + 可选父段预览（设计 §3.1/§3.4）。
 */
public class KbSearchHit {

	private final String datasetId;

	private final String documentId;

	private final String documentName;

	private final String segmentId;

	private final int position;

	private final String content;

	private final String answer;

	private final String keywords;

	private final Double score;

	private final String parentContent;

	private final Map<String, Object> metadata;

	public KbSearchHit(String datasetId, String documentId, String documentName, String segmentId,
			int position, String content, String answer, String keywords, Double score,
			String parentContent, Map<String, Object> metadata) {
		this.datasetId = datasetId;
		this.documentId = documentId;
		this.documentName = documentName;
		this.segmentId = segmentId;
		this.position = position;
		this.content = content;
		this.answer = answer;
		this.keywords = keywords;
		this.score = score;
		this.parentContent = parentContent;
		this.metadata = metadata;
	}

	public static KbSearchHit of(KbSegment segment, String documentName, Double score, String parentContent,
			Map<String, Object> metadata) {
		return new KbSearchHit(segment.getDatasetId(), segment.getDocumentId(), documentName, segment.getId(),
				segment.getPosition() == null ? 0 : segment.getPosition(), segment.getContent(),
				segment.getAnswer(), segment.getKeywords(), score, parentContent, metadata);
	}

	public String getDatasetId() {
		return datasetId;
	}

	public String getDocumentId() {
		return documentId;
	}

	public String getDocumentName() {
		return documentName;
	}

	public String getSegmentId() {
		return segmentId;
	}

	public int getPosition() {
		return position;
	}

	public String getContent() {
		return content;
	}

	public String getAnswer() {
		return answer;
	}

	public String getKeywords() {
		return keywords;
	}

	public Double getScore() {
		return score;
	}

	public String getParentContent() {
		return parentContent;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}
}
