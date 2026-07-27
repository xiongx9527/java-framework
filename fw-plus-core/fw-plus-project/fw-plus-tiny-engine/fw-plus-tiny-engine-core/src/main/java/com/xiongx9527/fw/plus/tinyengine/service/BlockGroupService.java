package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockGroupDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockGroup;

import java.util.List;

public interface BlockGroupService extends IService<BlockGroup> {

	List<BlockGroup> findAllBlockGroup();
	BlockGroup findBlockGroupById(Integer id);
	List<BlockGroupDto> findBlockGroupByCondition(BlockGroup blockGroup);
	Integer deleteBlockGroupById(Integer id);
	Integer updateBlockGroupById(BlockGroup blockGroup);
	BlockGroup createBlockGroup(BlockGroup blockGroup);
	List<BlockGroup> getBlockGroupByIdsOrAppId(List<Integer> ids, Integer appId, String from);
}
