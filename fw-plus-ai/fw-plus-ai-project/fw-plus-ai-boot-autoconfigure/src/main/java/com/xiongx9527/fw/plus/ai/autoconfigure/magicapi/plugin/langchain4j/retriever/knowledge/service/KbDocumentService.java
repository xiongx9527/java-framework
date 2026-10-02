package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDataset;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDocumentMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline.KbTokenCounter;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.store.KbOriginalFileStore;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 知识库文档服务（设计 §3.1/§3.3）：上传/文本入库（waiting + 即时触发）、重建索引、启禁/归档、
 * 批量操作、下载、metadata 设置、删除（级联分段/向量/原始文件）。
 */
public class KbDocumentService {

	private final KbDocumentMapper documentMapper;

	private final com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDatasetMapper datasetMapper;

	private final KbIndexingService indexingService;

	private final KbOriginalFileStore fileStore;

	private final KbKnowledgeState state;

	public KbDocumentService(KbDocumentMapper documentMapper,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDatasetMapper datasetMapper,
			KbIndexingService indexingService, KbOriginalFileStore fileStore, KbKnowledgeState state) {
		this.documentMapper = documentMapper;
		this.datasetMapper = datasetMapper;
		this.indexingService = indexingService;
		this.fileStore = fileStore;
		this.state = state;
	}

