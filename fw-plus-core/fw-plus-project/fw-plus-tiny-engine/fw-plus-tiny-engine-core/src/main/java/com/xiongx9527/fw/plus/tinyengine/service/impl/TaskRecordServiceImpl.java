package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.TaskRecordMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.TaskRecord;
import com.xiongx9527.fw.plus.tinyengine.service.TaskRecordService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskRecordServiceImpl extends ServiceImpl<TaskRecordMapper, TaskRecord> implements TaskRecordService {

	@Autowired
	private TinyEngineUserContext userContext;

	@Override
	public List<TaskRecord> queryAllTaskRecord() {
		return baseMapper.queryAllTaskRecord();
	}

	@Override
	public TaskRecord queryTaskRecordById(Integer id) {
		return baseMapper.queryTaskRecordById(id);
	}

	@Override
	public TaskRecord taskStatus(Integer id) {
		TaskRecord condition = new TaskRecord();
		condition.setId(id);
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		condition.setTenantId(user == null ? null : user.getTenantId());
		List<TaskRecord> records = baseMapper.queryTaskRecordByCondition(condition);
		return records == null || records.isEmpty() ? null : records.get(0);
	}

	@Override
	public List<TaskRecord> queryTaskRecordByCondition(TaskRecord taskRecord) {
		return baseMapper.queryTaskRecordByCondition(taskRecord);
	}

	@Override
	public Integer deleteTaskRecordById(Integer id) {
		return baseMapper.deleteTaskRecordById(id);
	}

	@Override
	public Integer updateTaskRecordById(TaskRecord taskRecord) {
		return baseMapper.updateTaskRecordById(taskRecord);
	}

	@Override
	public Integer createTaskRecord(TaskRecord taskRecord) {
		return baseMapper.createTaskRecord(taskRecord);
	}

	@Override
	public List<List<TaskRecord>> status(int taskTypeId, String uniqueIds) {
		List<List<TaskRecord>> result = new ArrayList<List<TaskRecord>>();
		if (uniqueIds == null || uniqueIds.trim().isEmpty()) {
			return result;
		}
		String[] values = uniqueIds.split(",");
		for (String value : values) {
			result.add(baseMapper.findTaskRecordByTaskIdAndUniqueid(taskTypeId,
					Integer.parseInt(value.trim())));
		}
		return result;
	}
}
