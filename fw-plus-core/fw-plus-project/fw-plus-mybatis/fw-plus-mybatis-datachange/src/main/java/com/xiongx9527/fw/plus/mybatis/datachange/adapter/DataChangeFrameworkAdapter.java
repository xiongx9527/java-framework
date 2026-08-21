package com.xiongx9527.fw.plus.mybatis.datachange.adapter;

public interface DataChangeFrameworkAdapter {
	boolean supports(DataChangeInvocation invocation);
	FrameworkChangeMetadata resolve(DataChangeInvocation invocation);
	int getOrder();
}
