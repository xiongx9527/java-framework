package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Tenant;

import java.util.List;

public interface TenantService extends IService<Tenant> {

	List<Tenant> findAllTenant();
	Tenant findTenantById(Integer id);
	List<Tenant> findTenantByCondition(Tenant tenant);
	Integer deleteTenantById(Integer id);
	Integer updateTenantById(Tenant tenant);
	Integer createTenant(Tenant tenant);
}
