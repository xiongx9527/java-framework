package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeAccessKeyRepository;
import com.xiongx9527.fw.plus.common.exception.BizException;
import com.xiongx9527.fw.plus.dify.dataset.types.external.KnowledgeArgs;
import com.xiongx9527.fw.plus.dify.dataset.types.external.KnowledgeMetadataCondition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class TinyflowDifyExternalController {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private final TinyflowDifyExternalKnowledgeService service;

	public TinyflowDifyExternalController(TinyflowDifyExternalKnowledgeService service) {
		if (service == null) {
			throw new IllegalArgumentException("Tinyflow Dify external service must not be null");
		}
		this.service = service;
	}

	@PostMapping(value = "${fw.plus.ai.tinyflow.dify.external.base-path:/external-knowledge}/retrieval",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> retrieval(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestBody String body) {
		try {
			String token = bearerToken(authorization);
			TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey = service.authenticateAccessKey(token);
			String knowledgeId = extractKnowledgeId(body);
			if (knowledgeId != null) {
				service.authorizeScope(accessKey, knowledgeId);
			}
			return ResponseEntity.ok(service.retrieval(accessKey, parse(body)));
		} catch (HeaderException exception) {
			return error(403, 1001, exception.getMessage());
		} catch (TinyflowDifyExternalKnowledgeService.ExternalKnowledgeException exception) {
			return error(exception.getStatus(), exception.getCode(), exception.getMessage());
		} catch (RuntimeException exception) {
			return error(500, 5000, "Internal server error");
		}
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> malformedBody() {
		return error(400, 1003, "Invalid request");
	}

	private String bearerToken(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			throw new HeaderException();
		}
		String token = authorization.substring("Bearer ".length());
		if (token.length() == 0 || token.trim().length() != token.length() || token.indexOf(' ') >= 0
				|| token.indexOf('\t') >= 0) {
			throw new HeaderException();
		}
		return token;
	}

	private KnowledgeArgs parse(String rawBody) {
		JsonNode body;
		try {
			body = OBJECT_MAPPER.readTree(rawBody);
		} catch (IOException exception) {
			throw invalidRequest();
		}
		if (body == null || !body.isObject()) {
			throw invalidRequest();
		}
		KnowledgeArgs args = new KnowledgeArgs();
		args.setKnowledge_id(text(body.get("knowledge_id")));
		args.setQuery(text(body.get("query")));

		JsonNode settingNode = body.get("retrieval_setting");
		if (settingNode != null && !settingNode.isNull()) {
			if (!settingNode.isObject()) {
				throw invalidRequest();
			}
			KnowledgeArgs.Setting setting = new KnowledgeArgs.Setting();
			JsonNode topK = settingNode.get("top_k");
			if (topK != null && !topK.isNull()) {
				if (!topK.isIntegralNumber() || !topK.canConvertToInt()) {
					throw invalidRequest();
				}
				setting.setTop_k(Integer.valueOf(topK.intValue()));
			}
			JsonNode threshold = settingNode.get("score_threshold");
			if (threshold != null && !threshold.isNull()) {
				if (!threshold.isNumber()) {
					throw invalidRequest();
				}
				setting.setScore_threshold(Double.valueOf(threshold.doubleValue()));
			}
			args.setRetrieval_setting(setting);
		}

		JsonNode metadataNode = body.get("metadata_condition");
		if (metadataNode != null && !metadataNode.isNull()) {
			if (!metadataNode.isObject()) {
				throw invalidRequest();
			}
			KnowledgeMetadataCondition metadata = new KnowledgeMetadataCondition();
			metadata.setLogical_operator(text(metadataNode.get("logical_operator")));
			JsonNode conditionsNode = metadataNode.get("conditions");
			if (conditionsNode != null && !conditionsNode.isNull()) {
				if (!conditionsNode.isArray()) {
					throw invalidRequest();
				}
				List<KnowledgeMetadataCondition.Condition> conditions =
						new ArrayList<KnowledgeMetadataCondition.Condition>();
				for (JsonNode conditionNode : conditionsNode) {
					if (!conditionNode.isObject()) {
						throw invalidRequest();
					}
					KnowledgeMetadataCondition.Condition condition = new KnowledgeMetadataCondition.Condition();
					condition.setName(text(conditionNode.get("name")));
					condition.setComparison_operator(text(conditionNode.get("comparison_operator")));
					JsonNode value = conditionNode.get("value");
					condition.setValue(value == null || value.isNull() ? null
							: OBJECT_MAPPER.convertValue(value, Object.class));
					conditions.add(condition);
				}
				metadata.setConditions(conditions);
			}
			args.setMetadata_condition(metadata);
		}
		return args;
	}

	private String extractKnowledgeId(String rawBody) {
		if (rawBody == null) {
			return null;
		}
		try (JsonParser parser = OBJECT_MAPPER.getFactory().createParser(rawBody)) {
			if (parser.nextToken() != JsonToken.START_OBJECT) {
				return null;
			}
			while (parser.nextToken() == JsonToken.FIELD_NAME) {
				String fieldName = parser.getCurrentName();
				JsonToken valueToken = parser.nextToken();
				if ("knowledge_id".equals(fieldName) && valueToken == JsonToken.VALUE_STRING) {
					return parser.getValueAsString();
				}
				parser.skipChildren();
			}
		} catch (IOException ignored) {
			return null;
		}
		return null;
	}

	private String text(JsonNode node) {
		return node == null || !node.isTextual() ? null : node.textValue();
	}

	private TinyflowDifyExternalKnowledgeService.ExternalKnowledgeException invalidRequest() {
		return new TinyflowDifyExternalKnowledgeService.ExternalKnowledgeException(400, 1003, "Invalid request");
	}

	private ResponseEntity<Map<String, Object>> error(int status, int code, String message) {
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("error_code", Integer.valueOf(code));
		body.put("error_msg", message);
		return ResponseEntity.status(HttpStatus.valueOf(status)).contentType(MediaType.APPLICATION_JSON).body(body);
	}

	private static final class HeaderException extends BizException {
		private static final long serialVersionUID = 1L;
		private HeaderException() { super("Authorization header is required"); }
	}
}
