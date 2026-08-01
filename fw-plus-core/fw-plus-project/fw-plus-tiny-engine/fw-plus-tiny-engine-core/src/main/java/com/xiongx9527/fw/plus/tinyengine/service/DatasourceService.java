package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Datasource;

import java.util.List;

public interface DatasourceService extends IService<Datasource> {

	List<Datasource> queryAllDatasource();
	Datasource queryDatasourceById(Integer id);
	List<Datasource> queryDatasourceByCondition(Datasource datasource);
	Datasource deleteDatasourceById(Integer id);
	Datasource updateDatasourceById(Datasource datasource);
	Datasource createDatasource(Datasource datasource);
}
