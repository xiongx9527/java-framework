package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.I18nEntryMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nEntry;
import com.xiongx9527.fw.plus.tinyengine.service.I18nEntryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class I18nEntryServiceImpl extends ServiceImpl<I18nEntryMapper, I18nEntry> implements I18nEntryService {

	@Override
	public List<I18nEntryDto> queryAllI18nEntry() {
		return baseMapper.queryAllI18nEntry();
	}
}
