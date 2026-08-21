package com.xiongx9527.fw.plus.tinyengine.service;

import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;

public interface AppTemplateService {

	AppDto queryAllAppTemplate(Integer currentPage, Integer pageSize, String orderBy, App app);

	App queryAppTemplateById(Integer id);

	App createAppByTemplate(App app);

	Integer updateAppTemplateById(App app);
}
