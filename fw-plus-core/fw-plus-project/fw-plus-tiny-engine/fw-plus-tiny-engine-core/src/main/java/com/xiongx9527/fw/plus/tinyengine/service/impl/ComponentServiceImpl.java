package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.ComponentMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Component;
import com.xiongx9527.fw.plus.tinyengine.service.ComponentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComponentServiceImpl extends ServiceImpl<ComponentMapper, Component> implements ComponentService {

	@Override
	public List<Component> findAllComponent() {
		return baseMapper.queryAllComponent();
	}
}
