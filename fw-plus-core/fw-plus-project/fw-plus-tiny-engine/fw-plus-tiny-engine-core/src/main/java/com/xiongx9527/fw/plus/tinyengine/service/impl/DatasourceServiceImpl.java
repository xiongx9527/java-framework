package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.DatasourceMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Datasource;
import com.xiongx9527.fw.plus.tinyengine.service.DatasourceService;
import com.xiongx9527.fw.plus.tinyengine.service.AppRoleService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatasourceServiceImpl extends MapperServiceSupport<DatasourceMapper> implements DatasourceService {
	@Autowired
	private AppRoleService appRoleService;

	@Override
	public List<Datasource> queryAllDatasource() {
		List<Datasource> datasources = baseMapper.queryAllDatasource();
		requireAll(datasources, AppPermission.READ);
		return datasources;
	}

	@Override
	public Datasource queryDatasourceById(Integer id) {
		Datasource datasource = baseMapper.queryDatasourceById(id);
		require(datasource == null ? null : datasource.getApp(), AppPermission.READ);
		return datasource;
	}

	@Override
	public List<Datasource> queryDatasourceByCondition(Datasource datasource) {
		require(datasource == null ? null : datasource.getApp(), AppPermission.READ);
		List<Datasource> datasources = baseMapper.queryDatasourceByCondition(datasource);
		requireAll(datasources, AppPermission.READ);
		return datasources;
	}

	@Override
	public Datasource deleteDatasourceById(Integer id) {
		Datasource datasource = queryDatasourceById(id);
		require(datasource == null ? null : datasource.getApp(), AppPermission.DEVELOP);
		if (datasource == null || baseMapper.deleteDatasourceById(id) != 1) {
			throw failure(ExceptionEnum.CM009);
		}
		return datasource;
	}

	@Override
	public Datasource updateDatasourceById(Datasource datasource) {
		Datasource existing = datasource == null ? null : baseMapper.queryDatasourceById(datasource.getId());
		require(existing == null ? null : existing.getApp(), AppPermission.DEVELOP);
		if (datasource == null || datasource.getId() == null || existing == null) {
			throw failure(ExceptionEnum.CM001);
		}
		datasource.setApp(existing.getApp());
		datasource.setTenantId(existing.getTenantId());
		datasource.setRenterId(existing.getRenterId());
		datasource.setSiteId(existing.getSiteId());
		if (baseMapper.updateDatasourceById(datasource) != 1) {
			throw failure(ExceptionEnum.CM001);
		}
		return queryDatasourceById(datasource.getId());
	}

	@Override
	public Datasource createDatasource(Datasource datasource) {
		if (datasource == null || datasource.getApp() == null || datasource.getApp() == 0
				|| datasource.getName() == null || datasource.getName().isEmpty()) {
			throw failure(ExceptionEnum.CM002);
		}
		require(datasource.getApp(), AppPermission.DEVELOP);
		if (baseMapper.createDatasource(datasource) != 1) {
			throw failure(ExceptionEnum.CM001);
		}
		return queryDatasourceById(datasource.getId());
	}

	private ServiceException failure(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}

	private void require(Integer appId, AppPermission permission) {
		if (appRoleService != null) {
			appRoleService.requirePermission(appId, permission);
		}
	}

	private void requireAll(List<Datasource> datasources, AppPermission permission) {
		if (datasources == null) {
			return;
		}
		for (Datasource datasource : datasources) {
			require(datasource == null ? null : datasource.getApp(), permission);
		}
	}
}
