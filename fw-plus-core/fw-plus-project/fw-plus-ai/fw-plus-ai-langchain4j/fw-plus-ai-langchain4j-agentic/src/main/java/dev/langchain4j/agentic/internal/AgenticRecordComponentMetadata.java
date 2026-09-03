package dev.langchain4j.agentic.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Explicit metadata for the Agentic value types that were records upstream.
 *
 * <p>The registry is keyed by binary class name rather than {@link Class} so that it does not
 * retain application class loaders. A lookup validates the class supplied by the current
 * application before returning metadata; a class with a registered name but a changed accessor or
 * type therefore fails instead of silently taking the ordinary-field path.</p>
 */
public final class AgenticRecordComponentMetadata {

    private static final Map<String, TypeMetadata> TYPES = createTypes();

    private AgenticRecordComponentMetadata() {
    }

    /**
     * Looks up and validates a registered Agentic type.
     *
     * @param type the class loaded by the current application class loader
     * @return {@code true} when explicit metadata exists; {@code false} for an ordinary type
     */
    public static boolean lookup(Class<?> type) {
        return metadata(type) != null;
    }

    /**
     * Describes the registered components, or returns {@code null} for an ordinary type.
     *
     * @param type the class loaded by the current application class loader
     * @param descriptionFunction recursive description callback supplied by SupervisorPlanner
     * @return component description, or {@code null} when the type is not registered
     */
    public static String describe(Class<?> type, BiFunction<Class<?>, String, String> descriptionFunction) {
        TypeMetadata metadata = metadata(type);
        return metadata == null ? null : metadata.describe(type, descriptionFunction);
    }

