package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.ComponentLibraryMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ComponentLibrary;
import com.xiongx9527.fw.plus.tinyengine.service.ComponentLibraryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComponentLibraryServiceImpl extends ServiceImpl<ComponentLibraryMapper, ComponentLibrary> implements ComponentLibraryService {

	@Override
	public List<ComponentLibrary> queryAllComponentLibrary() {
		return baseMapper.queryAllComponentLibrary();
	}

	@Override
	public ComponentLibrary queryComponentLibraryById(Integer id) {
		return baseMapper.queryComponentLibraryById(id);
	}

	@Override
	public List<ComponentLibrary> queryComponentLibraryByCondition(ComponentLibrary componentLibrary) {
		return baseMapper.queryComponentLibraryByCondition(componentLibrary);
	}

	@Override
	public ComponentLibrary deleteComponentLibraryById(Integer id) {
		ComponentLibrary existing = queryComponentLibraryById(id);
		if (baseMapper.deleteComponentLibraryById(id) != 1) {
			throw failure();
		}
		return existing;
	}

	@Override
	public ComponentLibrary updateComponentLibraryById(ComponentLibrary componentLibrary) {
		if (componentLibrary == null || componentLibrary.getId() == null
				|| baseMapper.updateComponentLibraryById(componentLibrary) != 1) {
			throw failure();
		}
		return queryComponentLibraryById(componentLibrary.getId());
	}

	@Override
	public ComponentLibrary createComponentLibrary(ComponentLibrary componentLibrary) {
		if (componentLibrary == null || baseMapper.createComponentLibrary(componentLibrary) != 1) {
			throw failure();
		}
		return componentLibrary;
	}

	private ServiceException failure() {
		return new ServiceException(ExceptionEnum.CM008.getResultCode(), ExceptionEnum.CM008.getResultMsg());
	}
}
