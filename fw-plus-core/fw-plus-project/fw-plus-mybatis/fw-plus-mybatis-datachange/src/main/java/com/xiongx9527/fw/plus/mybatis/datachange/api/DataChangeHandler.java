package com.xiongx9527.fw.plus.mybatis.datachange.api;

public interface DataChangeHandler {
	boolean supports(DataChangeEvent event);
	void handle(DataChangeEvent event) throws Exception;
	int getOrder();
}
