package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PlatformHistory;

import java.util.List;

public interface PlatformHistoryService extends IService<PlatformHistory> {

	List<PlatformHistory> queryAllPlatformHistory();
}
