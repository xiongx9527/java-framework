package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockGroupMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockGroup;
import com.xiongx9527.fw.plus.tinyengine.service.BlockGroupService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlockGroupServiceImpl extends ServiceImpl<BlockGroupMapper, BlockGroup> implements BlockGroupService {

	@Override
	public List<BlockGroup> findAllBlockGroup() {
		return baseMapper.queryAllBlockGroup();
	}
}