    private static TypeMetadata metadata(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("Agentic type must not be null");
        }
        TypeMetadata metadata = TYPES.get(type.getName());
        if (metadata != null) {
            metadata.validate(type);
        }
        return metadata;
    }

    /** Returns the immutable set of registered binary class names for internal verification. */
    static Set<String> registeredTypeNames() {
        return TYPES.keySet();
    }

    /** Returns the number of explicit entries for internal verification. */
    static int registeredTypeCount() {
        return TYPES.size();
    }

    private static Map<String, TypeMetadata> createTypes() {
        Map<String, TypeMetadata> types = new LinkedHashMap<String, TypeMetadata>();
        put(types, "dev.langchain4j.agentic.AgenticServices$DefaultDeclarativeAgentCreationContext",
                component("agentServiceClass", "java.lang.Class", "java.lang.Class<T>", "agentServiceClass", "public"),
                component("agentBuilder", "dev.langchain4j.agentic.agent.AgentBuilder",
                        "dev.langchain4j.agentic.agent.AgentBuilder<T, ?>", "agentBuilder", "public"));
        put(types, "dev.langchain4j.agentic.AgenticServices$AgentConfigurator",
                component("configurator", "java.util.function.Consumer",
                        "java.util.function.Consumer<dev.langchain4j.agentic.AgenticServices$DeclarativeAgentCreationContext<?>>",
                        "configurator", "public"),
                component("subAgentResolver", "java.util.function.Function",
                        "java.util.function.Function<java.lang.Class<?>, java.lang.Object>", "subAgentResolver", "public"),
                component("agentInstanceFactory", "java.util.function.Function",
                        "java.util.function.Function<dev.langchain4j.agentic.internal.InternalAgent, java.lang.Object>",
                        "agentInstanceFactory", "public"),
                component("defaultMemoryIdSupplier", "java.util.function.Supplier",
                        "java.util.function.Supplier<java.lang.Object>", "defaultMemoryIdSupplier", "public"));
        put(types, "dev.langchain4j.agentic.observability.BeforeAgentToolExecution",
                component("agentInstance", "dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.agentic.planner.AgentInstance", "agentInstance", "public"),
                component("toolExecution", "dev.langchain4j.service.tool.BeforeToolExecution",
                        "dev.langchain4j.service.tool.BeforeToolExecution", "toolExecution", "public"));
        put(types, "dev.langchain4j.agentic.agent.ErrorContext",
                component("agentName", "java.lang.String", "java.lang.String", "agentName", "public"),
                component("agenticScope", "dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.scope.AgenticScope", "agenticScope", "public"),
                component("exception", "dev.langchain4j.agentic.agent.AgentInvocationException",
                        "dev.langchain4j.agentic.agent.AgentInvocationException", "exception", "public"));
        put(types, "dev.langchain4j.agentic.scope.AgentInvocation",
                component("agentType", "java.lang.Class", "java.lang.Class<?>", "agentType", "public"),
                component("agentName", "java.lang.String", "java.lang.String", "agentName", "public"),
                component("agentId", "java.lang.String", "java.lang.String", "agentId", "public"),
                component("input", "java.util.Map", "java.util.Map<java.lang.String, java.lang.Object>", "input", "public"),
                component("output", "java.lang.Object", "java.lang.Object", "output", "public"));
        put(types, "dev.langchain4j.agentic.planner.InitPlanningContext",
                component("agenticScope", "dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.scope.AgenticScope", "agenticScope", "public"),
                component("plannerAgent", "dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.agentic.planner.AgentInstance", "plannerAgent", "public"),
                component("subagents", "java.util.List",
                        "java.util.List<dev.langchain4j.agentic.planner.AgentInstance>", "subagents", "public"));
        put(types, "dev.langchain4j.agentic.workflow.HumanInTheLoop",
                component("outputKey", "java.lang.String", "java.lang.String", "outputKey", "public"),
                component("description", "java.lang.String", "java.lang.String", "description", "public"),
                component("async", "boolean", "boolean", "async", "public"),
                component("responseProvider", "java.util.function.Function",
                        "java.util.function.Function<dev.langchain4j.agentic.scope.AgenticScope, ?>", "responseProvider", "public"),
                component("listener", "dev.langchain4j.agentic.observability.AgentListener",
                        "dev.langchain4j.agentic.observability.AgentListener", "listener", "public"),
                component("arguments", "java.util.List",
                        "java.util.List<dev.langchain4j.agentic.planner.AgentArgument>", "arguments", "public"));
        put(types, "dev.langchain4j.agentic.observability.AgentRequest",
                component("agenticScope", "dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.scope.AgenticScope", "agenticScope", "public"),
                component("agent", "dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.agentic.planner.AgentInstance", "agent", "public"),
                component("inputs", "java.util.Map", "java.util.Map<java.lang.String, java.lang.Object>", "inputs", "public"));
        put(types, "dev.langchain4j.agentic.observability.AgentInvocationError",
                component("agenticScope", "dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.scope.AgenticScope", "agenticScope", "public"),
                component("agent", "dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.agentic.planner.AgentInstance", "agent", "public"),
                component("inputs", "java.util.Map", "java.util.Map<java.lang.String, java.lang.Object>", "inputs", "public"),
                component("error", "java.lang.Throwable", "java.lang.Throwable", "error", "public"));
        put(types, "dev.langchain4j.agentic.observability.AgentResponse",
                component("agenticScope", "dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.scope.AgenticScope", "agenticScope", "public"),
                component("agent", "dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.agentic.planner.AgentInstance", "agent", "public"),
                component("inputs", "java.util.Map", "java.util.Map<java.lang.String, java.lang.Object>", "inputs", "public"),
                component("output", "java.lang.Object", "java.lang.Object", "output", "public"),
                component("chatRequest", "dev.langchain4j.model.chat.request.ChatRequest",
                        "dev.langchain4j.model.chat.request.ChatRequest", "chatRequest", "public"),
                component("chatResponse", "dev.langchain4j.model.chat.response.ChatResponse",
                        "dev.langchain4j.model.chat.response.ChatResponse", "chatResponse", "public"));
        put(types, "dev.langchain4j.agentic.planner.AgentArgument",
                component("type", "java.lang.reflect.Type", "java.lang.reflect.Type", "type", "public"),
                component("name", "java.lang.String", "java.lang.String", "name", "public"),
                component("defaultValue", "java.lang.Object", "java.lang.Object", "defaultValue", "public"),
                component("isOptional", "boolean", "boolean", "isOptional", "public"));
        put(types, "dev.langchain4j.agentic.agent.ErrorRecoveryResult",
                component("type", "dev.langchain4j.agentic.agent.ErrorRecoveryResult$Type",
                        "dev.langchain4j.agentic.agent.ErrorRecoveryResult$Type", "type", "public"),
                component("result", "java.lang.Object", "java.lang.Object", "result", "public"));
        put(types, "dev.langchain4j.agentic.internal.AgentInvocationArguments",
                component("namedArgs", "java.util.Map", "java.util.Map<java.lang.String, java.lang.Object>", "namedArgs", "public"),
                component("positionalArgs", "java.lang.Object[]", "java.lang.Object[]", "positionalArgs", "public"));
        put(types, "dev.langchain4j.agentic.observability.HtmlReportGenerator",
                component("monitor", "dev.langchain4j.agentic.observability.AgentMonitor",
                        "dev.langchain4j.agentic.observability.AgentMonitor", "monitor", "public"),
                component("rootAgent", "dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.agentic.planner.AgentInstance", "rootAgent", "public"),
                component("memoryId", "java.lang.Object", "java.lang.Object", "memoryId", "public"));
        put(types, "dev.langchain4j.agentic.internal.AgentExecutor",
                component("agentInvoker", "dev.langchain4j.agentic.internal.AgentInvoker",
                        "dev.langchain4j.agentic.internal.AgentInvoker", "agentInvoker", "public"),
                component("agent", "java.lang.Object", "java.lang.Object", "agent", "public"));
        put(types, "dev.langchain4j.agentic.scope.DefaultAgenticScope$AgentMessage",
                component("agentName", "java.lang.String", "java.lang.String", "agentName", "public"),
                component("agentId", "java.lang.String", "java.lang.String", "agentId", "public"),
                component("message", "dev.langchain4j.data.message.ChatMessage",
                        "dev.langchain4j.data.message.ChatMessage", "message", "public"));
        put(types, "dev.langchain4j.agentic.scope.DefaultAgenticScope$CompensableExecution",
                component("toolExecution", "dev.langchain4j.service.tool.ToolExecution",
                        "dev.langchain4j.service.tool.ToolExecution", "toolExecution", "public"),
                component("compensatingAction", "java.util.function.Consumer",
                        "java.util.function.Consumer<dev.langchain4j.service.tool.ToolExecution>", "compensatingAction", "public"));
        put(types, "dev.langchain4j.agentic.scope.AgenticScopeKey",
                component("agentId", "java.lang.String", "java.lang.String", "agentId", "public"),
                component("memoryId", "java.lang.Object", "java.lang.Object", "memoryId", "public"));
        put(types, "dev.langchain4j.agentic.observability.AfterAgentToolExecution",
                component("agentInstance", "dev.langchain4j.agentic.planner.AgentInstance",
                        "dev.langchain4j.agentic.planner.AgentInstance", "agentInstance", "public"),
                component("toolExecution", "dev.langchain4j.service.tool.ToolExecution",
                        "dev.langchain4j.service.tool.ToolExecution", "toolExecution", "public"));
        put(types, "dev.langchain4j.agentic.declarative.DeclarativeUtil$DefaultSupplierParameterResolverContext",
                component("declaringAgentClass", "java.lang.Class", "java.lang.Class<?>", "declaringAgentClass", "public"),
                component("supplierMethod", "java.lang.reflect.Method", "java.lang.reflect.Method", "supplierMethod", "public"),
                component("parameter", "java.lang.reflect.Parameter", "java.lang.reflect.Parameter", "parameter", "public"));
        put(types, "dev.langchain4j.agentic.workflow.ConditionalAgent",
                component("condition", "java.lang.String", "java.lang.String", "condition", "public"),
                component("predicate", "java.util.function.Predicate",
                        "java.util.function.Predicate<dev.langchain4j.agentic.scope.AgenticScope>", "predicate", "public"),
                component("agentInstances", "java.util.List",
                        "java.util.List<dev.langchain4j.agentic.planner.AgentInstance>", "agentInstances", "public"));
        put(types, "dev.langchain4j.agentic.workflow.impl.ConditionalPlanner",
                component("conditionalSubagents", "java.util.List",
                        "java.util.List<dev.langchain4j.agentic.workflow.ConditionalAgent>", "conditionalSubagents", "public"));
        put(types, "dev.langchain4j.agentic.planner.PlanningContext",
                component("agenticScope", "dev.langchain4j.agentic.scope.AgenticScope",
                        "dev.langchain4j.agentic.scope.AgenticScope", "agenticScope", "public"),
                component("previousAgentInvocation", "dev.langchain4j.agentic.scope.AgentInvocation",
                        "dev.langchain4j.agentic.scope.AgentInvocation", "previousAgentInvocation", "public"));
        return Collections.unmodifiableMap(types);
    }

    private static void put(Map<String, TypeMetadata> types, String binaryName, ComponentMetadata... components) {
        if (types.put(binaryName, new TypeMetadata(binaryName, Arrays.asList(components))) != null) {
            throw new IllegalStateException("Duplicate Agentic metadata: " + binaryName);
        }
    }

    private static ComponentMetadata component(String name, String rawTypeName, String genericTypeName,
                                               String accessorName, String accessorVisibility) {
        return new ComponentMetadata(name, rawTypeName, genericTypeName, accessorName, accessorVisibility);
    }

    static final class TypeMetadata {
        private final String binaryName;
        private final List<ComponentMetadata> components;

        private TypeMetadata(String binaryName, List<ComponentMetadata> components) {
            this.binaryName = binaryName;
            this.components = Collections.unmodifiableList(new ArrayList<ComponentMetadata>(components));
        }

        String binaryName() {
            return binaryName;
        }

        List<ComponentMetadata> components() {
            return components;
        }

        /**
         * Builds the supervisor's component description using the raw component types resolved
         * from the supplied class loader. The callback is deliberately supplied by the caller so
         * recursion remains in SupervisorPlanner rather than being duplicated here.
         */
        public String describe(Class<?> ownerType, BiFunction<Class<?>, String, String> descriptionFunction) {
            validate(ownerType);
            List<String> descriptions = new ArrayList<String>(components.size());
            for (ComponentMetadata component : components) {
                Class<?> rawType = resolveRawType(ownerType, component);
                if (TYPES.containsKey(rawType.getName())) {
                    descriptions.add(descriptionFunction.apply(rawType, component.name()));
                } else {
                    descriptions.add(component.name() + ": " + rawType.getSimpleName());
                }
            }
            return join(descriptions);
        }

        private void validate(Class<?> actualType) {
            if (!binaryName.equals(actualType.getName())) {
                throw new IllegalStateException("Agentic metadata owner mismatch: " + binaryName
                        + " (actual " + actualType.getName() + ")");
            }
            for (ComponentMetadata component : components) {
                Method accessor;
                try {
                    accessor = actualType.getDeclaredMethod(component.accessorName);
                } catch (NoSuchMethodException e) {
                    throw drift(actualType, component, "missing accessor", e);
                }
                String actualVisibility = Modifier.isPublic(accessor.getModifiers()) ? "public"
                        : Modifier.isProtected(accessor.getModifiers()) ? "protected"
                        : Modifier.isPrivate(accessor.getModifiers()) ? "private" : "package";
                if (!component.accessorVisibility.equals(actualVisibility) || Modifier.isStatic(accessor.getModifiers())) {
                    throw drift(actualType, component, "accessor visibility expected "
                            + component.accessorVisibility + " but was " + actualVisibility
                            + " and must be an instance method", null);
                }
                String actualRawType = sourceTypeName(accessor.getReturnType());
                if (!component.rawTypeName.equals(actualRawType)) {
                    throw drift(actualType, component, "raw type expected " + component.rawTypeName
                            + " but was " + actualRawType, null);
                }
                Type actualGenericType = accessor.getGenericReturnType();
                String actualGenericTypeName = actualGenericType.getTypeName();
                if (!component.genericTypeName.equals(actualGenericTypeName)) {
                    throw drift(actualType, component, "generic type expected " + component.genericTypeName
                            + " but was " + actualGenericTypeName, null);
                }
            }
        }

        private static IllegalStateException drift(Class<?> actualType, ComponentMetadata component,
                                                   String reason, Exception cause) {
            String message = "Agentic metadata drift for " + actualType.getName() + "." + component.name
                    + ": " + reason;
            return cause == null ? new IllegalStateException(message) : new IllegalStateException(message, cause);
        }

        private static Class<?> resolveRawType(Class<?> ownerType, ComponentMetadata component) {
            String name = component.rawTypeName;
            if ("boolean".equals(name)) return Boolean.TYPE;
            if ("byte".equals(name)) return Byte.TYPE;
            if ("char".equals(name)) return Character.TYPE;
            if ("short".equals(name)) return Short.TYPE;
            if ("int".equals(name)) return Integer.TYPE;
            if ("long".equals(name)) return Long.TYPE;
            if ("float".equals(name)) return Float.TYPE;
            if ("double".equals(name)) return Double.TYPE;
            if ("void".equals(name)) return Void.TYPE;
            try {
                if (name.endsWith("[]")) {
                    Class<?> componentType = resolveClass(ownerType, name.substring(0, name.length() - 2));
                    return Array.newInstance(componentType, 0).getClass();
                }
                return resolveClass(ownerType, name);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Agentic metadata type cannot be resolved for "
                        + ownerType.getName() + "." + component.name + ": " + name, e);
            }
        }

        private static Class<?> resolveClass(Class<?> ownerType, String name) throws ClassNotFoundException {
            ClassLoader loader = ownerType.getClassLoader();
            return Class.forName(name, false, loader);
        }

        private static String join(List<String> values) {
            StringBuilder result = new StringBuilder();
            for (String value : values) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                result.append(value);
            }
            return result.toString();
        }
    }

    static final class ComponentMetadata {
        private final String name;
        private final String rawTypeName;
        private final String genericTypeName;
        private final String accessorName;
        private final String accessorVisibility;

        private ComponentMetadata(String name, String rawTypeName, String genericTypeName, String accessorName,
                                  String accessorVisibility) {
            this.name = name;
            this.rawTypeName = rawTypeName;
            this.genericTypeName = genericTypeName;
            this.accessorName = accessorName;
            this.accessorVisibility = accessorVisibility;
        }

        String name() {
            return name;
        }

        String rawTypeName() {
            return rawTypeName;
        }

        String genericTypeName() {
            return genericTypeName;
        }

        String accessorName() {
            return accessorName;
        }

        String accessorVisibility() {
            return accessorVisibility;
        }
    }

    private static String sourceTypeName(Class<?> type) {
        if (!type.isArray()) {
            return type.getName();
        }
        return sourceTypeName(type.getComponentType()) + "[]";
    }
}
