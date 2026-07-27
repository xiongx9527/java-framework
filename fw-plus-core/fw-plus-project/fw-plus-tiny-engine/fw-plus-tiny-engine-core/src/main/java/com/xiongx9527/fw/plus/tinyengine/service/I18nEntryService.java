package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nEntry;

import java.util.List;

public interface I18nEntryService extends IService<I18nEntry> {

	List<I18nEntryDto> queryAllI18nEntry();
}
