package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;

public interface DataChangeDialect {
	boolean supports(DatabaseMetadata metadata);
	TableIdentifier normalize(TableIdentifier table);
	int getOrder();
}
