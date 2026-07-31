package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.DatasourceMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Datasource;
import com.xiongx9527.fw.plus.tinyengine.service.DatasourceService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatasourceServiceImpl extends ServiceImpl<DatasourceMapper, Datasource> implements DatasourceService {

	@Override
	public List<Datasource> queryAllDatasource() {
		return baseMapper.queryAllDatasource();
	}

	@Override
	public Datasource queryDatasourceById(Integer id) {
		return baseMapper.queryDatasourceById(id);
	}

	@Override
	public List<Datasource> queryDatasourceByCondition(Datasource datasource) {
		return baseMapper.queryDatasourceByCondition(datasource);
	}

	@Override
	public Datasource deleteDatasourceById(Integer id) {
		Datasource datasource = queryDatasourceById(id);
		if (datasource == null || baseMapper.deleteDatasourceById(id) != 1) {
			throw failure(ExceptionEnum.CM009);
		}
		return datasource;
	}

	@Override
	public Datasource updateDatasourceById(Datasource datasource) {
		if (datasource == null || datasource.getId() == null || baseMapper.updateDatasourceById(datasource) != 1) {
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
		if (baseMapper.createDatasource(datasource) != 1) {
			throw failure(ExceptionEnum.CM001);
		}
		return queryDatasourceById(datasource.getId());
	}

	private ServiceException failure(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}
}
