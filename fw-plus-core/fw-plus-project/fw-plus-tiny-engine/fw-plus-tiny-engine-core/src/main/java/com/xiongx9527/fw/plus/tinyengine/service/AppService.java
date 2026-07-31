package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.SchemaI18n;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;

import java.util.List;

public interface AppService extends IService<App> {

	List<App> queryAllApp();
	AppDto queryAllAppByPage(Integer currentPage, Integer pageSize, String orderBy, App app);
	App queryAppById(Integer id);
	List<App> queryAppByCondition(App app);
	App deleteAppById(Integer id);
	App updateAppById(App app);
	App createApp(App app);
	SchemaI18n formatI18nEntrites(List<I18nEntryDto> i18nEntries, Integer usedIn, Integer id);
}
