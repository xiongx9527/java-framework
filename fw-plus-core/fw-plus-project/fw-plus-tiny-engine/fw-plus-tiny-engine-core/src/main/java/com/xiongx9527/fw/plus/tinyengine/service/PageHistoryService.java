package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageHistory;

import java.util.List;

public interface PageHistoryService extends IService<PageHistory> {

	List<PageHistory> findAllPageHistory();
}
