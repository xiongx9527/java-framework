package com.xiongx9527.fw.plus.tinyengine.service;

import com.xiongx9527.fw.plus.tinyengine.model.entity.Datasource;

import java.util.List;

public interface DatasourceService {

	List<Datasource> queryAllDatasource();
	Datasource queryDatasourceById(Integer id);
	List<Datasource> queryDatasourceByCondition(Datasource datasource);
	Datasource deleteDatasourceById(Integer id);
	Datasource updateDatasourceById(Datasource datasource);
	Datasource createDatasource(Datasource datasource);
}
