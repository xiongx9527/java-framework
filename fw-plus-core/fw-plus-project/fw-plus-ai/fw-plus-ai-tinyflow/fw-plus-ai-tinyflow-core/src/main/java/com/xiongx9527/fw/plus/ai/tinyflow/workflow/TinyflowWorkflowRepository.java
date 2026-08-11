package com.xiongx9527.fw.plus.ai.tinyflow.workflow;

public interface TinyflowWorkflowRepository {

	TinyflowWorkflow findById(String id);

	TinyflowWorkflow findByPath(String path);

	void save(TinyflowWorkflow workflow);

	void delete(String id);
}
