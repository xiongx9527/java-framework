package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
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

	@Override
	public AppExtension findAppExtensionById(Integer id) {
		return baseMapper.queryAppExtensionById(id);
	}

	@Override
	public List<AppExtension> findAppExtensionByCondition(AppExtension appExtension) {
		return baseMapper.queryAppExtensionByCondition(appExtension);
	}

	@Override
	public AppExtension deleteAppExtensionById(Integer id) {
		AppExtension existing = findAppExtensionById(id);
		if (existing == null || baseMapper.deleteAppExtensionById(id) != 1) {
			throw failure(ExceptionEnum.CM009);
		}
		return existing;
	}

	@Override
	public AppExtension updateAppExtensionById(AppExtension appExtension) {
		if (appExtension == null || appExtension.getId() == null
				|| baseMapper.updateAppExtensionById(appExtension) != 1) {
			throw failure(ExceptionEnum.CM001);
		}
		return findAppExtensionById(appExtension.getId());
	}

	@Override
	public AppExtension createAppExtension(AppExtension appExtension) {
		if (!baseMapper.queryAppExtensionByCondition(appExtension).isEmpty()) {
			throw failure(ExceptionEnum.CM003);
		}
		if (baseMapper.createAppExtension(appExtension) != 1) {
			throw failure(ExceptionEnum.CM001);
		}
		return appExtension;
	}

	private ServiceException failure(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}
}
