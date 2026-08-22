package com.xiongx9527.fw.plus.tinyengine.service.impl;

import org.springframework.beans.factory.annotation.Autowired;

abstract class MapperServiceSupport<M> {

	@Autowired
	protected M baseMapper;
}
