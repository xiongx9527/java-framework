package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageTemplateMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageTemplate;
import com.xiongx9527.fw.plus.tinyengine.service.PageTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PageTemplateServiceImpl extends ServiceImpl<PageTemplateMapper, PageTemplate> implements PageTemplateService {

	@Override
	public List<PageTemplate> queryAllPageTemplate(String name, String type) {
		return name == null || name.isEmpty()
				? baseMapper.queryAllPageTemplate(type)
				: baseMapper.queryPageTemplateByName(name, type);
	}

	@Override
	public PageTemplate queryPageTemplateById(Integer id) {
		return baseMapper.queryPageTemplateById(id);
	}

	@Override
	public List<PageTemplate> queryPageTemplateByCondition(PageTemplate pageTemplate) {
		return baseMapper.queryPageTemplateByCondition(pageTemplate);
	}

	@Override
	public Integer deletePageTemplateByIds(List<Integer> ids) {
		if (ids == null || ids.isEmpty()) {
			throw failure(ExceptionEnum.CM002);
		}
		Integer deleted = baseMapper.deletePageTemplateByIds(ids);
		if (deleted == null || deleted.intValue() != ids.size()) {
			throw failure(ExceptionEnum.CM001);
		}
		return deleted;
	}

	@Override
	public Integer updatePageTemplateById(PageTemplate pageTemplate) {
		return baseMapper.updatePageTemplateById(pageTemplate);
	}

	@Override
	public PageTemplate createPageTemplate(PageTemplate pageTemplate) {
		PageTemplate condition = new PageTemplate();
		condition.setName(pageTemplate.getName());
		if (!baseMapper.queryPageTemplateByCondition(condition).isEmpty()) {
			throw failure(ExceptionEnum.CM003);
		}
		if (baseMapper.createPageTemplate(pageTemplate) != 1) {
			throw failure(ExceptionEnum.CM001);
		}
		return queryPageTemplateById(pageTemplate.getId());
	}

	private ServiceException failure(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}
}
