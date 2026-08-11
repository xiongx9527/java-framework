package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockGroupBlockMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockGroupMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockHistoryMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.I18nEntryMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.TenantMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.UserMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockBuildDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParam;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParamDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.NotGroupDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.SchemaI18n;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Block;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockGroup;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockGroupBlock;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Tenant;
import com.xiongx9527.fw.plus.tinyengine.model.entity.User;
import com.xiongx9527.fw.plus.tinyengine.service.BlockHistoryService;
import com.xiongx9527.fw.plus.tinyengine.service.BlockService;
import com.xiongx9527.fw.plus.tinyengine.service.I18nEntryService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BlockServiceImpl extends ServiceImpl<BlockMapper, Block> implements BlockService {
	private static final int FULL_PUBLIC = 1;
	private static final int PUBLIC_IN_TENANTS = 2;

	@Autowired
	private UserMapper userMapper;
	@Autowired
	private TenantMapper tenantMapper;
	@Autowired
	private BlockHistoryService blockHistoryService;
	@Autowired
	private AppMapper appMapper;
	@Autowired
	private BlockHistoryMapper blockHistoryMapper;
	@Autowired
	private I18nEntryService i18nEntryService;
	@Autowired
	private I18nEntryMapper i18nEntryMapper;
	@Autowired
	private BlockGroupMapper blockGroupMapper;
	@Autowired
	private BlockGroupBlockMapper blockGroupBlockMapper;
	@Autowired
	private TinyEngineUserContext userContext;

	@Override
	public List<Block> queryAllBlock() {
		return baseMapper.queryAllBlock();
	}

	@Override
	public List<BlockHistory> blockHistories(Integer blockId) {
		List<BlockHistory> histories = blockHistoryService.findBlockHistoriesByBlockId(blockId, currentTenantId());
		return histories == null ? Collections.<BlockHistory>emptyList() : histories;
	}

	@Override
	public List<Tenant> blockTenants() {
		String tenantId = currentTenantId();
		if (!notEmpty(tenantId)) {
			return Collections.emptyList();
		}
		List<Tenant> tenants = tenantMapper.selectList(new QueryWrapper<Tenant>().eq("id", tenantId));
		return tenants == null ? Collections.<Tenant>emptyList() : tenants;
	}

	@Override
	public List<User> blockUsers() {
		String tenantId = currentTenantId();
		if (!notEmpty(tenantId)) {
			return Collections.emptyList();
		}
		List<String> authorIds = baseMapper.selectDistinctCreatedByTenant(tenantId);
		if (authorIds == null || authorIds.isEmpty()) {
			return Collections.emptyList();
		}
		List<User> users = userMapper.selectUsersByIds(authorIds);
		return users == null ? Collections.<User>emptyList() : users;
	}

	@Override
	public BlockDto queryBlockById(Integer id) {
		BlockDto block = baseMapper.findBlockAndGroupAndHistoByBlockId(id);
		if (block == null) {
			return null;
		}
		block.setIsPublished(published(block.getLastBuildInfo()));
		block.setGroups(blockGroupMapper.findBlockGroupByBlockId(block.getId(), currentUserId()));
		return block;
	}

	@Override
	public List<Block> queryBlockByCondition(Block block) {
		return baseMapper.queryBlockByCondition(block);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Integer deleteBlockById(Integer id) {
		baseMapper.deleteBlockById(id);
		return blockGroupBlockMapper.deleteByBlockId(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public BlockDto updateBlockById(BlockParam blockParam) {
		if (blockParam == null || blockParam.getId() == null) {
			throw failure(ExceptionEnum.CM002);
		}
		Block existing = baseMapper.queryBlockById(blockParam.getId());
		if (existing == null) {
			throw failure(ExceptionEnum.CM001);
		}
		if (!Objects.equals(existing.getOccupierBy(), currentUserId())) {
			throw failure(ExceptionEnum.CM007);
		}
		Block changed = new Block();
		BeanUtils.copyProperties(blockParam, changed);
		changed.setOccupierBy(currentUserId());
		if (blockParam.getLatestHistoryId() != null) {
			changed.setLatestHistoryId(blockParam.getLatestHistoryId().getId());
		}
		if (blockParam.getScreenshot() != null && blockParam.getLabel() != null) {
			changed.setScreenshot("");
		}
		List<Integer> groups = blockParam.getGroups();
		if (groups != null) {
			List<BlockGroup> oldGroups = blockGroupMapper.findBlockGroupByBlockId(changed.getId(), currentUserId());
			if (oldGroups != null) {
				for (BlockGroup group : oldGroups) {
					blockGroupBlockMapper.deleteByGroupIdAndBlockId(group.getId(), changed.getId());
				}
			}
		}
		if (baseMapper.updateBlockById(changed) != 1) {
			throw failure(ExceptionEnum.CM001);
		}
		if (groups != null) {
			for (Integer groupId : groups) {
				BlockGroupBlock relation = new BlockGroupBlock();
				relation.setBlockId(changed.getId());
				relation.setBlockGroupId(groupId);
				blockGroupBlockMapper.createBlockGroupBlock(relation);
			}
		}
		return queryBlockById(changed.getId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public BlockDto createBlock(BlockParam blockParam) {
		if (blockParam == null || blockParam.getLabel() == null) {
			throw failure(ExceptionEnum.CM002);
		}
		Block block = new Block();
		BeanUtils.copyProperties(blockParam, block);
		block.setOccupierBy(blockParam.getOccupier() == null
				? currentUserId() : String.valueOf(blockParam.getOccupier().getId()));
		block.setIsDefault(false);
		block.setIsOfficial(false);
		block.setPlatformId(currentPlatformId());
		if (baseMapper.createBlock(block) < 1) {
			throw failure(ExceptionEnum.CM001);
		}
		blockParam.setId(block.getId());
		List<Integer> groups = blockParam.getGroups();
		if (groups != null && !groups.isEmpty()) {
			BlockGroupBlock relation = new BlockGroupBlock();
			relation.setBlockGroupId(groups.get(0));
			relation.setBlockId(block.getId());
			if (blockGroupBlockMapper.createBlockGroupBlock(relation) < 1) {
				throw failure(ExceptionEnum.CM001);
			}
		}
		return queryBlockById(block.getId());
	}

	@Override
	public IPage<Block> findBlocksByPagetionList(BlockParamDto request) {
		if (request.getAppId() != null && !Pattern.matches("^[1-9]+[0-9]*$", request.getAppId())) {
			request.setAppId(null);
		}
		QueryWrapper<Block> query = new QueryWrapper<Block>();
		if (notEmpty(request.getName())) {
			query.like("name", request.getName());
		}
		if (notEmpty(request.getDescription())) {
			query.or().like("description", request.getDescription());
		}
		if (notEmpty(request.getLabel())) {
			query.or().eq("label", request.getLabel());
		}
		if (request.getSort() != null) {
			String[] sort = request.getSort().split(":", 2);
			if (sort.length == 2 && "ASC".equalsIgnoreCase(sort[1])) {
				query.orderByAsc("created_time");
			} else {
				query.orderByDesc("last_updated_time");
			}
		}
		int limit = request.getLimit() == null || request.getLimit() < 1 ? 10 : request.getLimit();
		int start = request.getStart() == null || request.getStart() < 0 ? 0 : request.getStart();
		return baseMapper.selectPage(new Page<Block>((start / limit) + 1, limit), query);
	}

	@Override
	public List<String> allTags() {
		List<Block> blocks = baseMapper.selectList(new QueryWrapper<Block>().select("tags").isNotNull("tags"));
		if (blocks == null) {
			return Collections.emptyList();
		}
		return blocks.stream().filter(Objects::nonNull).map(Block::getTags).filter(Objects::nonNull)
				.flatMap(List::stream).collect(Collectors.toList());
	}

	@Override
	public IPage<Block> findBlocksByConditionPagetion(Map<String, String> request) {
		String name = request == null ? null : request.get("name_cn");
		String description = request == null ? null : request.get("description");
		QueryWrapper<Block> query = new QueryWrapper<Block>();
		query.and(wrapper -> wrapper.like(notEmpty(name), "name", name)
				.or().like(notEmpty(description), "description", description));
		List<Block> blocks = baseMapper.selectList(query);
		int size = blocks == null || blocks.isEmpty() ? 1 : blocks.size();
		return baseMapper.selectPage(new Page<Block>(1, size), query);
	}

	@Override
	public List<User> getUsers(List<Block> blocksList) {
		if (blocksList == null || blocksList.isEmpty()) {
			return Collections.emptyList();
		}
		Set<String> userIds = new HashSet<String>();
		for (Block block : blocksList) {
			if (block.getCreatedBy() != null) {
				userIds.add(block.getCreatedBy());
			}
		}
		return userMapper.selectUsersByIds(new ArrayList<String>(userIds));
	}

	@Override
	public List<Block> listNew(String appId, String groupId) {
		int parsedAppId = parseId(appId);
		int parsedGroupId = parseId(groupId);
		App app = appMapper.queryAppById(parsedAppId, currentTenantId());
		if (parsedGroupId != 0 && (app == null || !Integer.valueOf(parsedAppId).equals(app.getId()))) {
			throw failure(ExceptionEnum.CM206);
		}
		if (parsedGroupId != 0) {
			return baseMapper.findBlockByBlockGroupId(parsedGroupId, currentUserId());
		}
		List<Block> appBlocks = baseMapper.findBlocksByBlockGroupIdAppId(parsedAppId);
		Block condition = new Block();
		condition.setCreatedBy(currentUserId());
		List<Block> personalBlocks = queryBlockByCondition(condition);
		List<Block> combined = Stream.concat(safe(appBlocks).stream(), safe(personalBlocks).stream())
				.collect(Collectors.toList());
		Map<Integer, Block> unique = new java.util.LinkedHashMap<Integer, Block>();
		for (Block block : combined) {
			block.setIsPublished(published(block.getLastBuildInfo()));
			unique.put(block.getId(), block);
		}
		return new ArrayList<Block>(unique.values());
	}

	@Override
	public List<BlockDto> getNotInGroupBlocks(NotGroupDto request) {
		List<BlockDto> blocks = baseMapper.findBlocksReturn(request);
		if (blocks == null) {
			return Collections.emptyList();
		}
		for (BlockDto block : blocks) {
			block.setGroups(blockGroupMapper.findBlockGroupByBlockId(block.getId(), currentUserId()));
		}
		return blocks.stream().filter(block -> block.getLastBuildInfo() != null
				&& block.getContent() != null && block.getAssets() != null)
				.filter(block -> block.getGroups() == null || block.getGroups().stream()
						.noneMatch(group -> group != null && Objects.equals(group.getId(), request.getGroupId())))
				.filter(block -> Integer.valueOf(FULL_PUBLIC).equals(block.getPublicStatus())
						|| Integer.valueOf(PUBLIC_IN_TENANTS).equals(block.getPublicStatus()))
				.collect(Collectors.toList());
	}

	@Override
	public BlockDto getBlockByLabel(String label, Integer appId) {
		Block condition = new Block();
		condition.setLabel(label);
		condition.setAppId(appId);
		List<Block> blocks = queryBlockByCondition(condition);
		return blocks == null || blocks.isEmpty() ? null : queryBlockById(blocks.get(0).getId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public BlockDto deploy(BlockBuildDto request) {
		if (request == null || request.getBlock() == null) {
			throw failure(ExceptionEnum.CM002);
		}
		if (request.getBlock().getContent() == null || request.getBlock().getContent().isEmpty()) {
			throw failure(ExceptionEnum.CM204);
		}
		int blockId = ensureBlockId(request.getBlock());
		if (historyExists(blockId, request.getVersion())) {
			throw failure(ExceptionEnum.CM205);
		}
		List<I18nEntryDto> entries = i18nEntryMapper.findI18nEntriesByHostandHostType(blockId, "block");
		SchemaI18n schema = i18nEntryService.formatEntriesList(entries);
		BlockDto block = request.getBlock();
		Map<String, Map<String, String>> i18n = new HashMap<String, Map<String, String>>();
		i18n.put("zh_CN", schema.getZhCn());
		i18n.put("en_US", schema.getEnUs());
		block.setI18n(i18n);
		BlockHistory history = new BlockHistory();
		BeanUtils.copyProperties(block, history);
		history.setIsPublic(block.getPublic());
		history.setRefId(blockId);
		history.setVersion(request.getVersion());
		history.setMessage(request.getDeployInfo());
		Map<String, Object> buildInfo = buildInfo(request.getVersion());
		history.setBuildInfo(buildInfo);
		history.setId(null);
		if (blockHistoryMapper.createBlockHistory(history) < 1) {
			throw failure(ExceptionEnum.CM008);
		}
		BlockParam update = new BlockParam();
		update.setId(blockId);
		update.setAppId(block.getAppId());
		update.setLastBuildInfo(buildInfo);
		update.setLatestHistoryId(history);
		update.setLatestVersion(history.getVersion());
		update.setGroups(null);
		return updateBlockById(update);
	}

	private int ensureBlockId(BlockDto block) {
		if (block.getId() != null) {
			return block.getId();
		}
		Block condition = new Block();
		condition.setLabel(block.getLabel());
		condition.setFramework(block.getFramework());
		condition.setCreatedBy(currentUserId());
		List<Block> existing = queryBlockByCondition(condition);
		if (existing != null && !existing.isEmpty()) {
			return existing.get(0).getId();
		}
		BlockParam create = new BlockParam();
		BeanUtils.copyProperties(block, create);
		create.setGroups(block.getGroups() == null ? Collections.<Integer>emptyList()
				: block.getGroups().stream().map(BlockGroup::getId).collect(Collectors.toList()));
		BlockDto created = createBlock(create);
		if (created == null || created.getId() == null) {
			throw failure(ExceptionEnum.CM001);
		}
		return created.getId();
	}

	private boolean historyExists(Integer id, String version) {
		BlockHistory condition = new BlockHistory();
		condition.setRefId(id);
		condition.setVersion(version);
		List<BlockHistory> histories = blockHistoryMapper.queryBlockHistoryByCondition(condition);
		return histories != null && !histories.isEmpty();
	}

	private Map<String, Object> buildInfo(String version) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("result", true);
		result.put("versions", Collections.singletonList(version));
		result.put("endTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		return result;
	}

	private boolean published(Map<String, Object> buildInfo) {
		return buildInfo != null && buildInfo.get("result") instanceof Boolean
				? (Boolean) buildInfo.get("result") : false;
	}

	private List<Block> safe(List<Block> blocks) {
		return blocks == null ? Collections.<Block>emptyList() : blocks;
	}

	private int parseId(String value) {
		try {
			return value == null || value.isEmpty() ? 0 : Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			throw failure(ExceptionEnum.CM342);
		}
	}

	private boolean notEmpty(String value) {
		return value != null && !value.isEmpty();
	}

	private TinyEngineUser currentUser() {
		return userContext == null ? null : userContext.getCurrentUser();
	}

	private String currentUserId() {
		TinyEngineUser user = currentUser();
		return user == null ? null : user.getUserId();
	}

	private String currentTenantId() {
		TinyEngineUser user = currentUser();
		return user == null ? null : user.getTenantId();
	}

	private Integer currentPlatformId() {
		TinyEngineUser user = currentUser();
		return user == null ? null : user.getPlatformId();
	}

	private ServiceException failure(ExceptionEnum error) {
		return new ServiceException(error.getResultCode(), error.getResultMsg());
	}
}
