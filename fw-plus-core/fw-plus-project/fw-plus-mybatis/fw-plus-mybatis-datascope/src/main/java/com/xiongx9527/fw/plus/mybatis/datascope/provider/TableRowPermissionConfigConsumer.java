package com.xiongx9527.fw.plus.mybatis.datascope.provider;

import java.io.InputStream;

@FunctionalInterface
public interface TableRowPermissionConfigConsumer {
	void accept(String fileName, InputStream inputStream);
}
