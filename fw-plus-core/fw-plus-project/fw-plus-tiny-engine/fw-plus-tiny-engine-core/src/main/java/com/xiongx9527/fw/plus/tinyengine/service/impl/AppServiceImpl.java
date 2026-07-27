package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.service.AppService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

	private final TinyEngineUserContext userContext;

	public AppServiceImpl(TinyEngineUserContext userContext) {
		this.userContext = userContext;
	}

	@Override
	public List<App> queryAllApp() {
		TinyEngineUser user = userContext.getCurrentUser();
		return baseMapper.queryAllApp(user == null ? null : user.getTenantId());
	}
}
