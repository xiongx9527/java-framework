package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BusinessCategory;

import java.util.List;

public interface BusinessCategoryService extends IService<BusinessCategory> {

	List<BusinessCategory> queryAllBusinessCategory();
	BusinessCategory queryBusinessCategoryById(Integer id);
	List<BusinessCategory> queryBusinessCategoryByCondition(BusinessCategory businessCategory);
	Integer deleteBusinessCategoryById(Integer id);
	Integer updateBusinessCategoryById(BusinessCategory businessCategory);
	Integer createBusinessCategory(BusinessCategory businessCategory);
}
