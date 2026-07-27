package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.xiongx9527.fw.plus.tinyengine.mapper.BlockMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.UserMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.CanvasDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Block;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.model.entity.User;
import com.xiongx9527.fw.plus.tinyengine.service.CanvasService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CanvasServiceImpl implements CanvasService {

	private final PageMapper pageMapper;
	private final BlockMapper blockMapper;
	private final UserMapper userMapper;
	private final TinyEngineUserContext userContext;

	public CanvasServiceImpl(PageMapper pageMapper, BlockMapper blockMapper, UserMapper userMapper,
			TinyEngineUserContext userContext) {
		this.pageMapper = pageMapper;
		this.blockMapper = blockMapper;
		this.userMapper = userMapper;
		this.userContext = userContext;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public CanvasDto lockCanvas(Integer id, String state, String type) {
		User user = currentUser();
		String occupier = user.getId();
		String nextOccupier = "occupy".equals(state) ? occupier : null;
		boolean updated;
		if ("page".equals(type)) {
			Page page = pageMapper.queryPageById(id);
			updated = page != null && canOperate(page.getOccupierBy(), occupier);
			if (updated) {
				Page changed = new Page();
				changed.setId(id);
				changed.setOccupierBy(nextOccupier);
				pageMapper.updatePageById(changed);
			}
		} else {
			Block block = blockMapper.queryBlockById(id);
			updated = block != null && canOperate(block.getOccupierBy(), occupier);
			if (updated) {
				Block changed = new Block();
				changed.setId(id);
				changed.setOccupierBy(nextOccupier);
				blockMapper.updateBlockById(changed);
			}
		}
		CanvasDto result = new CanvasDto();
		result.setOperate(updated ? "success" : "failed");
		result.setOccupier(nextOccupier == null ? null : user);
		return result;
	}

	private User currentUser() {
		TinyEngineUser current = userContext.getCurrentUser();
		String userId = current == null ? null : current.getUserId();
		User user = userId == null ? null : userMapper.queryUserById(userId);
		if (user == null) {
			user = new User();
			user.setId(userId);
			if (current != null) {
				user.setUsername(current.getUserName());
			}
		}
		return user;
	}

	private boolean canOperate(String currentOccupier, String userId) {
		return currentOccupier == null || currentOccupier.equals(userId);
	}
}
