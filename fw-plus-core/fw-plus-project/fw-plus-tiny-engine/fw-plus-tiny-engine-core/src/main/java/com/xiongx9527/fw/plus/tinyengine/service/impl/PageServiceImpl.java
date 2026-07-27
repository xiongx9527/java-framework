package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.service.PageService;
import com.xiongx9527.fw.plus.tinyengine.support.hook.NoopTinyEngineSaveHook;
import com.xiongx9527.fw.plus.tinyengine.support.hook.TinyEngineSaveHook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PageServiceImpl extends ServiceImpl<PageMapper, Page> implements PageService {

	private TinyEngineSaveHook saveHook = new NoopTinyEngineSaveHook();

	@Autowired(required = false)
	public void setSaveHook(TinyEngineSaveHook saveHook) {
		if (saveHook != null) {
			this.saveHook = saveHook;
		}
	}

	@Override
	public List<Page> queryAllPage(Integer appId) {
		return appId == null ? baseMapper.queryAllPage() : baseMapper.queryPageByApp(appId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean save(Page page) {
		boolean saved = super.save(page);
		if (saved) {
			saveHook.afterSave(page);
		}
		return saved;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateById(Page page) {
		boolean updated = super.updateById(page);
		if (updated) {
			saveHook.afterSave(page);
		}
		return updated;
	}
}
