package dev.langchain4j.agentic.workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.internal.AgentSpecsProvider;
import dev.langchain4j.agentic.internal.CollectionHelper;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.planner.AgentArgument;
import dev.langchain4j.agentic.scope.AgenticScope;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class HumanInTheLoop implements AgentSpecsProvider {

    private final String outputKey;
    private final String description;
    private final boolean async;
    private final Function<AgenticScope, ?> responseProvider;
    private final AgentListener listener;
    private final List<AgentArgument> arguments;

    public HumanInTheLoop(
            String outputKey,
            String description,
            boolean async,
            Function<AgenticScope, ?> responseProvider,
            AgentListener listener,
            List<AgentArgument> arguments) {
        this.outputKey = outputKey;
        this.description = description;
        this.async = async;
        this.responseProvider = responseProvider;
        this.listener = listener;
        this.arguments = arguments;
    }

    @Override
    public String outputKey() {
        return outputKey;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public boolean async() {
        return async;
    }

    public Function<AgenticScope, ?> responseProvider() {
        return responseProvider;
    }

    @Override
    public AgentListener listener() {
        return listener;
    }

    @Override
    public List<AgentArgument> arguments() {
        return arguments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HumanInTheLoop)) return false;
        HumanInTheLoop that = (HumanInTheLoop) o;
        return async == that.async
                && Objects.equals(outputKey, that.outputKey)
                && Objects.equals(description, that.description)
                && Objects.equals(responseProvider, that.responseProvider)
                && Objects.equals(listener, that.listener)
                && Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash = 31 * hash + (outputKey == null ? 0 : outputKey.hashCode());
        hash = 31 * hash + (description == null ? 0 : description.hashCode());
        hash = 31 * hash + Boolean.hashCode(async);
        hash = 31 * hash + (responseProvider == null ? 0 : responseProvider.hashCode());
        hash = 31 * hash + (listener == null ? 0 : listener.hashCode());
        hash = 31 * hash + (arguments == null ? 0 : arguments.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return "HumanInTheLoop[outputKey=" + outputKey
                + ", description=" + description
                + ", async=" + async
                + ", responseProvider=" + responseProvider
                + ", listener=" + listener
                + ", arguments=" + arguments + "]";
    }

    @Agent("An agent that asks the user for missing information")
    public Object askUser(AgenticScope scope) {
        return responseProvider.apply(scope);
    }

    public static class HumanInTheLoopBuilder {

        private String outputKey = "response";
        private String description = "An agent that asks the user for missing information";
        private boolean async = false;
        private Function<AgenticScope, ?> responseProvider;
        private AgentListener agentListener;
        private List<AgentArgument> arguments;

        public HumanInTheLoopBuilder responseProvider(Supplier<?> responseProvider) {
            return responseProvider(scope -> responseProvider.get());
        }

        public HumanInTheLoopBuilder responseProvider(Function<AgenticScope, ?> responseProvider) {
            this.responseProvider = responseProvider;
            return this;
        }

        public HumanInTheLoopBuilder outputKey(String outputKey) {
            this.outputKey = outputKey;
            return this;
        }

        public HumanInTheLoopBuilder description(String description) {
            this.description = description;
            return this;
        }

        public HumanInTheLoopBuilder async(boolean async) {
            this.async = async;
            return this;
        }

        public HumanInTheLoopBuilder inputs(List<AgentArgument> inputs) {
            this.arguments = inputs;
            return this;
        }

        public HumanInTheLoopBuilder inputKey(Class<?> type, String name) {
            this.arguments = CollectionHelper.listOf(new AgentArgument(type, name));
            return this;
        }

        public HumanInTheLoopBuilder inputKeys(Class<?> type1, String name1, Class<?> type2, String name2) {
            this.arguments = CollectionHelper.listOf(new AgentArgument(type1, name1), new AgentArgument(type2, name2));
            return this;
        }

        public HumanInTheLoopBuilder inputKeys(Class<?> type1, String name1, Class<?> type2, String name2,
                                                Class<?> type3, String name3) {
            this.arguments = CollectionHelper.listOf(new AgentArgument(type1, name1), new AgentArgument(type2, name2),
                    new AgentArgument(type3, name3));
            return this;
        }

        public HumanInTheLoopBuilder inputKeys(Class<?> type1, String name1, Class<?> type2, String name2,
                                                Class<?> type3, String name3, Class<?> type4, String name4) {
            this.arguments = CollectionHelper.listOf(new AgentArgument(type1, name1), new AgentArgument(type2, name2),
                    new AgentArgument(type3, name3), new AgentArgument(type4, name4));
            return this;
        }

        public HumanInTheLoopBuilder listener(AgentListener agentListener) {
            this.agentListener = agentListener;
            return this;
        }

        public HumanInTheLoop build() {
            return new HumanInTheLoop(outputKey, description, async, responseProvider, agentListener, arguments);
        }
    }
}
