package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockCarriersRelationMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockGroupBlockMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockGroupMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockGroupDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Block;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockCarriersRelation;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockGroup;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockGroupBlock;
import com.xiongx9527.fw.plus.tinyengine.service.AppRoleService;
import com.xiongx9527.fw.plus.tinyengine.service.BlockGroupService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppPermission;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BlockGroupServiceImpl extends MapperServiceSupport<BlockGroupMapper> implements BlockGroupService {
	private static final String HOST_TYPE = "blockGroup";
	@Autowired
	private BlockCarriersRelationMapper blockCarriersRelationMapper;
	@Autowired
	private BlockGroupBlockMapper blockGroupBlockMapper;
	@Autowired
	private TinyEngineUserContext userContext;
	@Autowired
	private AppRoleService appRoleService;
	@Autowired
	private AppMapper appMapper;

	@Override
	public List<BlockGroup> findAllBlockGroup() {
		return filterVisibleGroups(baseMapper.queryAllBlockGroup());
	}

	@Override
	public BlockGroup findBlockGroupById(Integer id) {
		return findBlockGroupById(id, null);
	}

	private BlockGroup findBlockGroupById(Integer id, String groupCreatedBy) {
		BlockGroup group = baseMapper.queryBlockGroupAndBlockById(id, null, groupCreatedBy);
		require(group == null ? null : group.getAppId(), AppPermission.READ);
		applyCurrentVersions(group);
		return group;
	}

	@Override
	public List<BlockGroupDto> findBlockGroupByCondition(BlockGroup blockGroup) {
		List<BlockGroupDto> groups = baseMapper.queryBlockGroupByCondition(blockGroup);
		if (groups != null) {
			for (BlockGroupDto group : groups) {
				require(group == null ? null : group.getAppId(), AppPermission.READ);
			}
		}
		return groups;
	}

	@Override
	public Integer deleteBlockGroupById(Integer id) {
		BlockGroup stored = baseMapper.queryBlockGroupAndBlockById(id, null, null);
		require(stored == null ? null : stored.getAppId(), AppPermission.DEVELOP);
		return baseMapper.deleteBlockGroupById(id);
	}

	@Override
	public Integer updateBlockGroupById(BlockGroup blockGroup) {
		BlockGroup stored = blockGroup == null || blockGroup.getId() == null ? null
				: baseMapper.queryBlockGroupAndBlockById(blockGroup.getId(), null, null);
		require(stored == null ? null : stored.getAppId(), AppPermission.DEVELOP);
		blockGroup.setAppId(stored.getAppId());
		blockGroup.setTenantId(stored.getTenantId());
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
			require(blockGroup == null ? null : blockGroup.getAppId(), AppPermission.DEVELOP);
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
		return findBlockGroupById(blockGroup.getId(), currentUserId());
	}

	@Override
	public List<BlockGroup> getBlockGroupByIdsOrAppId(List<Integer> ids, Integer appId, String from) {
		boolean filterByVisibility = ids == null && appId == null;
		if (appId != null) {
			require(appId, AppPermission.READ);
		}
		String userId = currentUserId();
		String blockCreatedBy = "block".equals(from) ? userId : null;
		List<BlockGroup> groups = new ArrayList<BlockGroup>();
		if (ids != null) {
			for (Integer id : ids) {
				groups.add(baseMapper.queryBlockGroupAndBlockById(id, blockCreatedBy, null));
			}
		} else if (appId != null) {
			groups = baseMapper.queryBlockGroupByAppId(appId, blockCreatedBy, null);
		} else {
			groups = baseMapper.queryAllBlockGroupAndBlock(blockCreatedBy, null);
		}
		if (filterByVisibility) {
			groups = filterVisibleGroups(groups);
		}
		if (groups != null) {
			for (BlockGroup group : groups) {
				require(group == null ? null : group.getAppId(), AppPermission.READ);
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

	private boolean isSystem(TinyEngineUser user) {
		return user != null && "system".equals(user.getUserId());
	}

	private List<BlockGroup> filterVisibleGroups(List<BlockGroup> groups) {
		if (groups == null || groups.isEmpty()) {
			return Collections.emptyList();
		}
		TinyEngineUser user = currentUser();
		if (user == null || user.getTenantId() == null || appMapper == null) {
			return Collections.emptyList();
		}
		List<App> visibleApps = appMapper.queryVisibleApps(user.getTenantId(), user.getUserId(), isSystem(user));
		if (visibleApps == null || visibleApps.isEmpty()) {
			return Collections.emptyList();
		}
		Set<Integer> visibleAppIds = new HashSet<Integer>();
		for (App app : visibleApps) {
			if (app != null && app.getId() != null) {
				visibleAppIds.add(app.getId());
			}
		}
		return groups.stream().filter(group -> group != null && visibleAppIds.contains(group.getAppId()))
				.collect(Collectors.toList());
	}

	private void requireAll(List<BlockGroup> groups, AppPermission permission) {
		if (groups == null) {
			return;
		}
		for (BlockGroup group : groups) {
			require(group == null ? null : group.getAppId(), permission);
		}
	}

	private void require(Integer appId, AppPermission permission) {
		if (appRoleService != null) {
			appRoleService.requirePermission(appId, permission);
		}
	}

	private ServiceException failure(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}
}
