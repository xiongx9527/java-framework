package com.xiongx9527.fw.plus.ai.support.workflow;

import com.xiongx9527.fw.plus.ai.tool.ToolSpecification;
import java.util.List;

/**
 * 编排请求。
 * 出处：AgentScope goal + AgenticScope（oid/uid 由 adapter 注入且只读，工具执行前用 CallContext 重新校验，不可信 scope 内值）；
 * langchain4j-agentic planner/supervisor 输入；spring-ai agent 目标。
 */
public class WorkflowRequest {
    private final String goal;
    private final List<ToolSpecification> allowedTools;
    private final WorkflowBudget budget;
    private final String oid;
    private final String uid;
    public WorkflowRequest(String goal, List<ToolSpecification> allowedTools, WorkflowBudget budget, String oid, String uid) {
        this.goal = goal; this.allowedTools = allowedTools; this.budget = budget; this.oid = oid; this.uid = uid;
    }
    public String goal() { return goal; }
    public List<ToolSpecification> allowedTools() { return allowedTools; }
    public WorkflowBudget budget() { return budget; }
    /** 租户（只读，由 adapter 注入，工具执行前需重新校验）。 */
    public String oid() { return oid; }
    /** 用户（只读）。 */
    public String uid() { return uid; }
}
