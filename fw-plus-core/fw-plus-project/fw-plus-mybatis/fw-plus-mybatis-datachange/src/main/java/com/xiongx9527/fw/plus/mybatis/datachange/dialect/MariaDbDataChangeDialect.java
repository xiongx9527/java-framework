package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

public final class MariaDbDataChangeDialect extends MySqlDataChangeDialect {
	@Override
	public boolean supports(DatabaseMetadata metadata) {
		return metadata != null && "mariadb".equals(metadata.getProductName());
	}
}
