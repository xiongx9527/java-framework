package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nLang;

import java.util.List;

public interface I18nLangService extends IService<I18nLang> {

	List<I18nLang> queryAllI18nLang();
}
