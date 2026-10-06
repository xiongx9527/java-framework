package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.web;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.auth.KbSaTokenAuthChecker;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDataset;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSegment;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbDocumentService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSearchHit;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSearchService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSettingService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对外 REST API（设计 §3.3/§3.4）：业务对接 + Dify External Knowledge API 兼容，同一服务。
 * 认证统一走 {@link KbSaTokenAuthChecker}（Sa-Token）；未认证返回 error_code 1002，
 * 数据集不存在返回 error_code 2001。
 *
 * <p>类级装配条件与 {@code KnowledgeConfiguration} 对齐（本控制器唯一注册入口是该配置的
 * {@code kbOpenApiController} Bean 方法，条件对它不生效，仅约束组件扫描）：应用若误将
 * {@code com.xiongx9527.fw.plus.ai.autoconfigure.**} 纳入 {@code @ComponentScan}，本类会被当作应用
 * 自己的控制器注册，而此时知识库服务层可能整体未装配（无 SqlSessionFactory 或开关关闭），
 * 导致启动期缺 {@link KbDocumentService} 报 "required a bean of type ... that could not be found"。
 * 加上条件后，误扫描时本类不注册，知识库未装配的应用照常启动。</p>
 */
@ConditionalOnClass(SqlSessionFactory.class)
@ConditionalOnBean({KbDocumentService.class, KbSearchService.class,
		com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbDatasetService.class,
		com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSegmentService.class,
		com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService.class,
		KbSaTokenAuthChecker.class,
		com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSettingService.class})
@RestController
public class KbOpenApiController {

	private final KbDocumentService documentService;

	private final KbSearchService searchService;

	private final com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbDatasetService datasetService;

	private final com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSegmentService segmentService;

	private final com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService metadataFieldService;

	private final KbSettingService settingService;

	private final KbSaTokenAuthChecker authChecker;

	public KbOpenApiController(KbDocumentService documentService, KbSearchService searchService,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbDatasetService datasetService,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSegmentService segmentService,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService metadataFieldService,
			KbSettingService settingService,
			KbSaTokenAuthChecker authChecker) {
		this.documentService = documentService;
		this.searchService = searchService;
		this.datasetService = datasetService;
		this.segmentService = segmentService;
		this.metadataFieldService = metadataFieldService;
		this.settingService = settingService;
		this.authChecker = authChecker;
	}

	// ---------------- 控制台设置（kb_setting 单行，保存即热生效） ----------------

	@GetMapping("/settings")
	public Object getSettings() {
		if (!auth(null, null)) {
			return error(1002, "认证失败");
		}
		return ok(settingService.current());
	}

	@PostMapping("/settings")
	public Object saveSettings(@RequestBody com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSetting setting) {
		if (!auth(null, null)) {
			return error(1002, "认证失败");
		}
		try {
			return ok(settingService.save(setting));
		} catch (IllegalArgumentException e) {
			return error(2001, e.getMessage());
		}
	}

	// ---------------- 业务对接接口 ----------------

	@PostMapping(value = "/v1/datasets/{datasetId}/document/upload-by-file",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Object uploadByFile(@PathVariable("datasetId") String datasetId,
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "metadata", required = false) String metadataJson,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		try {
			return ok(documentService.createByFile(datasetId, file,
					com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.web.KbConsoleController
							.parseStaticMetadataJson(metadataJson)));
		} catch (IllegalArgumentException e) {
			return error(2001, e.getMessage());
		}
	}

	@PostMapping("/v1/datasets/{datasetId}/document/create-by-text")
	public Object createByText(@PathVariable("datasetId") String datasetId,
			@RequestBody Map<String, Object> body,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		try {
			String name = body.get("name") == null ? null : String.valueOf(body.get("name"));
			String text = body.get("text") == null ? null : String.valueOf(body.get("text"));
			Object metadata = body.get("metadata");
			return ok(documentService.createByText(datasetId, name, text,
					metadata instanceof Map ? (Map<String, Object>) metadata : null));
		} catch (IllegalArgumentException e) {
			return error(2001, e.getMessage());
		}
	}

	/** 业务方文档分页列表（Sa-Token 认证）。 */
	@GetMapping("/v1/datasets/{datasetId}/documents")
	public Object listDocuments(@PathVariable("datasetId") String datasetId,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		try {
			return ok(datasetService.listDocuments(datasetId, page, pageSize));
		} catch (IllegalArgumentException e) {
			return error(2001, e.getMessage());
		}
	}

	/** 业务方分段分页列表（Sa-Token 认证）。 */
	@GetMapping("/v1/datasets/{datasetId}/documents/{documentId}/segments")
	public Object listSegments(@PathVariable("datasetId") String datasetId,
			@PathVariable("documentId") String documentId,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		return ok(segmentService.listSegments(documentId, page, pageSize));
	}

