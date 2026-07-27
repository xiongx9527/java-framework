package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.BusinessCategoryMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BusinessCategory;
import com.xiongx9527.fw.plus.tinyengine.service.BusinessCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusinessCategoryServiceImpl extends ServiceImpl<BusinessCategoryMapper, BusinessCategory> implements BusinessCategoryService {

	@Override
	public List<BusinessCategory> queryAllBusinessCategory() {
		return baseMapper.queryAllBusinessCategory();
	}
}
