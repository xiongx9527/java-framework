package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Tenant;

import java.util.List;

public interface TenantService extends IService<Tenant> {

	List<Tenant> findAllTenant();
}