	/** 业务方单独更新文档 metadata：body 为键值对 JSON；向量快照在重建索引后同步。 */
	@PostMapping("/v1/datasets/{datasetId}/documents/{documentId}/metadata")
	public Object updateDocumentMetadata(@PathVariable("datasetId") String datasetId,
			@PathVariable("documentId") String documentId,
			@RequestBody Map<String, Object> metadata,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		try {
			requireDocumentInDataset(datasetId, documentId);
			return ok(documentService.setMetadata(documentId, metadata));
		} catch (IllegalArgumentException e) {
			return error(2001, e.getMessage());
		}
	}

	/** 业务方文本更新（create-by-text 类型）：body {text}，重算 hash 并重新入队索引。 */
	@PostMapping("/v1/datasets/{datasetId}/document/{documentId}/text")
	public Object updateDocumentText(@PathVariable("datasetId") String datasetId,
			@PathVariable("documentId") String documentId,
			@RequestBody Map<String, Object> body,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		try {
			requireDocumentInDataset(datasetId, documentId);
			String text = body.get("text") == null ? "" : String.valueOf(body.get("text"));
			Object metadata = body.get("metadata");
			return ok(documentService.updateText(documentId, text,
					metadata instanceof Map ? (Map<String, Object>) metadata : null));
		} catch (IllegalArgumentException e) {
			return error(2001, e.getMessage());
		}
	}

	/** 业务方新增分段：body {content, keywords?, answer?}，自动重嵌入。 */
	@PostMapping("/v1/datasets/{datasetId}/documents/{documentId}/segments")
	public Object createSegment(@PathVariable("datasetId") String datasetId,
			@PathVariable("documentId") String documentId,
			@RequestBody Map<String, Object> body,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		try {
			requireDocumentInDataset(datasetId, documentId);
			String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
			String keywords = body.get("keywords") == null ? null : String.valueOf(body.get("keywords"));
			String answer = body.get("answer") == null ? null : String.valueOf(body.get("answer"));
			return ok(segmentService.create(documentId, content, keywords, answer));
		} catch (IllegalArgumentException e) {
			return error(2001, e.getMessage());
		}
	}

	/** 业务方修改分段：body {content?, keywords?, answer?}，内容变更自动重嵌入。 */
	@PostMapping("/v1/datasets/{datasetId}/segments/{segmentId}")
	public Object updateSegment(@PathVariable("datasetId") String datasetId,
			@PathVariable("segmentId") String segmentId,
			@RequestBody Map<String, Object> body,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		try {
			requireSegmentInDataset(datasetId, segmentId);
			String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
			String keywords = body.get("keywords") == null ? null : String.valueOf(body.get("keywords"));
			String answer = body.get("answer") == null ? null : String.valueOf(body.get("answer"));
			return ok(segmentService.update(segmentId, content, keywords, answer));
		} catch (IllegalArgumentException e) {
			return error(2001, e.getMessage());
		}
	}

	/** 业务方分段启禁：body {enabled}。 */
	@PostMapping("/v1/datasets/{datasetId}/segments/{segmentId}/enabled")
	public Object patchSegmentEnabled(@PathVariable("datasetId") String datasetId,
			@PathVariable("segmentId") String segmentId,
			@RequestBody Map<String, Object> body,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		try {
			requireSegmentInDataset(datasetId, segmentId);
			boolean enabled = !"false".equalsIgnoreCase(String.valueOf(body.get("enabled")));
			return ok(segmentService.patchEnabled(segmentId, enabled));
		} catch (IllegalArgumentException e) {
			return error(2001, e.getMessage());
		}
	}

	/** 业务方删除分段。 */
	@PostMapping("/v1/datasets/{datasetId}/segments/{segmentId}/delete")
	public Object deleteSegment(@PathVariable("datasetId") String datasetId,
			@PathVariable("segmentId") String segmentId,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		try {
			requireSegmentInDataset(datasetId, segmentId);
			segmentService.delete(segmentId);
			return ok(null);
		} catch (IllegalArgumentException e) {
			return error(2001, e.getMessage());
		}
	}

	/** 校验文档属于指定数据集，不属于时抛 2001 语义异常。 */
	private void requireDocumentInDataset(String datasetId, String documentId) {
		KbDataset dataset = searchService.datasetOf(datasetId);
		if (dataset == null) {
			throw new IllegalArgumentException("知识库数据集不存在: " + datasetId);
		}
		com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDocument document =
				documentService.get(documentId);
		if (!datasetId.equals(document.getDatasetId())) {
			throw new IllegalArgumentException("文档不属于该数据集: " + documentId);
		}
	}

