package com.xiongx9527.fw.plus.tinyengine.dynamic.service;

import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicDelete;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicInsert;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicPageResult;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicQuery;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableColumn;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicUpdate;

import java.util.List;

public interface DynamicService {

	DynamicPageResult query(DynamicQuery query);

	long insert(DynamicInsert insert);

	int update(DynamicUpdate update);

	int delete(DynamicDelete delete);

	List<DynamicTableColumn> getTableStructure(String tableName);
}
