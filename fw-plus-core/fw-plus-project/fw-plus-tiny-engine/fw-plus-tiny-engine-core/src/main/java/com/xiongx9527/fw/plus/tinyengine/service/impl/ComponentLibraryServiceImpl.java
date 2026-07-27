package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
}
