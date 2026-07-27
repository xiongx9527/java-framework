package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.I18nEntryMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.I18nLangMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.DeleteI18nEntry;
import com.xiongx9527.fw.plus.tinyengine.model.dto.Entry;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryListResult;
import com.xiongx9527.fw.plus.tinyengine.model.dto.OperateI18nBatchEntries;
import com.xiongx9527.fw.plus.tinyengine.model.dto.OperateI18nEntries;
import com.xiongx9527.fw.plus.tinyengine.model.dto.SchemaI18n;
import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nEntry;
import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nLang;
import com.xiongx9527.fw.plus.tinyengine.service.I18nEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class I18nEntryServiceImpl extends ServiceImpl<I18nEntryMapper, I18nEntry> implements I18nEntryService {

	@Autowired
	private I18nLangMapper i18nLangMapper;

	@Override
	public I18nEntryListResult findAllI18nEntry() {
		return buildResult(baseMapper.queryAllI18nEntry());
	}

	@Override
	public I18nEntryDto findI18nEntryById(Integer id) {
		return baseMapper.queryI18nEntryById(id);
	}

	@Override
	public List<I18nEntryDto> findI18nEntryByCondition(I18nEntry i18nEntry) {
		return baseMapper.queryI18nEntryByCondition(i18nEntry);
	}

	@Override
	public Integer updateI18nEntryById(I18nEntry i18nEntry) {
		return baseMapper.updateI18nEntryById(i18nEntry);
	}

	@Override
	public SchemaI18n formatEntriesList(List<I18nEntryDto> entries) {
		Map<String, String> chinese = new HashMap<String, String>();
		Map<String, String> english = new HashMap<String, String>();
		if (entries != null) {
			for (I18nEntryDto entry : entries) {
				if (entry.getLang() == null) {
					continue;
				}
				if ("zh_CN".equals(entry.getLang().getLang())) {
					chinese.put(entry.getKey(), entry.getContent());
				}
				else if ("en_US".equals(entry.getLang().getLang())) {
					english.put(entry.getKey(), entry.getContent());
				}
			}
		}
		SchemaI18n result = new SchemaI18n();
		result.setZhCn(chinese);
		result.setEnUs(english);
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public List<I18nEntry> create(OperateI18nEntries entries) {
		List<I18nEntry> values = fillEntries(entries, languageIds());
		for (I18nEntry value : values) {
			baseMapper.createI18nEntry(value);
		}
		return values;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public List<I18nEntry> bulkCreate(OperateI18nBatchEntries entries) {
		List<I18nEntry> values = new ArrayList<I18nEntry>();
		Map<String, Integer> languageIds = languageIds();
		if (entries != null && entries.getEntries() != null) {
			for (Entry entry : entries.getEntries()) {
				OperateI18nEntries operation = new OperateI18nEntries();
				operation.setHost(entries.getHost());
				operation.setHostType(entries.getHostType());
				operation.setKey(entry.getKey());
				operation.setContents(entry.getContents());
				values.addAll(fillEntries(operation, languageIds));
			}
		}
		for (I18nEntry value : values) {
			baseMapper.createI18nEntry(value);
		}
		return values;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public List<I18nEntry> bulkUpdate(OperateI18nEntries entries) {
		if ("app".equals(entries.getHostType())) {
			I18nEntry condition = new I18nEntry();
			condition.setHostType(entries.getHostType());
			condition.setHost(Integer.valueOf(entries.getHost()));
			condition.setKey(entries.getKey());
			List<I18nEntryDto> existing = baseMapper.queryI18nEntryByCondition(condition);
			if (existing == null || existing.isEmpty()) {
				return create(entries);
			}
		}
		List<I18nEntry> values = fillEntries(entries, languageIds());
		for (I18nEntry value : values) {
			baseMapper.updateByEntry(value.getContent(), value.getHost(), value.getHostType(), value.getKey(),
					value.getLang());
		}
		return values;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public List<I18nEntryDto> deleteI18nEntriesByHostAndHostTypeAndKey(DeleteI18nEntry entry) {
		List<I18nEntryDto> deleted = new ArrayList<I18nEntryDto>();
		if (entry == null || entry.getKeyIn() == null) {
			return deleted;
		}
		for (String key : entry.getKeyIn()) {
			I18nEntry condition = new I18nEntry();
			condition.setHostType(entry.getHostType());
			condition.setHost(Integer.valueOf(entry.getHost()));
			condition.setKey(key);
			List<I18nEntryDto> matches = baseMapper.queryI18nEntryByCondition(condition);
			if (matches == null) {
				continue;
			}
			for (I18nEntryDto match : matches) {
				baseMapper.deleteI18nEntryById(match.getId());
			}
			deleted.addAll(matches);
		}
		return deleted;
	}

	@Override
	public I18nEntryListResult findI18nEntryByApp(Integer host, String hostType) {
		return buildResult(baseMapper.findI18nEntriesByHostandHostType(host, hostType));
	}

	private I18nEntryListResult buildResult(List<I18nEntryDto> entries) {
		I18nEntryListResult result = new I18nEntryListResult();
		List<I18nLang> languages = getHostLangs();
		if (languages == null || languages.isEmpty()) {
			return result;
		}
		List<I18nLang> summaries = new ArrayList<I18nLang>();
		for (I18nLang language : languages) {
			summaries.add(new I18nLang(language.getLang(), language.getLabel()));
		}
		result.setI18nLangsList(summaries);
		result.setMessages(formatEntriesList(entries));
		return result;
	}

	private List<I18nLang> getHostLangs() {
		QueryWrapper<I18nLang> query = new QueryWrapper<I18nLang>();
		query.eq("lang", "zh_CN").or().eq("lang", "en_US");
		return i18nLangMapper.selectList(query);
	}

	private Map<String, Integer> languageIds() {
		Map<String, Integer> result = new HashMap<String, Integer>();
		List<I18nLang> languages = getHostLangs();
		if (languages != null) {
			for (I18nLang language : languages) {
				result.put(language.getLang(), language.getId());
			}
		}
		return result;
	}

	private List<I18nEntry> fillEntries(OperateI18nEntries operation, Map<String, Integer> languageIds) {
		List<I18nEntry> result = new ArrayList<I18nEntry>();
		if (operation == null || operation.getContents() == null) {
			return result;
		}
		for (Map.Entry<String, String> content : operation.getContents().entrySet()) {
			Integer languageId = languageIds.get(content.getKey());
			if (languageId == null || languageId == 0) {
				continue;
			}
			I18nEntry entry = new I18nEntry();
			entry.setHost(Integer.valueOf(operation.getHost()));
			entry.setHostType(operation.getHostType());
			entry.setKey(operation.getKey());
			entry.setLang(languageId);
			entry.setContent(content.getValue());
			result.add(entry);
		}
		return result;
	}
}
