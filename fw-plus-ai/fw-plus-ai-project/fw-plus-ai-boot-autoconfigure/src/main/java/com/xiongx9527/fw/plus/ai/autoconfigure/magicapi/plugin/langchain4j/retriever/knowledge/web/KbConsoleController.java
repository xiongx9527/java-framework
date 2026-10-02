package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.web;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.auth.KbSaTokenAuthChecker;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDataset;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSegment;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbDatasetService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbDocumentService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSearchHit;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSegmentService;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理端接口（设计 §3.1，前缀 {@code /magic/web/kb/}，随 MagicController 鉴权）。
 * 返回统一包装 {code, message, data}；code=200 成功。
 */
@RequestMapping("/kb")
public class KbConsoleController extends MagicController implements MagicExceptionHandler {

	private final KbDatasetService datasetService;

	private final KbDocumentService documentService;

	private final KbSegmentService segmentService;

	private final com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService metadataFieldService;

	private final com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbIndexingService indexingService;

	public KbConsoleController(MagicConfiguration configuration, KbDatasetService datasetService,
			KbDocumentService documentService, KbSegmentService segmentService,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService metadataFieldService,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbIndexingService indexingService) {
		super(configuration);
		this.datasetService = datasetService;
		this.documentService = documentService;
		this.segmentService = segmentService;
		this.metadataFieldService = metadataFieldService;
		this.indexingService = indexingService;
	}

	// ---------------- 元数据字段定义 ----------------

	@GetMapping("/dataset/{id}/metadata-fields")
	@ResponseBody
	public Object listMetadataFields(@PathVariable("id") String id) {
		return ok(metadataFieldService.list(id));
	}

	@PostMapping("/dataset/{id}/metadata-fields")
	@ResponseBody
	public Object createMetadataField(@PathVariable("id") String id, @RequestBody Map<String, Object> body) {
		String name = body.get("name") == null ? null : String.valueOf(body.get("name"));
		String type = body.get("type") == null ? null : String.valueOf(body.get("type"));
		String description = body.get("description") == null ? null : String.valueOf(body.get("description"));
		return ok(metadataFieldService.create(id, name, type, description));
	}

	@RequestMapping(value = "/metadata-fields/{fieldId}", method = {RequestMethod.PUT, RequestMethod.POST})
	@ResponseBody
	public Object updateMetadataField(@PathVariable("fieldId") String fieldId,
			@RequestBody Map<String, Object> body) {
		String type = body.get("type") == null ? null : String.valueOf(body.get("type"));
		String description = body.get("description") == null ? null : String.valueOf(body.get("description"));
		return ok(metadataFieldService.update(fieldId, type, description));
	}

	@DeleteMapping("/metadata-fields/{fieldId}")
	@ResponseBody
	public Object deleteMetadataField(@PathVariable("fieldId") String fieldId) {
		metadataFieldService.delete(fieldId);
		return ok(null);
	}

	@PostMapping("/metadata-fields/{fieldId}/delete")
	@ResponseBody
	public Object deleteMetadataFieldPost(@PathVariable("fieldId") String fieldId) {
		metadataFieldService.delete(fieldId);
		return ok(null);
	}

	// ---------------- 数据集 ----------------

	@GetMapping("/dataset")
	@ResponseBody
	public Object listDatasets() {
		return ok(datasetService.list());
	}

	@PostMapping("/dataset")
	@ResponseBody
	public Object createDataset(@RequestBody KbDataset dataset) {
		return ok(datasetService.create(dataset));
	}

	@GetMapping("/dataset/{id}")
	@ResponseBody
	public Object getDataset(@PathVariable("id") String id) {
		return ok(datasetService.get(id));
	}

	@RequestMapping(value = "/dataset/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
	@ResponseBody
	public Object updateDataset(@PathVariable("id") String id, @RequestBody KbDataset dataset) {
		return ok(datasetService.update(id, dataset));
	}

	@RequestMapping(value = "/dataset/{id}", method = {RequestMethod.DELETE, RequestMethod.POST})
	@ResponseBody
	public Object deleteDataset(@PathVariable("id") String id) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("referencedRetrievers", datasetService.delete(id));
		return ok(data);
	}

