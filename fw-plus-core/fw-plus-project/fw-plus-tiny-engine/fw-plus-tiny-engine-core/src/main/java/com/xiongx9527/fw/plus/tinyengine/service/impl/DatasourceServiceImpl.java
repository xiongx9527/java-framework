package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
}
