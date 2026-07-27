package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;

import java.util.List;

public interface PageService extends IService<Page> {

	List<Page> queryAllPage(Integer appId);

	Page queryPageById(Integer id);

	List<Page> queryPageByCondition(Page page);

	Page delPage(Integer id);

	Page createPage(Page page);

	Page createFolder(Page page);

	Page updatePage(Page page);

	Page update(Page page);
}
