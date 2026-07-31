package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Model;

import java.util.List;

public interface ModelService extends IService<Model> {

	Model queryModelById(Integer id);
	List<Model> getModelByName(String nameCn);
	List<Model> getModelByEnName(String nameEn);
	Page<Model> pageQuery(int currentPage, int pageSize, String nameCn, String nameEn);
	String getTableById(Integer id);
	String getAllTable();
	List<String> getAllModelName();
	List<Model> queryModelByCondition(Model model);
}
