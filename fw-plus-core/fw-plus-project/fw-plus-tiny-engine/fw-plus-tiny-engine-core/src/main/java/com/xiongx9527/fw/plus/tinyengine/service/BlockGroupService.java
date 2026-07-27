package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockGroup;

import java.util.List;

public interface BlockGroupService extends IService<BlockGroup> {

	List<BlockGroup> findAllBlockGroup();
}
