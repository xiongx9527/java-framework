package dev.langchain4j.agentic.internal;

import static dev.langchain4j.agentic.scope.DefaultAgenticScope.isSerializable;

import dev.langchain4j.agentic.agent.AgentInvocationException;
import dev.langchain4j.agentic.agent.ErrorRecoveryResult;
import dev.langchain4j.agentic.agent.MissingArgumentException;
import dev.langchain4j.agentic.scope.AgenticSystemSuspendedException;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.planner.AgentArgument;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.planner.AgenticSystemTopology;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import dev.langchain4j.service.TokenStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AgentExecutor implements AgentInstance, InternalAgent {

    private final AgentInvoker agentInvoker;
    private final Object agent;

    public AgentExecutor(AgentInvoker agentInvoker, Object agent) {
        this.agentInvoker = agentInvoker;
        this.agent = agent;
    }

    public AgentInvoker agentInvoker() {
        return agentInvoker;
    }

    public Object agent() {
        return agent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentExecutor)) return false;
        AgentExecutor that = (AgentExecutor) o;
        return Objects.equals(agentInvoker, that.agentInvoker) && Objects.equals(agent, that.agent);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + (agentInvoker == null ? 0 : agentInvoker.hashCode());
        hash = 31 * hash + (agent == null ? 0 : agent.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return "AgentExecutor[agentInvoker=" + agentInvoker + ", agent=" + agent + "]";
    }

    private static final Logger LOG = LoggerFactory.getLogger(AgentExecutor.class);

    public Object execute(DefaultAgenticScope agenticScope, PlannerExecutor planner) {
        return execute(agenticScope, planner, agentInvoker.async());
    }

    public Object syncExecute(DefaultAgenticScope agenticScope, PlannerExecutor planner) {
        if (agentInvoker.async()) {
            LOG.info("Executing '{}' agent in a sync way even if declared as async", agentInvoker.name());
        }
        return execute(agenticScope, planner, false);
    }

    private Object execute(DefaultAgenticScope agenticScope, PlannerExecutor planner, boolean async) {
        Object invokedAgent;
        if (agent instanceof AgenticScopeOwner) {
            AgenticScopeOwner co = (AgenticScopeOwner) agent;
            invokedAgent = co.withAgenticScope(agenticScope);
        } else {
            invokedAgent = agent;
        }
        return internalExecute(agenticScope, invokedAgent, planner, async);
    }

    private Object handleAgentFailure(
            AgentInvocationException e,
            DefaultAgenticScope agenticScope,
            Object invokedAgent,
            PlannerExecutor planner,
            AgentInvocationArguments args,
            boolean plannerAlreadyNotified) {
        ErrorRecoveryResult recoveryResult = agenticScope.handleError(agentInvoker.name(), e);
        switch (recoveryResult.type()) {
            case THROW_EXCEPTION:
                throw e;
            case RETRY:
                return internalExecute(agenticScope, invokedAgent, planner, false);
            case RETURN_RESULT:
                return plannerAlreadyNotified
                        ? recoveryResult.result()
                        : completeAgentInvocation(recoveryResult.result(), agenticScope, invokedAgent, planner, args);
        }
        throw new IncompatibleClassChangeError();
    }

    private Object internalExecute(
            DefaultAgenticScope agenticScope, Object invokedAgent, PlannerExecutor planner, boolean async) {
        AgentInvocationArguments args = null;
        try {
            try {
                args = agentInvoker.toInvocationArguments(agenticScope);
            } catch (MissingArgumentException e) {
                if (optional()) {
                    LOG.info(
                            "Skipping optional agent '{}' because of missing argument '{}'",
                            agentInvoker.name(),
                            e.argumentName());
                    Object response = agenticScope.readState(agentInvoker.outputKey());
                    if (planner != null) {
                        planner.onSubagentInvoked(new AgentInvocation(type(), name(), agentId(), CollectionHelper.mapOf(), response));
                    }
                    return response;
                }
                throw e;
            }

            Object response = agentResponse(agenticScope, invokedAgent, planner, args, async);
            return completeAgentInvocation(response, agenticScope, invokedAgent, planner, args);
        } catch (AgenticSystemSuspendedException e) {
            if (planner != null) {
                planner.onSubagentSuspended();
            }
            return null;
        } catch (AgentInvocationException e) {
            return handleAgentFailure(e, agenticScope, invokedAgent, planner, args, false);
        }
    }

    private Object completeAgentInvocation(
            Object response,
            DefaultAgenticScope agenticScope,
            Object invokedAgent,
            PlannerExecutor planner,
            AgentInvocationArguments args) {
        String outputKey = agentInvoker.outputKey();
        if (outputKey != null && !StringHelper.isBlank(outputKey)) {
            agenticScope.writeState(outputKey, response);
        }
        Map<String, Object> namedArgs = args != null ? args.namedArgs() : CollectionHelper.mapOf();
        AgentInvocation agentInvocation = new AgentInvocation(
                type(), name(), agentId(), namedArgs, isSerializable(response) ? response : "<unknown>");
        agenticScope.registerAgentInvocation(agentInvocation, invokedAgent);
        if (planner != null) {
            planner.onSubagentInvoked(agentInvocation);
        }
        return response;
    }

    private Object agentResponse(
            DefaultAgenticScope agenticScope,
            Object invokedAgent,
            PlannerExecutor planner,
            AgentInvocationArguments args,
            boolean async) {
        if (async) {
            return new AsyncResponse<>(() -> {
                try {
                    return agentInvoker.invoke(agenticScope, invokedAgent, args);
                } catch (AgentInvocationException e) {
                    return handleAgentFailure(e, agenticScope, invokedAgent, planner, args, true);
                }
            });
        }

        Object response = agentInvoker.invoke(agenticScope, invokedAgent, args);
        if (planner != null && response instanceof TokenStream) {
            TokenStream tokenStream = (TokenStream) response;
            return planner.propagateStreaming() ? tokenStream : new StreamingResponse(tokenStream);
        }
        return response;
    }

    @Override
    public Class<?> type() {
        return agentInvoker.type();
    }

    @Override
    public Class<? extends Planner> plannerType() {
        return agentInvoker.plannerType();
    }

    @Override
    public String name() {
        return agentInvoker.name();
    }

    @Override
    public String agentId() {
        return agentInvoker.agentId();
    }

    @Override
    public String description() {
        return agentInvoker.description();
    }

    @Override
    public Type outputType() {
        return agentInvoker.outputType();
    }

    @Override
    public String outputKey() {
        return agentInvoker.outputKey();
    }

    @Override
    public List<AgentArgument> arguments() {
        return agentInvoker.arguments();
    }

    @Override
    public List<AgentInstance> subagents() {
        return agentInvoker.subagents();
    }

    @Override
    public boolean async() {
        return agentInvoker.async();
    }

    @Override
    public boolean optional() {
        return agentInvoker.optional();
    }

    @Override
    public AgenticSystemTopology topology() {
        return agentInvoker.topology();
    }

    @Override
    public AgentInstance parent() {
        return agentInvoker.parent();
    }

    @Override
    public void setParent(InternalAgent parent) {
        agentInvoker.setParent(parent);
    }

    @Override
    public boolean compensateOnError() {
        return agentInvoker.compensateOnError();
    }

    @Override
    public void enableCrossAgentCompensation() {
        agentInvoker.enableCrossAgentCompensation();
    }

    @Override
    public void registerInheritedParentListener(AgentListener parentListener) {
        agentInvoker.registerInheritedParentListener(parentListener);
    }

    @Override
    public void appendId(final String idSuffix) {
        agentInvoker.appendId(idSuffix);
    }

    @Override
    public AgentListener listener() {
        return agentInvoker.listener();
    }

    @Override
    public <T extends AgentInstance> T as(Class<T> agentInstanceClass) {
        return agentInvoker.as(agentInstanceClass);
    }

    void setParent(InternalAgent parent, int index) {
        setParent(parent);
        propagateParentIndex(agentInvoker, index);
    }

    private void propagateParentIndex(InternalAgent agent, int index) {
        agent.appendId("$" + index);
        for (AgentInstance subagent : agent.subagents()) {
            if (subagent instanceof InternalAgent) {
                InternalAgent internalAgent = (InternalAgent) subagent;
                propagateParentIndex(internalAgent, index);
            }
        }
    }
}
