package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppExtensionMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AppExtension;
import com.xiongx9527.fw.plus.tinyengine.service.AppExtensionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppExtensionServiceImpl extends ServiceImpl<AppExtensionMapper, AppExtension> implements AppExtensionService {

	@Override
	public List<AppExtension> findAllAppExtension() {
		return baseMapper.queryAllAppExtension();
	}
}
