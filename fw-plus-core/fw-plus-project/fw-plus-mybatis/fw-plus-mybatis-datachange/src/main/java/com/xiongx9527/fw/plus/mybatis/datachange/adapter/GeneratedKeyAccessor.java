package com.xiongx9527.fw.plus.mybatis.datachange.adapter;

import java.util.List;
import java.util.Map;

public interface GeneratedKeyAccessor {
	Map<String, Object> readKeyValues(Object parameterObject, List<String> keyColumns);
}
