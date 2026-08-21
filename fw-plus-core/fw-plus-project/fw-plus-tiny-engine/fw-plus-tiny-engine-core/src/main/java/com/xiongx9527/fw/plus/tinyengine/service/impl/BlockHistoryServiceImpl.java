package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockHistoryMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockHistory;
import com.xiongx9527.fw.plus.tinyengine.service.AppRoleService;
import com.xiongx9527.fw.plus.tinyengine.service.BlockHistoryService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlockHistoryServiceImpl extends MapperServiceSupport<BlockHistoryMapper> implements BlockHistoryService {
	@Autowired
	private AppRoleService appRoleService;

	@Override
	public List<BlockHistory> findAllBlockHistory() {
		List<BlockHistory> histories = baseMapper.queryAllBlockHistory();
		requireAll(histories, AppPermission.READ);
		return histories;
	}

	@Override
	public BlockHistory findBlockHistoryById(Integer id) {
		BlockHistory history = baseMapper.queryBlockHistoryById(id);
		require(history == null ? null : history.getAppId(), AppPermission.READ);
		return history;
	}

	@Override
	public List<BlockHistory> findBlockHistoryByCondition(BlockHistory blockHistory) {
		require(blockHistory == null ? null : blockHistory.getAppId(), AppPermission.READ);
		List<BlockHistory> histories = baseMapper.queryBlockHistoryByCondition(blockHistory);
		requireAll(histories, AppPermission.READ);
		return histories;
	}

	@Override
	public Integer deleteBlockHistoryById(Integer id) {
		BlockHistory existing = id == null ? null : baseMapper.queryBlockHistoryById(id);
		require(existing == null ? null : existing.getAppId(), AppPermission.DEVELOP);
		return baseMapper.deleteBlockHistoryById(id);
	}

	@Override
	public Integer updateBlockHistoryById(BlockHistory blockHistory) {
		BlockHistory existing = blockHistory == null || blockHistory.getId() == null ? null
				: baseMapper.queryBlockHistoryById(blockHistory.getId());
		require(existing == null ? null : existing.getAppId(), AppPermission.DEVELOP);
		if (blockHistory != null && existing != null) {
			blockHistory.setAppId(existing.getAppId());
			blockHistory.setTenantId(existing.getTenantId());
			blockHistory.setRenterId(existing.getRenterId());
			blockHistory.setSiteId(existing.getSiteId());
		}
		return baseMapper.updateBlockHistoryById(blockHistory);
	}

	@Override
	public Integer createBlockHistory(BlockHistory blockHistory) {
		require(blockHistory == null ? null : blockHistory.getAppId(), AppPermission.DEVELOP);
		return baseMapper.createBlockHistory(blockHistory);
	}

	@Override
	public List<BlockHistory> findBlockHistoriesByBlockId(Integer blockId, String tenantId) {
		if (blockId == null || tenantId == null || tenantId.trim().isEmpty()) {
			return java.util.Collections.emptyList();
		}
		List<BlockHistory> histories = baseMapper.selectList(new QueryWrapper<BlockHistory>()
				.eq("ref_id", blockId)
				.eq("tenant_id", tenantId)
				.orderByDesc("id"));
		requireAll(histories, AppPermission.READ);
		return histories;
	}

	private void require(Integer appId, AppPermission permission) {
		if (appRoleService != null) {
			appRoleService.requirePermission(appId, permission);
		}
	}

	private void requireAll(List<BlockHistory> histories, AppPermission permission) {
		if (histories == null) {
			return;
		}
		for (BlockHistory history : histories) {
			require(history == null ? null : history.getAppId(), permission);
		}
	}
}
