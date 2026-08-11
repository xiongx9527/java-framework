package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockBuildDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParam;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParamDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.NotGroupDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Block;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Tenant;
import com.xiongx9527.fw.plus.tinyengine.model.entity.User;

import java.util.List;
import java.util.Map;

public interface BlockService extends IService<Block> {

	List<Block> queryAllBlock();
	BlockDto queryBlockById(Integer id);
	List<Block> queryBlockByCondition(Block block);
	Integer deleteBlockById(Integer id);
	BlockDto updateBlockById(BlockParam blockParam);
	BlockDto createBlock(BlockParam blockParam);
	IPage<Block> findBlocksByPagetionList(BlockParamDto blockParamDto);
	List<String> allTags();
	IPage<Block> findBlocksByConditionPagetion(Map<String, String> request);
	List<User> getUsers(List<Block> blocksList);
	List<Block> listNew(String appId, String groupId);
	List<BlockDto> getNotInGroupBlocks(NotGroupDto notGroupDto);
	BlockDto getBlockByLabel(String label, Integer appId);
	BlockDto deploy(BlockBuildDto blockBuildDto);
	List<BlockHistory> blockHistories(Integer blockId);
	List<Tenant> blockTenants();
	List<User> blockUsers();
}
