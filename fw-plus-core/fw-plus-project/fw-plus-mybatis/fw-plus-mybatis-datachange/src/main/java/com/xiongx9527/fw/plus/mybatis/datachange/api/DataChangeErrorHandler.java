package com.xiongx9527.fw.plus.mybatis.datachange.api;

public interface DataChangeErrorHandler {
	void handle(DataChangeFailure failure) throws Exception;
}
