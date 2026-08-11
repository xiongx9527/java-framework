package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockCarriersRelationMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockGroupBlockMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockGroupMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockGroupDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Block;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockCarriersRelation;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockGroup;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockGroupBlock;
import com.xiongx9527.fw.plus.tinyengine.service.BlockGroupService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BlockGroupServiceImpl extends ServiceImpl<BlockGroupMapper, BlockGroup> implements BlockGroupService {
	private static final String HOST_TYPE = "blockGroup";
	@Autowired
	private BlockCarriersRelationMapper blockCarriersRelationMapper;
	@Autowired
	private BlockGroupBlockMapper blockGroupBlockMapper;
	@Autowired
	private TinyEngineUserContext userContext;

	@Override
	public List<BlockGroup> findAllBlockGroup() {
		return baseMapper.queryAllBlockGroup();
	}

	@Override
	public BlockGroup findBlockGroupById(Integer id) {
		BlockGroup group = baseMapper.queryBlockGroupAndBlockById(id, null, currentUserId());
		applyCurrentVersions(group);
		return group;
	}

	@Override
	public List<BlockGroupDto> findBlockGroupByCondition(BlockGroup blockGroup) {
		return baseMapper.queryBlockGroupByCondition(blockGroup);
	}

	@Override
	public Integer deleteBlockGroupById(Integer id) {
		return baseMapper.deleteBlockGroupById(id);
	}

	@Override
	public Integer updateBlockGroupById(BlockGroup blockGroup) {
		List<Block> blocks = blockGroup.getBlocks() == null ? new ArrayList<Block>() : blockGroup.getBlocks();
		List<BlockGroupBlock> existing = blockGroupBlockMapper.findBlockGroupBlockByBlockGroupId(blockGroup.getId());
		List<Integer> oldIds = existing.stream().map(BlockGroupBlock::getBlockId).collect(Collectors.toList());
		if (blocks.isEmpty()) {
			blockCarriersRelationMapper.deleteBlockCarriersRelation(blockGroup.getId(), HOST_TYPE, null);
			blockGroupBlockMapper.deleteBlockGroupBlockByGroupId(blockGroup.getId());
			return baseMapper.updateBlockGroupById(blockGroup);
		}
		List<Integer> newIds = blocks.stream().map(Block::getId).collect(Collectors.toList());
		for (Integer oldId : oldIds) {
			if (!newIds.contains(oldId)) {
				blockGroupBlockMapper.deleteByGroupIdAndBlockId(blockGroup.getId(), oldId);
				blockCarriersRelationMapper.deleteBlockCarriersRelation(blockGroup.getId(), HOST_TYPE, oldId);
			}
		}
		List<BlockCarriersRelation> relations = new ArrayList<BlockCarriersRelation>();
		for (Block block : blocks) {
			if (!oldIds.contains(block.getId())) {
				BlockGroupBlock relation = new BlockGroupBlock();
				relation.setBlockGroupId(blockGroup.getId());
				relation.setBlockId(block.getId());
				blockGroupBlockMapper.createBlockGroupBlock(relation);
			}
			BlockCarriersRelation carrier = new BlockCarriersRelation();
			carrier.setHostId(blockGroup.getId());
			carrier.setHostType(HOST_TYPE);
			carrier.setBlockId(block.getId());
			carrier.setVersion(block.getLatestVersion());
			relations.add(carrier);
		}
		blockCarriersRelationMapper.createOrUpdateBatch(relations);
		return baseMapper.updateBlockGroupById(blockGroup);
	}

	@Override
		public BlockGroup createBlockGroup(BlockGroup blockGroup) {
			TinyEngineUser currentUser = currentUser();
			if (currentUser != null) {
				blockGroup.setTenantId(currentUser.getTenantId());
				blockGroup.setRenterId(currentUser.getRenterId());
				blockGroup.setCreatedBy(currentUser.getUserId());
				blockGroup.setLastUpdatedBy(currentUser.getUserId());
				if (blockGroup.getPlatformId() == null) {
					blockGroup.setPlatformId(currentUser.getPlatformId());
				}
			}
			LocalDateTime now = LocalDateTime.now();
		blockGroup.setCreatedTime(now);
		blockGroup.setLastUpdatedTime(now);
		if (!baseMapper.queryBlockGroupByCondition(blockGroup).isEmpty()) {
			throw failure(ExceptionEnum.CM003);
		}
		if (baseMapper.createBlockGroup(blockGroup) != 1) {
			throw failure(ExceptionEnum.CM001);
		}
		return findBlockGroupById(blockGroup.getId());
	}

	@Override
	public List<BlockGroup> getBlockGroupByIdsOrAppId(List<Integer> ids, Integer appId, String from) {
		String userId = currentUserId();
		String blockCreatedBy = "block".equals(from) ? userId : null;
		List<BlockGroup> groups = new ArrayList<BlockGroup>();
		if (ids != null) {
			for (Integer id : ids) {
				groups.add(baseMapper.queryBlockGroupAndBlockById(id, blockCreatedBy, userId));
			}
		} else if (appId != null) {
			groups = baseMapper.queryBlockGroupByAppId(appId, blockCreatedBy, userId);
		} else {
			groups = baseMapper.queryAllBlockGroupAndBlock(blockCreatedBy, userId);
		}
		if (groups != null) {
			for (BlockGroup group : groups) {
				applyCurrentVersions(group);
			}
		}
		return groups;
	}

	private void applyCurrentVersions(BlockGroup group) {
		if (group == null || group.getBlocks() == null) {
			return;
		}
		for (Block block : group.getBlocks()) {
			BlockCarriersRelation condition = new BlockCarriersRelation();
			condition.setBlockId(block.getId());
			condition.setHostId(group.getId());
			condition.setHostType(HOST_TYPE);
			List<BlockCarriersRelation> relations =
					blockCarriersRelationMapper.queryBlockCarriersRelationByCondition(condition);
			if (relations != null && !relations.isEmpty()) {
				block.setCurrentVersion(relations.get(0).getVersion());
			}
		}
	}

	private String currentUserId() {
		TinyEngineUser user = currentUser();
		return user == null ? null : user.getUserId();
	}

	private TinyEngineUser currentUser() {
		return userContext == null ? null : userContext.getCurrentUser();
	}

	private ServiceException failure(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}
}
