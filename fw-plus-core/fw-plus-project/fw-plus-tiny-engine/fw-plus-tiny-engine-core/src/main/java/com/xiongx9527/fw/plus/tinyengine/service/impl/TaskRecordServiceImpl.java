package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.mapper.TaskRecordMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.TaskRecord;
import com.xiongx9527.fw.plus.tinyengine.service.TaskRecordService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskRecordServiceImpl extends ServiceImpl<TaskRecordMapper, TaskRecord> implements TaskRecordService {

	@Override
	public List<TaskRecord> queryAllTaskRecord() {
		return baseMapper.queryAllTaskRecord();
	}

	@Override
	public TaskRecord queryTaskRecordById(Integer id) {
		return baseMapper.queryTaskRecordById(id);
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
