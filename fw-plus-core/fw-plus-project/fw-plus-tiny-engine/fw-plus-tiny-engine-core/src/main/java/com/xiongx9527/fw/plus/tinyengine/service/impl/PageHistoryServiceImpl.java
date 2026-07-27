package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageHistoryMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageHistory;
import com.xiongx9527.fw.plus.tinyengine.service.PageHistoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PageHistoryServiceImpl extends ServiceImpl<PageHistoryMapper, PageHistory> implements PageHistoryService {

	@Override
	public List<PageHistory> findAllPageHistory() {
		return baseMapper.queryAllPageHistory();
	}
}
