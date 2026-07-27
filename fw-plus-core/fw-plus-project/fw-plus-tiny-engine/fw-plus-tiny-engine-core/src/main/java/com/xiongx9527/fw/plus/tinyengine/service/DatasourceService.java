package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Datasource;

import java.util.List;

public interface DatasourceService extends IService<Datasource> {

	List<Datasource> queryAllDatasource();
}
