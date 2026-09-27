package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class LangChain4jEmbeddingStoreProviderSpec {

    enum CreationKind {
        AUTO,
        BUILDER,
        CONSTRUCTOR,
        RAW_CONSTRUCTOR_ARGS
    }

    enum ValueKind {
        PROPERTY,
        BEAN,
        CONSTANT
    }

    enum ConditionKind {
        ALWAYS,
        PRESENT,
        ABSENT,
        DEFINED,
        UNDEFINED,
        ALL,
        ANY
    }

    private final Set<String> providers;
    private final String storeClassName;
    private final List<String> requiredClassNames;
    private final List<Requirement> requirements;
    private final List<CreationPlan> creationPlans;

    private LangChain4jEmbeddingStoreProviderSpec(Builder builder) {
        this.providers = immutableSet(builder.providers);
        this.storeClassName = requireText(builder.storeClassName, "storeClassName");
        this.requiredClassNames = immutableList(builder.requiredClassNames);
        this.requirements = immutableList(builder.requirements);
        this.creationPlans = immutableList(builder.creationPlans);
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("At least one provider is required");
        }
        if (creationPlans.isEmpty()) {
            throw new IllegalArgumentException("At least one creation plan is required: provider=" + providers);
        }
        validateCreationPlans();
    }

    private void validateCreationPlans() {
        for (int first = 0; first < creationPlans.size(); first++) {
            for (int second = first + 1; second < creationPlans.size(); second++) {
                if (overlaps(creationPlans.get(first).getCondition(),
                        creationPlans.get(second).getCondition())) {
                    throw new IllegalArgumentException("Overlapping creation plans: provider=" + providers
                            + ", first=" + first + ", second=" + second);
                }
            }
        }
    }

    private boolean overlaps(Condition first, Condition second) {
        for (Map<String, Integer> firstBranch : branches(first)) {
            for (Map<String, Integer> secondBranch : branches(second)) {
                if (compatible(firstBranch, secondBranch)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Map<String, Integer>> branches(Condition condition) {
        switch (condition.getKind()) {
            case ALWAYS:
                return Collections.singletonList(Collections.<String, Integer>emptyMap());
            case PRESENT:
                return singletonBranch(condition.getProperty(), 4);
            case ABSENT:
                return singletonBranch(condition.getProperty(), 1 | 2);
            case DEFINED:
                return singletonBranch(condition.getProperty(), 2 | 4);
            case UNDEFINED:
                return singletonBranch(condition.getProperty(), 1);
            case ANY:
                List<Map<String, Integer>> alternatives = new ArrayList<Map<String, Integer>>();
                for (Condition child : condition.getChildren()) {
                    alternatives.addAll(branches(child));
                }
                return alternatives;
            case ALL:
                List<Map<String, Integer>> combined =
                        Collections.singletonList(Collections.<String, Integer>emptyMap());
                for (Condition child : condition.getChildren()) {
                    combined = combine(combined, branches(child));
                }
                return combined;
            default:
                throw new IllegalArgumentException("Unsupported condition: " + condition.getKind());
        }
    }

    private List<Map<String, Integer>> singletonBranch(String property, int states) {
        Map<String, Integer> branch = new LinkedHashMap<String, Integer>();
        branch.put(property, states);
        return Collections.singletonList(branch);
    }

    private List<Map<String, Integer>> combine(List<Map<String, Integer>> left,
                                               List<Map<String, Integer>> right) {
        List<Map<String, Integer>> result = new ArrayList<Map<String, Integer>>();
        for (Map<String, Integer> first : left) {
            for (Map<String, Integer> second : right) {
                Map<String, Integer> merged = merge(first, second);
                if (merged != null) {
                    result.add(merged);
                }
            }
        }
        return result;
    }

    private Map<String, Integer> merge(Map<String, Integer> first,
                                       Map<String, Integer> second) {
        Map<String, Integer> merged = new LinkedHashMap<String, Integer>(first);
        for (Map.Entry<String, Integer> entry : second.entrySet()) {
            Integer existing = merged.get(entry.getKey());
            int states = existing == null ? entry.getValue().intValue()
                    : existing.intValue() & entry.getValue().intValue();
            if (states == 0) {
                return null;
            }
            merged.put(entry.getKey(), Integer.valueOf(states));
        }
        return merged;
    }

    private boolean compatible(Map<String, Integer> first, Map<String, Integer> second) {
        return merge(first, second) != null;
    }

    static Builder builder(String provider, String storeClassName, String... aliases) {
        return new Builder(provider, storeClassName, aliases);
    }

    static Condition always() {
        return new Condition(ConditionKind.ALWAYS, null, Collections.<Condition>emptyList());
    }

    static Condition present(String property) {
        return new Condition(ConditionKind.PRESENT, requireText(property, "property"),
                Collections.<Condition>emptyList());
    }

    static Condition absent(String property) {
        return new Condition(ConditionKind.ABSENT, requireText(property, "property"),
                Collections.<Condition>emptyList());
    }

    static Condition defined(String property) {
        return new Condition(ConditionKind.DEFINED, requireText(property, "property"),
                Collections.<Condition>emptyList());
    }

    static Condition undefined(String property) {
        return new Condition(ConditionKind.UNDEFINED, requireText(property, "property"),
                Collections.<Condition>emptyList());
    }

    static Condition all(Condition... conditions) {
        return compound(ConditionKind.ALL, conditions);
    }

    static Condition any(Condition... conditions) {
        return compound(ConditionKind.ANY, conditions);
    }

    static Value property(String... sources) {
        return new Value(ValueKind.PROPERTY, sources, null);
    }

    static Value bean(String... sources) {
        return new Value(ValueKind.BEAN, sources, null);
    }

    static Value constant(Object value) {
        return new Value(ValueKind.CONSTANT, new String[0], value);
    }

    Set<String> getProviders() {
        return providers;
    }

    String getStoreClassName() {
        return storeClassName;
    }

    List<String> getRequiredClassNames() {
        return requiredClassNames;
    }

    List<Requirement> getRequirements() {
        return requirements;
    }

    List<CreationPlan> getCreationPlans() {
        return creationPlans;
    }

    private static Condition compound(ConditionKind kind, Condition... conditions) {
        if (conditions == null || conditions.length == 0) {
            throw new IllegalArgumentException(kind + " condition requires children");
        }
        return new Condition(kind, null, immutableList(Arrays.asList(conditions)));
    }

    private static String normalize(String provider) {
        return requireText(provider, "provider").toLowerCase(Locale.ENGLISH);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static <T> List<T> immutableList(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<T>(values));
    }

    private static <T> Set<T> immutableSet(Set<T> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<T>(values));
    }

    static final class Builder {

        private final Set<String> providers = new LinkedHashSet<String>();
        private final String storeClassName;
        private final List<String> requiredClassNames = new ArrayList<String>();
        private final List<Requirement> requirements = new ArrayList<Requirement>();
        private final List<CreationPlan> creationPlans = new ArrayList<CreationPlan>();

        private Builder(String provider, String storeClassName, String... aliases) {
            addProvider(provider);
            if (aliases != null) {
                for (String alias : aliases) {
                    addProvider(alias);
                }
            }
            this.storeClassName = requireText(storeClassName, "storeClassName");
            this.requiredClassNames.add(this.storeClassName);
        }

        Builder requiredClasses(String... classNames) {
            if (classNames != null) {
                for (String className : classNames) {
                    String required = requireText(className, "requiredClassName");
                    if (!requiredClassNames.contains(required)) {
                        requiredClassNames.add(required);
                    }
                }
            }
            return this;
        }

        Builder require(String... properties) {
            requirements.add(Requirement.all(always(), properties));
            return this;
        }

        Builder requireAny(String... properties) {
            requirements.add(Requirement.any(always(), properties));
            return this;
        }

        Builder requireWhen(Condition condition, String... properties) {
            requirements.add(Requirement.all(condition, properties));
            return this;
        }

        Builder auto() {
            creationPlans.add(CreationPlan.auto(always()));
            return this;
        }

        PlanBuilder builderPlan() {
            return new PlanBuilder(this, CreationKind.BUILDER, always(), "builder", "build");
        }

        PlanBuilder builderPlan(Condition condition) {
            return new PlanBuilder(this, CreationKind.BUILDER, condition, "builder", "build");
        }

        Builder constructorPlan(Value... arguments) {
            return constructorPlan(always(), arguments);
        }

        Builder constructorPlan(Condition condition, Value... arguments) {
            creationPlans.add(CreationPlan.constructor(condition, arguments));
            return this;
        }

        Builder rawConstructorArgsPlan(Condition condition) {
            creationPlans.add(CreationPlan.rawConstructorArgs(condition));
            return this;
        }

        LangChain4jEmbeddingStoreProviderSpec build() {
            return new LangChain4jEmbeddingStoreProviderSpec(this);
        }

        private void addProvider(String provider) {
            String normalized = normalize(provider);
            if (!providers.add(normalized)) {
                throw new IllegalArgumentException("Duplicate provider alias: " + normalized);
            }
        }
    }

    static final class PlanBuilder {

        private final Builder owner;
        private final CreationKind kind;
        private final Condition condition;
        private final String factoryMethod;
        private final String terminalMethod;
        private final List<Invocation> invocations = new ArrayList<Invocation>();
        private boolean passthrough;

        private PlanBuilder(Builder owner, CreationKind kind, Condition condition,
                            String factoryMethod, String terminalMethod) {
            this.owner = owner;
            this.kind = kind;
            this.condition = condition == null ? always() : condition;
            this.factoryMethod = factoryMethod;
            this.terminalMethod = terminalMethod;
        }

        PlanBuilder property(String target, String... sources) {
            invocations.add(Invocation.optional(target,
                    LangChain4jEmbeddingStoreProviderSpec.property(sources)));
            return this;
        }

        PlanBuilder beanProperty(String target, String... sources) {
            invocations.add(Invocation.optional(target,
                    LangChain4jEmbeddingStoreProviderSpec.bean(sources)));
            return this;
        }

        PlanBuilder invoke(String method, Condition condition, Value... arguments) {
            invocations.add(new Invocation(requireText(method, "method"),
                    condition == null ? always() : condition, immutableList(Arrays.asList(arguments))));
            return this;
        }

        PlanBuilder passthrough() {
            this.passthrough = true;
            return this;
        }

        Builder end() {
            owner.creationPlans.add(new CreationPlan(kind, condition, factoryMethod, terminalMethod,
                    immutableList(invocations), Collections.<Value>emptyList(), passthrough));
            return owner;
        }
    }

    static final class Requirement {

        private final Condition condition;
        private final boolean any;
        private final List<String> properties;

        private Requirement(Condition condition, boolean any, String... properties) {
            this.condition = condition == null ? always() : condition;
            this.any = any;
            if (properties == null || properties.length == 0) {
                throw new IllegalArgumentException("Requirement properties must not be empty");
            }
            List<String> copy = new ArrayList<String>();
            for (String property : properties) {
                String name = requireText(property, "requiredProperty");
                if (!copy.contains(name)) {
                    copy.add(name);
                }
            }
            this.properties = immutableList(copy);
        }

        static Requirement all(Condition condition, String... properties) {
            return new Requirement(condition, false, properties);
        }

        static Requirement any(Condition condition, String... properties) {
            return new Requirement(condition, true, properties);
        }

        Condition getCondition() {
            return condition;
        }

        boolean isAny() {
            return any;
        }

        List<String> getProperties() {
            return properties;
        }
    }

    static final class CreationPlan {

        private final CreationKind kind;
        private final Condition condition;
        private final String factoryMethod;
        private final String terminalMethod;
        private final List<Invocation> invocations;
        private final List<Value> constructorArguments;
        private final boolean passthrough;

        private CreationPlan(CreationKind kind, Condition condition, String factoryMethod,
                             String terminalMethod, List<Invocation> invocations,
                             List<Value> constructorArguments, boolean passthrough) {
            this.kind = kind;
            this.condition = condition == null ? always() : condition;
            this.factoryMethod = factoryMethod;
            this.terminalMethod = terminalMethod;
            this.invocations = immutableList(invocations);
            this.constructorArguments = immutableList(constructorArguments);
            this.passthrough = passthrough;
        }

        static CreationPlan auto(Condition condition) {
            return new CreationPlan(CreationKind.AUTO, condition, "builder", "build",
                    Collections.<Invocation>emptyList(), Collections.<Value>emptyList(), true);
        }

        static CreationPlan constructor(Condition condition, Value... arguments) {
            return new CreationPlan(CreationKind.CONSTRUCTOR, condition, null, null,
                    Collections.<Invocation>emptyList(), immutableList(Arrays.asList(arguments)), false);
        }

        static CreationPlan rawConstructorArgs(Condition condition) {
            return new CreationPlan(CreationKind.RAW_CONSTRUCTOR_ARGS, condition, null, null,
                    Collections.<Invocation>emptyList(), Collections.<Value>emptyList(), false);
        }

        CreationKind getKind() {
            return kind;
        }

        Condition getCondition() {
            return condition;
        }

        String getFactoryMethod() {
            return factoryMethod;
        }

        String getTerminalMethod() {
            return terminalMethod;
        }

        List<Invocation> getInvocations() {
            return invocations;
        }

        List<Value> getConstructorArguments() {
            return constructorArguments;
        }

        boolean isPassthrough() {
            return passthrough;
        }
    }

    static final class Invocation {

        private final String method;
        private final Condition condition;
        private final List<Value> arguments;

        private Invocation(String method, Condition condition, List<Value> arguments) {
            this.method = method;
            this.condition = condition;
            this.arguments = arguments;
        }

        static Invocation optional(String method, Value argument) {
            return new Invocation(requireText(method, "method"), anyPresent(argument),
                    Collections.singletonList(argument));
        }

        String getMethod() {
            return method;
        }

        Condition getCondition() {
            return condition;
        }

        List<Value> getArguments() {
            return arguments;
        }

        private static Condition anyPresent(Value value) {
            if (value.kind == ValueKind.CONSTANT) {
                return always();
            }
            List<Condition> conditions = new ArrayList<Condition>();
            for (String source : value.sources) {
                conditions.add(defined(source));
            }
            return conditions.size() == 1 ? conditions.get(0)
                    : any(conditions.toArray(new Condition[conditions.size()]));
        }
    }

    static final class Value {

        private final ValueKind kind;
        private final List<String> sources;
        private final Object constant;

        private Value(ValueKind kind, String[] sources, Object constant) {
            this.kind = kind;
            List<String> copy = new ArrayList<String>();
            if (sources != null) {
                for (String source : sources) {
                    copy.add(requireText(source, "valueSource"));
                }
            }
            if (kind != ValueKind.CONSTANT && copy.isEmpty()) {
                throw new IllegalArgumentException(kind + " value requires at least one source");
            }
            this.sources = immutableList(copy);
            this.constant = constant;
        }

        ValueKind getKind() {
            return kind;
        }

        List<String> getSources() {
            return sources;
        }

        Object getConstant() {
            return constant;
        }
    }

    static final class Condition {

        private final ConditionKind kind;
        private final String property;
        private final List<Condition> children;

        private Condition(ConditionKind kind, String property, List<Condition> children) {
            this.kind = kind;
            this.property = property;
            this.children = children;
        }

        ConditionKind getKind() {
            return kind;
        }

        String getProperty() {
            return property;
        }

        List<Condition> getChildren() {
            return children;
        }
    }
}
