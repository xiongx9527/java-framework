package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Block;
import com.xiongx9527.fw.plus.tinyengine.service.BlockService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlockServiceImpl extends ServiceImpl<BlockMapper, Block> implements BlockService {

	@Override
	public List<Block> queryAllBlock() {
		return baseMapper.queryAllBlock();
	}
}
