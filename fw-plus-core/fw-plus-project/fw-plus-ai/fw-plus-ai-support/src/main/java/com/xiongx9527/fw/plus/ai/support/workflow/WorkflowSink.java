package com.xiongx9527.fw.plus.ai.support.workflow;

/**
 * 步骤回调：边执行边把进度推给调用方（前端展示 / 日志）。
 */
public interface WorkflowSink {

    void onStep(WorkflowStep step);
}