	/** 校验分段属于指定数据集。 */
	private void requireSegmentInDataset(String datasetId, String segmentId) {
		KbDataset dataset = searchService.datasetOf(datasetId);
		if (dataset == null) {
			throw new IllegalArgumentException("知识库数据集不存在: " + datasetId);
		}
		com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSegment segment =
				segmentService.getSegment(segmentId);
		if (!datasetId.equals(segment.getDatasetId())) {
			throw new IllegalArgumentException("分段不属于该数据集: " + segmentId);
		}
	}

	/** 业务方数据集元数据字段定义列表（赋值前先了解可用字段）。 */
	@GetMapping("/v1/datasets/{datasetId}/metadata-fields")
	public Object listMetadataFields(@PathVariable("datasetId") String datasetId,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		return ok(metadataFieldService.list(datasetId));
	}

	/** 业务方文档启禁（body {enabled}）：禁用文档不参与检索。 */
	@PostMapping("/v1/datasets/{datasetId}/documents/{documentId}/enabled")
	public Object patchDocumentEnabled(@PathVariable("datasetId") String datasetId,
			@PathVariable("documentId") String documentId,
			@RequestBody Map<String, Object> body,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		try {
			requireDocumentInDataset(datasetId, documentId);
			boolean enabled = !"false".equalsIgnoreCase(String.valueOf(body.get("enabled")));
			return ok(documentService.patchEnabled(documentId, enabled));
		} catch (IllegalArgumentException e) {
			return error(2001, e.getMessage());
		}
	}

	/** 业务方文档归档/取消归档（body {archived}），与启禁独立，归档不参与检索。 */
	@PostMapping("/v1/datasets/{datasetId}/documents/{documentId}/archived")
	public Object patchDocumentArchived(@PathVariable("datasetId") String datasetId,
			@PathVariable("documentId") String documentId,
			@RequestBody Map<String, Object> body,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		try {
			requireDocumentInDataset(datasetId, documentId);
			boolean archived = !"false".equalsIgnoreCase(String.valueOf(body.get("archived")));
			return ok(documentService.patchArchived(documentId, archived));
		} catch (IllegalArgumentException e) {
			return error(2001, e.getMessage());
		}
	}

	/** 业务方文档删除：级联删除分段/向量/原始文件。 */
	@PostMapping("/v1/datasets/{datasetId}/documents/{documentId}/delete")
	public Object deleteDocument(@PathVariable("datasetId") String datasetId,
			@PathVariable("documentId") String documentId,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		try {
			requireDocumentInDataset(datasetId, documentId);
			documentService.deleteDocument(documentId);
			return ok(null);
		} catch (IllegalArgumentException e) {
			return error(2001, e.getMessage());
		}
	}

	/** 业务方分段详情（Sa-Token 认证）。 */
	@GetMapping("/v1/datasets/{datasetId}/segments/{segmentId}")
	public Object segmentDetail(@PathVariable("datasetId") String datasetId,
			@PathVariable("segmentId") String segmentId,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		return ok(segmentService.getSegment(segmentId));
	}

	@GetMapping("/v1/datasets/{datasetId}/document/{documentId}")
	public Object documentStatus(@PathVariable("datasetId") String datasetId,
			@PathVariable("documentId") String documentId,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		return ok(documentService.get(documentId));
	}

	/** 业务检索（§3.3）：body {query, topK?, scoreThreshold?, metadataCondition?}。 */
	@PostMapping("/v1/datasets/{datasetId}/retrieve")
	public Object retrieve(@PathVariable("datasetId") String datasetId,
			@RequestBody Map<String, Object> body,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		try {
			String query = body.get("query") == null ? null : String.valueOf(body.get("query"));
			Integer topK = body.get("topK") == null ? null : Integer.valueOf(String.valueOf(body.get("topK")));
			Double minScore = body.get("scoreThreshold") == null ? null
					: Double.valueOf(String.valueOf(body.get("scoreThreshold")));
			Object condition = body.get("metadataCondition");
			JSONObject metadataCondition = condition instanceof Map
					? JSON.parseObject(JSON.toJSONString(condition)) : null;
			String documentId = body.get("documentId") == null ? null : String.valueOf(body.get("documentId"));
			List<KbSearchHit> hits = searchService.search(datasetId, query, topK, minScore, metadataCondition,
					documentId);
			List<Map<String, Object>> records = new ArrayList<>();
			for (KbSearchHit hit : hits) {
				Map<String, Object> record = new LinkedHashMap<>();
				record.put("content", hit.getContent());
				record.put("score", hit.getScore());
				record.put("documentName", hit.getDocumentName());
				record.put("position", hit.getPosition());
				record.put("metadata", hit.getMetadata() == null ? new LinkedHashMap<>() : hit.getMetadata());
				records.add(record);
			}
			return ok(records);
		} catch (IllegalArgumentException e) {
			return error(2001, e.getMessage());
		}
	}

