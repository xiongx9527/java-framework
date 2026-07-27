package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.TenantMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Tenant;
import com.xiongx9527.fw.plus.tinyengine.service.TenantService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

	@Override
	public List<Tenant> findAllTenant() {
		return baseMapper.queryAllTenant();
	}
}
