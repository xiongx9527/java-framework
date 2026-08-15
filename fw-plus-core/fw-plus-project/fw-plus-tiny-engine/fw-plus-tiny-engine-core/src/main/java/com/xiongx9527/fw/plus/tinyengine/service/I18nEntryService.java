package com.xiongx9527.fw.plus.tinyengine.service;

import com.xiongx9527.fw.plus.tinyengine.model.dto.DeleteI18nEntry;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryListResult;
import com.xiongx9527.fw.plus.tinyengine.model.dto.OperateI18nBatchEntries;
import com.xiongx9527.fw.plus.tinyengine.model.dto.OperateI18nEntries;
import com.xiongx9527.fw.plus.tinyengine.model.dto.SchemaI18n;
import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nEntry;

import java.util.List;

public interface I18nEntryService {

	I18nEntryListResult findAllI18nEntry();

	I18nEntryDto findI18nEntryById(Integer id);

	List<I18nEntryDto> findI18nEntryByCondition(I18nEntry i18nEntry);

	Integer updateI18nEntryById(I18nEntry i18nEntry);

	SchemaI18n formatEntriesList(List<I18nEntryDto> i18nEntries);

	List<I18nEntry> create(OperateI18nEntries entries);

	List<I18nEntry> bulkCreate(OperateI18nBatchEntries entries);

	List<I18nEntry> bulkUpdate(OperateI18nEntries entries);

	List<I18nEntryDto> deleteI18nEntriesByHostAndHostTypeAndKey(DeleteI18nEntry entry);

	I18nEntryListResult findI18nEntryByApp(Integer host, String hostType);
}
