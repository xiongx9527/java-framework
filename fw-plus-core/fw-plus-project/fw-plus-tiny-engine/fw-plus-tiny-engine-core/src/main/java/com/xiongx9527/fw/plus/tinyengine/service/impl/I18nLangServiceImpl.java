package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
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

	@Override
	public I18nLang queryI18nLangById(Integer id) {
		return baseMapper.queryI18nLangById(id);
	}

	@Override
	public List<I18nLang> queryI18nLangByCondition(I18nLang i18nLang) {
		return baseMapper.queryI18nLangByCondition(i18nLang);
	}

	@Override
	public Integer deleteI18nLangById(Integer id) {
		return baseMapper.deleteI18nLangById(id);
	}

	@Override
	public Integer updateI18nLangById(I18nLang i18nLang) {
		return baseMapper.updateI18nLangById(i18nLang);
	}

	@Override
	public Integer createI18nLang(I18nLang i18nLang) {
		return baseMapper.createI18nLang(i18nLang);
	}
}
