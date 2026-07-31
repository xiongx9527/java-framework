package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.PlatformMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Platform;
import com.xiongx9527.fw.plus.tinyengine.service.PlatformService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlatformServiceImpl extends ServiceImpl<PlatformMapper, Platform> implements PlatformService {

	@Override
	public List<Platform> queryAllPlatform() {
		return baseMapper.queryAllPlatform();
	}

	@Override
	public Platform queryPlatformById(Integer id) {
		return baseMapper.queryPlatformById(id);
	}

	@Override
	public List<Platform> queryPlatformByCondition(Platform platform) {
		return baseMapper.queryPlatformByCondition(platform);
	}

	@Override
	public Platform deletePlatformById(Integer id) {
		Platform platform = queryPlatformById(id);
		if (platform == null) {
			return null;
		}
		if (baseMapper.deletePlatformById(id) != 1) {
			throw failure(ExceptionEnum.CM008);
		}
		return platform;
	}

	@Override
	public Platform updatePlatformById(Platform platform) {
		if (platform == null || platform.getId() == null || baseMapper.updatePlatformById(platform) != 1) {
			throw failure(ExceptionEnum.CM008);
		}
		return queryPlatformById(platform.getId());
	}

	@Override
	public Platform createPlatform(Platform platform) {
		if (platform == null || platform.getName() == null || platform.getName().isEmpty()) {
			throw failure(ExceptionEnum.CM002);
		}
		if (baseMapper.createPlatform(platform) != 1) {
			throw failure(ExceptionEnum.CM008);
		}
		return platform;
	}

	private ServiceException failure(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}
}
