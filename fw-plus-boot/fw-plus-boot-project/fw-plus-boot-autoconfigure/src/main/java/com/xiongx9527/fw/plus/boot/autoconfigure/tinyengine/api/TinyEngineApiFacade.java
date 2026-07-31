package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api;

import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;

import java.util.List;
import java.util.Map;

public interface TinyEngineApiFacade {

	AppDto appsPage(Integer currentPage, Integer pageSize, String orderBy, App app);

	App createApp(App app);

	App appDetail(Integer id);

	Map<String, Object> appSchema(Integer id);

	Page createPage(Page page);

	Page updatePage(Integer id, Page page);

	List<Page> pages(Integer appId);

	Page pageDetail(Integer id);
}
