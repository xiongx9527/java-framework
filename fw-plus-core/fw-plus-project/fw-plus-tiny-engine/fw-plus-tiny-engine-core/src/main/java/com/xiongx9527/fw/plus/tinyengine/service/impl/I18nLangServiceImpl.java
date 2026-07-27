package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.I18nLangMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nLang;
import com.xiongx9527.fw.plus.tinyengine.service.I18nLangService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class I18nLangServiceImpl extends ServiceImpl<I18nLangMapper, I18nLang> implements I18nLangService {

	@Override
	public List<I18nLang> queryAllI18nLang() {
		return baseMapper.queryAllI18nLang();
	}
}
