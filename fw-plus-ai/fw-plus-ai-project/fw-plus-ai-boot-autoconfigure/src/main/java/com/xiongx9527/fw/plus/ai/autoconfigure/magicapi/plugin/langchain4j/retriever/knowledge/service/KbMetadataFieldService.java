package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbMetadataField;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbDocumentMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.mapper.KbMetadataFieldMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 元数据字段管理（对齐 Dify 元数据字段管理）：数据集级字段定义（name/type/description），
 * 文档 metadata 赋值前需先定义字段；字段删除时同步清理各文档 metadata 中的该键。
 */
public class KbMetadataFieldService {

	private static final Logger logger = LoggerFactory.getLogger(KbMetadataFieldService.class);

	private final KbMetadataFieldMapper fieldMapper;

	private final KbDocumentMapper documentMapper;

	private final KbKnowledgeState state;

	public KbMetadataFieldService(KbMetadataFieldMapper fieldMapper, KbDocumentMapper documentMapper,
			KbKnowledgeState state) {
		this.fieldMapper = fieldMapper;
		this.documentMapper = documentMapper;
		this.state = state;
	}

	/** 数据集的字段定义列表。 */
	public List<KbMetadataField> list(String datasetId) {
		state.checkReady();
		return fieldMapper.selectList(new QueryWrapper<KbMetadataField>()
				.eq("dataset_id", datasetId)
				.orderByAsc("name"));
	}

	/** 新增字段定义：数据集内字段名唯一。 */
	public KbMetadataField create(String datasetId, String name, String type, String description) {
		state.checkReady();
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("字段名不能为空");
		}
		String normalizedType = type == null || type.isEmpty() ? "string" : type;
		if (!"string".equals(normalizedType) && !"number".equals(normalizedType) && !"time".equals(normalizedType)) {
			throw new IllegalArgumentException("不支持的元数据字段类型: " + normalizedType + "（string / number / time）");
		}
		Integer exists = fieldMapper.selectCount(new QueryWrapper<KbMetadataField>()
				.eq("dataset_id", datasetId)
				.eq("name", name.trim()));
		if (exists != null && exists > 0) {
			throw new IllegalArgumentException("该数据集下已存在同名元数据字段: " + name);
		}
		KbMetadataField field = new KbMetadataField();
		field.setId(UUID.randomUUID().toString().replace("-", ""));
		field.setDatasetId(datasetId);
		field.setName(name.trim());
		field.setType(normalizedType);
		field.setDescription(description);
		Date now = new Date();
		field.setCreateTime(now);
		field.setUpdateTime(now);
		fieldMapper.insert(field);
		return field;
	}

	/** 更新字段定义（type/description；字段名创建后不可改，保证文档 metadata 键稳定）。 */
	public KbMetadataField update(String fieldId, String type, String description) {
		state.checkReady();
		KbMetadataField field = fieldMapper.selectById(fieldId);
		if (field == null) {
			throw new IllegalArgumentException("元数据字段定义不存在: " + fieldId);
		}
		if (type != null && !type.isEmpty() && !"string".equals(type) && !"number".equals(type)
				&& !"time".equals(type)) {
			throw new IllegalArgumentException("不支持的元数据字段类型: " + type);
		}
		KbMetadataField update = new KbMetadataField();
		update.setId(fieldId);
		update.setType(type != null && !type.isEmpty() ? type : field.getType());
		update.setDescription(description != null ? description : field.getDescription());
		update.setUpdateTime(new Date());
		fieldMapper.updateById(update);
		return fieldMapper.selectById(fieldId);
	}

	/**
	 * 删除字段定义：同步从该数据集全部文档 metadata 中移除该键（文档 metadata 变更后，
	 * 向量快照在重建索引后同步）。
	 */
	public void delete(String fieldId) {
		state.checkReady();
		KbMetadataField field = fieldMapper.selectById(fieldId);
		if (field == null) {
			throw new IllegalArgumentException("元数据字段定义不存在: " + fieldId);
		}
		int cleaned = 0;
		for (KbDocument document : documentMapper.selectList(new QueryWrapper<KbDocument>()
				.eq("dataset_id", field.getDatasetId()))) {
			if (document.getMetadata() == null || document.getMetadata().isEmpty()
					|| "{}".equals(document.getMetadata())) {
				continue;
			}
			try {
				Map<String, Object> metadata = JSON.parseObject(document.getMetadata());
				if (metadata != null && metadata.containsKey(field.getName())) {
					metadata.remove(field.getName());
					documentMapper.update(null, new UpdateWrapper<KbDocument>()
							.eq("id", document.getId())
							.set("metadata", JSON.toJSONString(metadata)));
					cleaned++;
				}
			} catch (Exception e) {
				logger.warn("清理文档元数据键失败（documentId={}）: {}", document.getId(), e.getMessage());
			}
		}
		logger.info("删除元数据字段 {}：已从 {} 个文档清理该键", field.getName(), cleaned);
		fieldMapper.deleteById(fieldId);
	}

	/**
	 * 校验文档 metadata：键必须已定义 + 值符合字段类型（数据集存在字段定义时严格校验；
	 * 数据集尚未定义任何字段时放行，向后兼容既有用法）。
	 *
	 * <ul>
	 *   <li>string：任意值；</li>
	 *   <li>number：值必须可解析为数字；</li>
	 *   <li>time：值必须为 {@code yyyy-MM-dd} 或 {@code yyyy-MM-dd HH:mm:ss} 格式。</li>
	 * </ul>
	 */
	public void validate(String datasetId, Map<String, Object> metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return;
		}
		List<KbMetadataField> fields = fieldMapper.selectList(new QueryWrapper<KbMetadataField>()
				.eq("dataset_id", datasetId));
		if (fields.isEmpty()) {
			return;
		}
		Map<String, KbMetadataField> defined = new LinkedHashMap<>();
		for (KbMetadataField field : fields) {
			defined.put(field.getName(), field);
		}
		for (Map.Entry<String, Object> entry : metadata.entrySet()) {
			String key = entry.getKey();
			KbMetadataField field = defined.get(key);
			if (field == null) {
				throw new IllegalArgumentException("未定义的元数据字段: " + key
						+ "（请先在数据集下定义元数据字段，已定义: " + defined.keySet() + "）");
			}
			validateValue(field, entry.getValue());
		}
	}

	private void validateValue(KbMetadataField field, Object value) {
		if (value == null || String.valueOf(value).isEmpty()) {
			return;
		}
		String text = String.valueOf(value).trim();
		if ("number".equals(field.getType())) {
			try {
				Double.parseDouble(text);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("元数据字段 " + field.getName()
						+ " 的值必须是数字: " + text);
			}
		} else if ("time".equals(field.getType())) {
			if (!isTimeValue(text)) {
				throw new IllegalArgumentException("元数据字段 " + field.getName()
						+ " 的值必须是时间格式（yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）: " + text);
			}
		}
	}

	private static boolean isTimeValue(String text) {
		try {
			java.time.LocalDate.parse(text);
			return true;
		} catch (Exception ignored) {
			// 尝试日期时间格式
		}
		try {
			java.time.LocalDateTime.parse(text,
					java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}
}
