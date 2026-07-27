package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageTemplateMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageTemplate;
import com.xiongx9527.fw.plus.tinyengine.service.PageTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PageTemplateServiceImpl extends ServiceImpl<PageTemplateMapper, PageTemplate> implements PageTemplateService {

	@Override
	public List<PageTemplate> queryAllPageTemplate(String name, String type) {
		return name == null ? baseMapper.queryAllPageTemplate(type) : baseMapper.queryPageTemplateByName(name, type);
	}
}
