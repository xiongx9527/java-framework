package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppExtensionMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AppExtension;
import com.xiongx9527.fw.plus.tinyengine.service.AppExtensionService;
import com.xiongx9527.fw.plus.tinyengine.service.AppRoleService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppPermission;
import org.springframework.beans.factory.annotation.Autowired;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppExtensionServiceImpl extends MapperServiceSupport<AppExtensionMapper> implements AppExtensionService {

	@Autowired
	private TinyEngineUserContext userContext;
	@Autowired
	private AppMapper appMapper;
	@Autowired
	private AppRoleService appRoleService;

	public AppExtensionServiceImpl() {
	}

	public AppExtensionServiceImpl(TinyEngineUserContext userContext, AppMapper appMapper) {
		this.userContext = userContext;
		this.appMapper = appMapper;
	}

	@Override
	public List<AppExtension> findAllAppExtension() {
		List<AppExtension> extensions = baseMapper.queryAllAppExtension();
		requireAll(extensions, AppPermission.READ);
		return extensions;
	}

	@Override
	public AppExtension findAppExtensionById(Integer id) {
		AppExtension extension = baseMapper.queryAppExtensionById(id);
		require(extension == null ? null : extension.getApp(), AppPermission.READ);
		return extension;
	}

	@Override
	public List<AppExtension> findAppExtensionByCondition(AppExtension appExtension) {
		applyAppScope(appExtension, false);
		require(appExtension == null ? null : appExtension.getApp(), AppPermission.READ);
		return baseMapper.queryAppExtensionByCondition(appExtension);
	}

	@Override
	public AppExtension deleteAppExtensionById(Integer id) {
		AppExtension existing = findAppExtensionById(id);
		require(existing == null ? null : existing.getApp(), AppPermission.MANAGE);
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
		require(existing.getApp(), AppPermission.MANAGE);
		appExtension.setApp(existing.getApp());
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
		require(appExtension == null ? null : appExtension.getApp(), AppPermission.MANAGE);
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

	private void require(Integer appId, AppPermission permission) {
		if (appRoleService != null) {
			appRoleService.requirePermission(appId, permission);
		}
	}

	private void requireAll(List<AppExtension> extensions, AppPermission permission) {
		if (extensions == null) {
			return;
		}
		for (AppExtension extension : extensions) {
			require(extension == null ? null : extension.getApp(), permission);
		}
	}
}