	// ---------------- Dify External Knowledge API 兼容（§3.4） ----------------

	/**
	 * POST /v1/external/knowledge/retrieval：Dify 注册端点（Dify 自动追加 /retrieval 时按其规则截取 base）。
	 * 请求 {knowledge_id, query, retrieval_setting:{top_k, score_threshold}, metadata_condition?}；
	 * 响应 records:[{content, score, title, metadata}]，metadata 恒为对象。
	 */
	@PostMapping("/v1/external/knowledge/retrieval")
	public Object difyRetrieval(@RequestBody Map<String, Object> body,
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "kb-token", required = false) String kbToken) {
		if (!auth(authorization, kbToken)) {
			return error(1002, "认证失败");
		}
		String knowledgeId = body.get("knowledge_id") == null ? null : String.valueOf(body.get("knowledge_id"));
		String query = body.get("query") == null ? null : String.valueOf(body.get("query"));
		if (knowledgeId == null || knowledgeId.isEmpty()) {
			return error(2001, "knowledge_id 不能为空");
		}
		try {
			if (searchService.datasetOf(knowledgeId) == null) {
				return error(2001, "知识库不存在: " + knowledgeId);
			}
		} catch (IllegalStateException e) {
			return error(2001, e.getMessage());
		}
		Object settingObj = body.get("retrieval_setting");
		Integer topK = null;
		Double minScore = null;
		if (settingObj instanceof Map) {
			Map<?, ?> setting = (Map<?, ?>) settingObj;
			if (setting.get("top_k") != null) {
				topK = Integer.valueOf(String.valueOf(setting.get("top_k")));
			}
			if (setting.get("score_threshold") != null) {
				minScore = Double.valueOf(String.valueOf(setting.get("score_threshold")));
			}
		}
		Object condition = body.get("metadata_condition");
		JSONObject metadataCondition = condition instanceof Map
				? JSON.parseObject(JSON.toJSONString(condition)) : null;
		List<KbSearchHit> hits = searchService.search(knowledgeId, query, topK, minScore, metadataCondition);
		return okRecords(hits);
	}

	/** Dify 响应格式：{"records":[{content, score, title, metadata}]}，metadata 恒为对象。 */
	private static Object okRecords(List<KbSearchHit> hits) {
		Map<String, Object> response = new LinkedHashMap<>();
		JSONArray records = new JSONArray();
		for (KbSearchHit hit : hits) {
			JSONObject record = new JSONObject();
			record.put("content", hit.getContent());
			record.put("score", hit.getScore() == null ? 0.0 : hit.getScore());
			record.put("title", hit.getDocumentName() == null ? "" : hit.getDocumentName());
			JSONObject metadata = hit.getMetadata() == null ? new JSONObject()
					: JSON.parseObject(JSON.toJSONString(hit.getMetadata()));
			if (metadata == null) {
				metadata = new JSONObject();
			}
			record.put("metadata", metadata);
			records.add(record);
		}
		response.put("records", records);
		return response;
	}

	// ---------------- 内部 ----------------

	private boolean auth(String authorization, String kbToken) {
		Map<String, String> headers = collectHeaders();
		// 显式参数优先（兼容直接调用 / 测试），真实请求由 RequestContextHolder 提供全量请求头
		if (StringUtils.hasText(authorization)) {
			headers.put("Authorization", authorization);
		}
		if (StringUtils.hasText(kbToken)) {
			headers.put("kb-token", kbToken);
		}
		try {
			authChecker.check(headers);
			return true;
		} catch (KbSaTokenAuthChecker.KbUnauthorizedException e) {
			return false;
		}
	}

	/**
	 * 读取当前请求的全量请求头：provider 化后不同 provider 从不同 header 取凭据（
	 * {@code Authorization}、Sa-Token tokenName、{@code apikey}、{@code ticket} 等），
	 * 因此不能再只透传固定的两个参数。无请求上下文时返回空表。
	 */
	private static Map<String, String> collectHeaders() {
		Map<String, String> headers = new LinkedHashMap<>();
		org.springframework.web.context.request.RequestAttributes attributes =
				org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
		if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
			javax.servlet.http.HttpServletRequest request =
					((org.springframework.web.context.request.ServletRequestAttributes) attributes).getRequest();
			java.util.Enumeration<String> names = request.getHeaderNames();
			while (names != null && names.hasMoreElements()) {
				String name = names.nextElement();
				headers.put(name, request.getHeader(name));
			}
		}
		return headers;
	}

	private static Object ok(Object data) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("data", data);
		return result;
	}

	private static Object error(int errorCode, String message) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("error_code", errorCode);
		result.put("error_msg", message);
		return result;
	}
}