	/** 召回测试（支持 metadata 过滤条件，格式对齐 Dify metadata_condition）。 */
	@PostMapping("/dataset/{id}/hitTest")
	@ResponseBody
	public Object hitTest(@PathVariable("id") String id, @RequestBody Map<String, Object> body) {
		String query = body.get("query") == null ? null : String.valueOf(body.get("query"));
		Integer topK = body.get("topK") == null ? null : Integer.valueOf(String.valueOf(body.get("topK")));
		Double minScore = body.get("minScore") == null ? null : Double.valueOf(String.valueOf(body.get("minScore")));
		Object condition = body.get("metadataCondition");
		String documentId = body.get("documentId") == null ? null : String.valueOf(body.get("documentId"));
		List<KbSearchHit> hits = datasetService.hitTest(id, query, topK, minScore,
				condition instanceof Map ? JSON.parseObject(JSON.toJSONString(condition)) : null, documentId);
		return ok(hits);
	}

	/** 切分预览（Dry-run，不落库不嵌入）：body {text, segmentMode?, segmentMaxLength?, segmentOverlap?, segmentDelimiter?} */
	@PostMapping("/dataset/preview-split")
	@ResponseBody
	public Object previewSplit(@RequestBody Map<String, Object> body) {
		String text = body.get("text") == null ? null : String.valueOf(body.get("text"));
		String mode = body.get("segmentMode") == null ? null : String.valueOf(body.get("segmentMode"));
		Integer maxLen = body.get("segmentMaxLength") == null ? null
				: Integer.valueOf(String.valueOf(body.get("segmentMaxLength")));
		Integer overlap = body.get("segmentOverlap") == null ? null
				: Integer.valueOf(String.valueOf(body.get("segmentOverlap")));
		String delimiter = body.get("segmentDelimiter") == null ? null : String.valueOf(body.get("segmentDelimiter"));
		return ok(datasetService.previewSplit(text, mode, maxLen, overlap, delimiter));
	}

	/** 导出数据集（不含向量）：JSON 附件下载。 */
	@GetMapping("/dataset/{id}/export")
	@ResponseBody
	public void exportDataset(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
		com.alibaba.fastjson2.JSONObject payload = datasetService.export(id);
		byte[] bytes = payload.toJSONString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
		response.setContentType("application/json");
		response.setHeader("Content-Disposition", "attachment; filename=\"kb-dataset-" + id + ".json\"");
		response.setContentLength(bytes.length);
		response.getOutputStream().write(bytes);
	}

