package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Block;

import java.util.List;

public interface BlockService extends IService<Block> {

	List<Block> queryAllBlock();
}
