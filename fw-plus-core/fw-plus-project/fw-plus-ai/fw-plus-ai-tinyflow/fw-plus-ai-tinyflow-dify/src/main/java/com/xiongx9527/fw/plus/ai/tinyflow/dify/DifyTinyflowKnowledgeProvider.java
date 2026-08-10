package com.xiongx9527.fw.plus.ai.tinyflow.dify;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowCredentialResolver;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowDifyConnectionRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeCapabilities;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeMatch;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import com.xiongx9527.fw.plus.dify.dataset.IDatasetClient;
import com.xiongx9527.fw.plus.dify.dataset.entity.Document;
import com.xiongx9527.fw.plus.dify.dataset.entity.Segment;
import com.xiongx9527.fw.plus.dify.dataset.types.RetrieveResult;
import com.xiongx9527.fw.plus.dify.dataset.types.RetrievalModel;
import com.xiongx9527.fw.plus.dify.types.DifyException;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public final class DifyTinyflowKnowledgeProvider implements TinyflowKnowledgeProvider {

	private static final String PROVIDER_KEY = "dify";
	private final TinyflowDifyConnectionRepository connections;
	private final DifyDatasetClientFactory clientFactory;

	public DifyTinyflowKnowledgeProvider(TinyflowDifyConnectionRepository connections,
			TinyflowCredentialResolver credentialResolver) {
		this(connections, new DifyDatasetClientFactory(credentialResolver));
	}

	public DifyTinyflowKnowledgeProvider(TinyflowDifyConnectionRepository connections,
			DifyDatasetClientFactory clientFactory) {
		if (connections == null || clientFactory == null) {
			throw new IllegalArgumentException("connections and clientFactory must not be null");
		}
		this.connections = connections;
		this.clientFactory = clientFactory;
	}

	@Override
	public String getKey() {
		return PROVIDER_KEY;
	}

	@Override
	public TinyflowKnowledgeCapabilities capabilities() {
		return TinyflowKnowledgeCapabilities.retrievalOnly();
	}

	@Override
	public TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request) {
		if (request == null) {
			throw new TinyflowException("Dify retrieval request is invalid");
		}
		DifyTinyflowConnection connection = connection(request);
		try {
			IDatasetClient client = clientFactory.create(connection);
			return map(request, client.retrieve(connection.getDatasetId(), request.getQuery(), toRetrievalModel(request)));
		} catch (TinyflowException e) {
			throw e;
		} catch (RuntimeException e) {
			throw translate(e);
		}
	}

	private DifyTinyflowConnection connection(TinyflowKnowledgeRequest request) {
		List<TinyflowDifyConnectionRepository.DifyConnection> candidates = connections.findByKnowledge(
				request.getTenantId(), request.getKnowledgeId());
		if (candidates == null || candidates.isEmpty()) {
			throw new TinyflowException("Dify connection not found");
		}
		if (candidates.size() != 1) {
			throw new TinyflowException("Dify connection is ambiguous");
		}
		TinyflowDifyConnectionRepository.DifyConnection connection = candidates.get(0);
		if (connection == null || !request.getTenantId().equals(connection.getTenantId())
				|| !request.getKnowledgeId().equals(connection.getKnowledgeId())) {
			throw new TinyflowException("Dify connection not found");
		}
		return DifyTinyflowConnection.from(connection);
	}

	private RetrievalModel toRetrievalModel(TinyflowKnowledgeRequest request) {
		return RetrievalModel.builder().topK(request.getTopK()).scoreThreshold(request.getThreshold())
				.scoreThresholdEnabled(Boolean.valueOf(request.getThreshold() != null)).build();
	}

	private TinyflowKnowledgeResult map(TinyflowKnowledgeRequest request, RetrieveResult response) {
		if (response == null || response.getRecords() == null) {
			throw invalidResponse();
		}
		List<TinyflowKnowledgeMatch> matches = new ArrayList<TinyflowKnowledgeMatch>();
		for (RetrieveResult.Record record : response.getRecords()) {
			matches.add(mapRecord(request, record));
		}
		return TinyflowKnowledgeResult.builder().matches(matches).build();
	}

	private TinyflowKnowledgeMatch mapRecord(TinyflowKnowledgeRequest request, RetrieveResult.Record record) {
		if (record == null || record.getScore() == null || Double.isNaN(record.getScore().doubleValue())
				|| Double.isInfinite(record.getScore().doubleValue()) || record.getSegment() == null) {
			throw invalidResponse();
		}
		Segment segment = record.getSegment();
		String documentId = documentId(segment);
		String sourceId = sourceId(segment.getDocument(), documentId);
		String sourceType = sourceType(segment.getDocument());
		String chunkId = text(segment.getId());
		String content = text(segment.getContent());
		if (chunkId == null || content == null) {
			throw invalidResponse();
		}
		return TinyflowKnowledgeMatch.builder().tenantId(request.getTenantId()).knowledgeId(request.getKnowledgeId())
				.sourceId(sourceId).sourceType(sourceType).documentId(documentId).chunkId(chunkId).content(content)
				.position(segment.getPosition()).tokenCount(tokenCount(segment.getTokens()))
				.metadata(metadata(segment.getDocument())).enabled(!Boolean.FALSE.equals(segment.getEnabled()))
				.score(record.getScore().doubleValue()).build();
	}

	private String documentId(Segment segment) {
		String documentId = segment.getDocument() == null ? null : text(segment.getDocument().getId());
		if (documentId == null) {
			documentId = text(segment.getDocumentId());
		}
		if (documentId == null) {
			throw invalidResponse();
		}
		return documentId;
	}

	private String sourceId(Document document, String documentId) {
		if (document != null && document.getDataSourceInfo() != null) {
			JSONObject info = document.getDataSourceInfo();
			for (String key : new String[] {"id", "source_id", "sourceId", "file_id", "fileId", "url"}) {
				String value = text(info.get(key));
				if (value != null) {
					return value;
				}
			}
		}
		return documentId;
	}

	private String sourceType(Document document) {
		String sourceType = document == null ? null : text(document.getDataSourceType());
		return sourceType == null ? PROVIDER_KEY : sourceType;
	}

	private Integer tokenCount(Long tokens) {
		if (tokens == null || tokens.longValue() < 0L || tokens.longValue() > Integer.MAX_VALUE) {
			return null;
		}
		return Integer.valueOf(tokens.intValue());
	}

	private Map<String, Object> metadata(Document document) {
		if (document == null || text(document.getName()) == null) {
			return Collections.emptyMap();
		}
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		metadata.put("documentName", text(document.getName()));
		return metadata;
	}

	private TinyflowException translate(RuntimeException error) {
		DifyException difyError = findDifyException(error);
		if (difyError != null && (Integer.valueOf(401).equals(difyError.getHttpStatus())
				|| Integer.valueOf(403).equals(difyError.getHttpStatus()))) {
			return new TinyflowException("Dify access denied");
		}
		if (difyError != null && Integer.valueOf(404).equals(difyError.getHttpStatus())) {
			return new TinyflowException("Dify resource not found");
		}
		if (isTimeout(error)) {
			return new TinyflowException("Dify retrieval timed out");
		}
		return new TinyflowException("Dify retrieval failed");
	}

	private DifyException findDifyException(Throwable error) {
		Throwable current = error;
		while (current != null) {
			if (current instanceof DifyException) {
				return (DifyException) current;
			}
			current = current.getCause();
		}
		return null;
	}

	private boolean isTimeout(Throwable error) {
		Throwable current = error;
		while (current != null) {
			if (current instanceof SocketTimeoutException || current instanceof InterruptedIOException
					|| current instanceof TimeoutException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private TinyflowException invalidResponse() {
		return new TinyflowException("Dify retrieval response is invalid");
	}

	private String text(Object value) {
		if (value == null) {
			return null;
		}
		String text = String.valueOf(value).trim();
		return text.length() == 0 ? null : text;
	}
}