	/**
	 * 文档跨数据集迁移：更新归属后触发重建索引（目标数据集的嵌入模型/向量库可能不同，
	 * 旧向量按原数据集删除，重新走 waiting → 嵌入写入目标数据集的 store）。
	 */
	public KbDocument move(String documentId, String targetDatasetId) {
		state.checkReady();
		KbDocument document = requireDocument(documentId);
		if (targetDatasetId == null || targetDatasetId.isEmpty()
				|| targetDatasetId.equals(document.getDatasetId())) {
			throw new IllegalArgumentException("目标数据集无效或与当前数据集相同");
		}
		KbDataset target = datasetMapper.selectById(targetDatasetId);
		if (target == null) {
			throw new IllegalArgumentException("目标数据集不存在: " + targetDatasetId);
		}
		java.util.Date now = new java.util.Date();
		// doc_hash 数据集内去重
		Integer existing = documentMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<KbDocument>()
				.eq("dataset_id", targetDatasetId)
				.eq("doc_hash", document.getDocHash()));
		if (existing != null && existing > 0) {
			throw new IllegalArgumentException("目标数据集已存在相同内容的文档（doc_hash 去重）");
		}
		com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<KbDocument> wrapper =
				new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<KbDocument>()
						.eq("id", documentId)
						.set("dataset_id", targetDatasetId)
						.set("indexing_status", "waiting")
						.set("error_msg", null)
						.set("update_time", now);
		documentMapper.update(null, wrapper);
		document.setDatasetId(targetDatasetId);
		indexingService.reindex(documentId);
		return documentMapper.selectById(documentId);
	}

	/** 文件上传入库：存原文 → 置 waiting → 即时触发异步索引。 */
	public KbDocument createByFile(String datasetId, MultipartFile file) {
		state.checkReady();
		try {
			String fileName = file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename();
			InputStream in = file.getInputStream();
			return createInternal(datasetId, fileName, "upload", in, file.getSize(), null);
		} catch (java.io.IOException e) {
			throw new IllegalStateException("读取上传文件失败: " + e.getMessage(), e);
		}
	}

	/** 文本入库（create-by-text）：同 waiting + 异步索引。 */
	public KbDocument createByText(String datasetId, String name, String text) {
		state.checkReady();
		if (text == null || text.isEmpty()) {
			throw new IllegalArgumentException("text 不能为空");
		}
		byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		return createInternal(datasetId, name == null || name.isEmpty() ? "create-by-text.txt" : name,
				"create-by-text", new java.io.ByteArrayInputStream(bytes), (long) bytes.length, text);
	}

	private KbDocument createInternal(String datasetId, String fileName, String sourceType,
			InputStream content, long size, String text) {
		if (datasetId == null || datasetId.isEmpty()) {
			throw new IllegalArgumentException("数据集 id 不能为空");
		}
		Date now = new Date();
		String documentId = UUID.randomUUID().toString().replace("-", "");
		// 先读出字节以便计算 doc_hash（数据集内去重）
		byte[] bytes = readAll(content);
		String hash = sha256Hex(bytes);
		Integer existing = documentMapper.selectCount(new QueryWrapper<KbDocument>()
				.eq("dataset_id", datasetId)
				.eq("doc_hash", hash));
		if (existing != null && existing > 0) {
			throw new IllegalArgumentException("该数据集下已存在相同内容的文档（doc_hash 去重）");
		}
		// 原始文件落盘（文件名用 document id 防冲突），路径写回 file_path
		String path = text != null
				? fileStore.saveText(documentId, fileName, text)
				: fileStore.save(documentId, fileName, new java.io.ByteArrayInputStream(bytes));

		KbDocument document = new KbDocument();
		document.setId(documentId);
		document.setDatasetId(datasetId);
		document.setName(fileName);
		document.setFilePath(path);
		document.setFileExt(extensionOf(fileName));
		document.setFileSize((long) bytes.length);
		document.setDataSourceType(sourceType);
		document.setWordCount(text != null ? approxWordCount(text) : null);
		document.setIndexingStatus("waiting");
		document.setEnabled("1");
		document.setArchived("0");
		document.setDocHash(hash);
		document.setCreateTime(now);
		document.setUpdateTime(now);
		documentMapper.insert(document);
		indexingService.trigger(document);
		return document;
	}

	private byte[] readAll(InputStream in) {
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int read;
		try {
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			in.close();
		} catch (java.io.IOException e) {
			throw new IllegalStateException("读取文件内容失败", e);
		}
		return out.toByteArray();
	}

	/** 文档详情（对外 API 状态查询复用）。 */
	public KbDocument get(String documentId) {
		state.checkReady();
		return requireDocument(documentId);
	}

	/** 重建索引：删旧分段与向量后回读原文重新入队（设计 §2.3）。 */
	public KbDocument reindex(String documentId) {
		state.checkReady();
		indexingService.reindex(documentId);
		return documentMapper.selectById(documentId);
	}

	/** 启禁用（禁用不参与检索）。 */
	public KbDocument patchEnabled(String documentId, boolean enabled) {
		return patchFlag(documentId, "enabled", enabled ? "1" : "0");
	}

	/** 归档/取消归档（与启禁用独立，归档不参与检索）。 */
	public KbDocument patchArchived(String documentId, boolean archived) {
		return patchFlag(documentId, "archived", archived ? "1" : "0");
	}

	private KbDocument patchFlag(String documentId, String field, String value) {
		state.checkReady();
		KbDocument existing = documentMapper.selectById(documentId);
		if (existing == null) {
			throw new IllegalArgumentException("文档不存在: " + documentId);
		}
		KbDocument update = new KbDocument();
		update.setId(documentId);
		if ("enabled".equals(field)) {
			update.setEnabled(value);
		} else {
			update.setArchived(value);
		}
		update.setUpdateTime(new Date());
		documentMapper.updateById(update);
		return documentMapper.selectById(documentId);
	}

	/**
	 * 批量操作（设计 §3.1）：documentIds + action（enable/disable/archive/unarchive/delete），
	 * 逐个执行并返回逐项结果 {id, ok, message}。
	 */
	public List<Map<String, Object>> batch(List<String> documentIds, String action) {
		state.checkReady();
		if (action == null || action.isEmpty()) {
			throw new IllegalArgumentException("action 不能为空");
		}
		List<Map<String, Object>> results = new ArrayList<>();
		for (String id : documentIds == null ? new ArrayList<String>() : documentIds) {
			Map<String, Object> result = new LinkedHashMap<>();
			result.put("id", id);
			try {
				if ("enable".equals(action)) {
					patchEnabled(id, true);
				} else if ("disable".equals(action)) {
					patchEnabled(id, false);
				} else if ("archive".equals(action)) {
					patchArchived(id, true);
				} else if ("unarchive".equals(action)) {
					patchArchived(id, false);
				} else if ("delete".equals(action)) {
					deleteDocument(id);
				} else {
					throw new IllegalArgumentException("不支持的批量操作: " + action);
				}
				result.put("ok", true);
			} catch (Exception e) {
				result.put("ok", false);
				result.put("message", e.getMessage());
			}
			results.add(result);
		}
		return results;
	}

	/** 下载原始文档字节流。 */
	public byte[] download(String documentId) {
		state.checkReady();
		KbDocument document = requireDocument(documentId);
		if (document.getFilePath() == null || document.getFilePath().isEmpty()) {
			throw new IllegalStateException("该文档没有原始文件（create-by-text 且未落盘）");
		}
		try (InputStream in = fileStore.read(document.getFilePath())) {
			return readAll(in);
		} catch (java.io.IOException e) {
			throw new IllegalStateException("下载原始文档失败: " + e.getMessage(), e);
		}
	}

	/** 设置文档 metadata 键值对（JSON 持久化，检索过滤/向量 metadata 同步用）。 */
	public KbDocument setMetadata(String documentId, Map<String, Object> metadata) {
		state.checkReady();
		requireDocument(documentId);
		KbDocument update = new KbDocument();
		update.setId(documentId);
		update.setMetadata(metadata == null ? "{}" : JSON.toJSONString(metadata));
		update.setUpdateTime(new Date());
		documentMapper.updateById(update);
		return documentMapper.selectById(documentId);
	}

	/** 删除文档：级联删除分段行与向量、原始文件。 */
	public void deleteDocument(String documentId) {
		state.checkReady();
		KbDocument document = requireDocument(documentId);
		indexingService.deleteSegmentsAndVectors(documentId);
		if (document.getFilePath() != null && !document.getFilePath().isEmpty()) {
			fileStore.delete(document.getFilePath());
		}
		documentMapper.deleteById(documentId);
	}

	private KbDocument requireDocument(String documentId) {
		KbDocument document = documentMapper.selectById(documentId);
		if (document == null) {
			throw new IllegalArgumentException("文档不存在: " + documentId);
		}
		return document;
	}

	private static String sha256Hex(byte[] bytes) {
		try {
			java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(bytes);
			StringBuilder builder = new StringBuilder();
			for (byte b : hash) {
				builder.append(String.format("%02x", b));
			}
			return builder.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 不可用", e);
		}
	}

	private static String extensionOf(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			return null;
		}
		int dot = fileName.lastIndexOf('.');
		return dot >= 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1) : null;
	}

	private static int approxWordCount(String text) {
		if (text == null || text.isEmpty()) {
			return 0;
		}
		return KbTokenCounter.estimate(text);
	}
}
