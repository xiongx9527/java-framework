package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppExtensionMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AppExtension;
import com.xiongx9527.fw.plus.tinyengine.service.AppExtensionService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppExtensionServiceImpl extends ServiceImpl<AppExtensionMapper, AppExtension> implements AppExtensionService {

	private TinyEngineUserContext userContext;
	private AppMapper appMapper;

	public AppExtensionServiceImpl() {
	}

	public AppExtensionServiceImpl(TinyEngineUserContext userContext, AppMapper appMapper) {
		this.userContext = userContext;
		this.appMapper = appMapper;
	}

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
		applyAppScope(appExtension, false);
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
		if (appExtension == null || appExtension.getId() == null) {
			throw failure(ExceptionEnum.CM001);
		}
		AppExtension existing = findAppExtensionById(appExtension.getId());
		if (existing == null) {
			throw failure(ExceptionEnum.CM009);
		}
		if (appExtension.getApp() == null) {
			appExtension.setApp(existing.getApp());
		}
		if (appExtension.getPlatformId() == null) {
			appExtension.setPlatformId(existing.getPlatformId());
		}
		if (appExtension.getCategory() == null) {
			appExtension.setCategory(existing.getCategory());
		}
		appExtension.setTenantId(existing.getTenantId());
		appExtension.setRenterId(existing.getRenterId());
		appExtension.setSiteId(existing.getSiteId());
		if (baseMapper.updateAppExtensionById(appExtension) != 1) {
			throw failure(ExceptionEnum.CM001);
		}
		return findAppExtensionById(appExtension.getId());
	}

	@Override
	public AppExtension createAppExtension(AppExtension appExtension) {
		applyAppScope(appExtension, true);
		if (!baseMapper.queryAppExtensionByCondition(appExtension).isEmpty()) {
			throw failure(ExceptionEnum.CM003);
		}
		if (baseMapper.createAppExtension(appExtension) != 1) {
			throw failure(ExceptionEnum.CM001);
		}
		return appExtension;
	}

	private void applyAppScope(AppExtension appExtension, boolean required) {
		if (appExtension == null || appExtension.getApp() == null || appMapper == null) {
			if (required && appMapper != null) {
				throw failure(ExceptionEnum.CM002);
			}
			return;
		}
		String tenantId = currentTenantId();
		App app = appMapper.queryAppById(appExtension.getApp(), tenantId);
		if (app == null) {
			if (required) {
				throw failure(ExceptionEnum.CM009);
			}
			return;
		}
		appExtension.setApp(app.getId());
		appExtension.setPlatformId(app.getPlatformId());
		appExtension.setTenantId(app.getTenantId());
		appExtension.setRenterId(app.getRenterId());
		appExtension.setSiteId(app.getSiteId());
	}

	private String currentTenantId() {
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		return user == null ? null : user.getTenantId();
	}

	private ServiceException failure(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}
}
