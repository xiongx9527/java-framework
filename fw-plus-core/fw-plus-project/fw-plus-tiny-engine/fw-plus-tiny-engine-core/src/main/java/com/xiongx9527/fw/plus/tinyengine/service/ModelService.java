package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Model;

import java.util.List;

public interface ModelService {

	boolean save(Model model);
	boolean updateById(Model model);
	boolean removeById(java.io.Serializable id);

	Model queryModelById(Integer id);
	Model queryModelDetailById(Integer id);
	List<Model> getModelByName(String nameCn);
	List<Model> getModelByEnName(String nameEn);
	Page<Model> pageQuery(int currentPage, int pageSize, String nameCn, String nameEn);
	String getTableById(Integer id);
	String getAllTable();
	List<String> getAllModelName();
	List<Model> queryModelByCondition(Model model);
}
