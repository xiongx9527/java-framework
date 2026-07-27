package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageTemplate;

import java.util.List;

public interface PageTemplateService extends IService<PageTemplate> {

	List<PageTemplate> queryAllPageTemplate(String name, String type);
}