	/** 导入数据集（POST 导出 JSON）：返回新数据集 id，全部文档重新入队嵌入。 */
	@PostMapping("/dataset/import")
	@ResponseBody
	public Object importDataset(@RequestBody com.alibaba.fastjson2.JSONObject payload) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("datasetId", datasetService.importDataset(payload));
		return ok(data);
	}

	/** 引用该数据集的 knowledge 类型 retriever 资源列表。 */
	@GetMapping("/dataset/{id}/retrievers")
	@ResponseBody
	public Object retrievers(@PathVariable("id") String id) {
		return ok(datasetService.referencedRetrieverResources(id));
	}

	// ---------------- 文档 ----------------

	@GetMapping("/dataset/{id}/document")
	@ResponseBody
	public Object listDocuments(@PathVariable("id") String id,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
		return ok(datasetService.listDocuments(id, page, pageSize));
	}

	@PostMapping(value = "/document/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseBody
	public Object upload(@RequestParam("datasetId") String datasetId,
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "metadata", required = false) String metadataJson) {
		return ok(documentService.createByFile(datasetId, file, parseStaticMetadataJson(metadataJson)));
	}

	/** 文档详情。 */
	@GetMapping("/document/{id}")
	@ResponseBody
	public Object getDocument(@PathVariable("id") String id) {
		return ok(documentService.get(id));
	}

	/** 分段详情。 */
	@GetMapping("/segment/{id}")
	@ResponseBody
	public Object getSegment(@PathVariable("id") String id) {
		return ok(segmentService.getSegment(id));
	}

	@PostMapping("/document/{id}/reindex")
	@ResponseBody
	public Object reindex(@PathVariable("id") String id) {
		return ok(documentService.reindex(id));
	}

	@RequestMapping(value = "/document/{id}/enabled", method = {RequestMethod.PATCH, RequestMethod.POST})
	@ResponseBody
	public Object patchEnabled(@PathVariable("id") String id, @RequestParam("value") boolean value) {
		return ok(documentService.patchEnabled(id, value));
	}

	@RequestMapping(value = "/document/{id}/archived", method = {RequestMethod.PATCH, RequestMethod.POST})
	@ResponseBody
	public Object patchArchived(@PathVariable("id") String id, @RequestParam("value") boolean value) {
		return ok(documentService.patchArchived(id, value));
	}

	/** 批量操作：body {documentIds, action}（enable/disable/archive/unarchive/delete）。 */
	@PostMapping("/dataset/{id}/document/batch")
	@ResponseBody
	@SuppressWarnings("unchecked")
	public Object batch(@PathVariable("id") String id, @RequestBody Map<String, Object> body) {
		Object ids = body.get("documentIds");
		String action = body.get("action") == null ? null : String.valueOf(body.get("action"));
		if (!(ids instanceof List)) {
			throw new IllegalArgumentException("documentIds 必须为数组");
		}
		return ok(documentService.batch((List<String>) ids, action));
	}

	@GetMapping("/document/{id}/download")
	@ResponseBody
	public void download(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
		KbDocument document = documentService.get(id);
		byte[] bytes = documentService.download(id);
		response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
		String fileName = document.getName() == null ? "download" : document.getName();
		response.setHeader("Content-Disposition", "attachment; filename=\""
				+ fileName.replaceAll("[\\r\\n\"]", "_") + "\"");
		response.setContentLength(bytes.length);
		response.getOutputStream().write(bytes);
	}

	@RequestMapping(value = "/document/{id}/metadata", method = {RequestMethod.PUT, RequestMethod.POST})
	@ResponseBody
	public Object setMetadata(@PathVariable("id") String id, @RequestBody Map<String, Object> metadata) {
		return ok(documentService.setMetadata(id, metadata));
	}

	// ---------------- 分段 ----------------

	/** 手动新增分段：body {content, keywords?, answer?}，position 自动追加，嵌入写入向量。 */
	@PostMapping("/document/{id}/segment")
	@ResponseBody
	public Object createSegment(@PathVariable("id") String id, @RequestBody Map<String, Object> body) {
		String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
		String keywords = body.get("keywords") == null ? null : String.valueOf(body.get("keywords"));
		String answer = body.get("answer") == null ? null : String.valueOf(body.get("answer"));
		return ok(segmentService.create(id, content, keywords, answer));
	}

	@GetMapping("/document/{id}/segment")
	@ResponseBody
	public Object listSegments(@PathVariable("id") String id,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
		return ok(segmentService.listSegments(id, page, pageSize));
	}

	@RequestMapping(value = "/segment/{id}/enabled", method = {RequestMethod.PATCH, RequestMethod.POST})
	@ResponseBody
	public Object patchSegmentEnabled(@PathVariable("id") String id, @RequestParam("value") boolean value) {
		return ok(segmentService.patchEnabled(id, value));
	}

	@RequestMapping(value = "/segment/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
	@ResponseBody
	public Object updateSegment(@PathVariable("id") String id, @RequestBody Map<String, Object> body) {
		String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
		String keywords = body.get("keywords") == null ? null : String.valueOf(body.get("keywords"));
		String answer = body.get("answer") == null ? null : String.valueOf(body.get("answer"));
		return ok(segmentService.update(id, content, keywords, answer));
	}

	@RequestMapping(value = "/segment/{id}", method = {RequestMethod.DELETE, RequestMethod.POST})
	@ResponseBody
	public Object deleteSegment(@PathVariable("id") String id) {
		segmentService.delete(id);
		return ok(null);
	}

	/** multipart 上传的 metadata JSON 字符串解析；空/非法返回 null（不入库）。 */
	@SuppressWarnings("unchecked")
	public static java.util.Map<String, Object> parseStaticMetadataJson(String metadataJson) {
		if (metadataJson == null || metadataJson.trim().isEmpty()) {
			return null;
		}
		try {
			Object parsed = com.alibaba.fastjson2.JSON.parse(metadataJson);
			return parsed instanceof Map ? (Map<String, Object>) parsed : null;
		} catch (Exception e) {
			throw new IllegalArgumentException("metadata 不是合法的 JSON 对象: " + e.getMessage());
		}
	}

	// ---------------- 统一响应 ----------------

	public static Object ok(Object data) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("code", 200);
		result.put("message", "success");
		result.put("data", data);
		return result;
	}

	public static Object error(String message) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("code", 500);
		result.put("message", message);
		return result;
	}
}
