package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.UntypedAgent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 隔离层守卫：验证业务调用方仅用框架自有类型 + {@link UntypedAgent} 句柄即可完成编排构建与执行，
 * 且门面公开 API 不泄漏实验性的 agentic 内部类型（internal / workflow / planner / SupervisorAgent）。
 */
class AgenticIsolationTest {

    private static final List<Class<?>> FACADE_CLASSES = List.of(
            AgenticWorkflows.class,
            AgenticParallelMapperBuilder.class,
            AgenticSupervisorBuilder.class,
            AgenticPlannerBuilder.class,
            AgenticHumanInTheLoopBuilder.class,
            AgenticUntypedAgentBuilder.class,
            AgenticTypedAgentBuilder.class,
            AgenticA2ABuilder.class,
            AgenticA2ATypedBuilder.class,
            AgenticSupervisor.class,
            AgenticHumanInTheLoop.class,
            AgenticToolProvider.class,
            AgenticToolProviders.class,
            AgenticPipelineRegistry.class);

    private static final List<String> FORBIDDEN_PREFIXES = List.of(
            "dev.langchain4j.agentic.internal.",
            "dev.langchain4j.agentic.workflow.",
            "dev.langchain4j.agentic.planner.");

    private static final Set<String> FORBIDDEN_EXACT = Set.of(
            "dev.langchain4j.agentic.supervisor.SupervisorAgent");

    @Test
    void businessCallerShouldBuildAndRunUsingOnlyFrameworkTypes() {
        // 仅接触框架类型 + UntypedAgent 句柄，不 import 任何 dev.langchain4j.agentic 内部/实验类型。
        AgenticHumanInTheLoop human = AgenticWorkflows.humanInTheLoop()
                .responseProvider(() -> "ok")
                .outputKey("answer")
                .build();
        UntypedAgent pipeline = AgenticWorkflows.sequence()
                .subAgents(human)
                .outputKey("answer")
                .build();

        assertThat(pipeline.invoke(Map.of())).isEqualTo("ok");
    }

    @Test
    void publicApiShouldNotLeakExperimentalAgenticTypes() {
        List<String> violations = new ArrayList<>();
        for (Class<?> clazz : allFacadeClasses()) {
            inspectMethods(clazz, violations);
            inspectConstructors(clazz, violations);
            inspectFields(clazz, violations);
        }
        assertThat(violations).as("门面公开 API 不得泄漏实验性 agentic 类型").isEmpty();
    }

    @Test
    void facadeShouldExposeAllPatternFactoriesForMagicScripts() {
        AgenticWorkflows facade = AgenticWorkflows.instance();
        assertThat(facade).isNotNull();

        Set<String> factoryNames = Arrays.stream(AgenticWorkflows.class.getDeclaredMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(factoryNames).contains(
                "sequence", "loop", "conditional", "parallel", "parallelMapper",
                "supervisor", "planner", "humanInTheLoop",
                "agent", "agentBuilder", "createAgenticSystem", "a2a", "a2aBuilder");
    }

    private static List<Class<?>> allFacadeClasses() {
        List<Class<?>> classes = new ArrayList<>(FACADE_CLASSES);
        classes.addAll(Arrays.asList(AgenticWorkflows.class.getDeclaredClasses()));
        return classes;
    }

    private static void inspectMethods(Class<?> clazz, List<String> violations) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) {
                continue;
            }
            checkType(clazz, method, "返回类型", method.getReturnType(), violations);
            for (Class<?> parameter : method.getParameterTypes()) {
                checkType(clazz, method, "参数", parameter, violations);
            }
        }
    }

    private static void inspectConstructors(Class<?> clazz, List<String> violations) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (!Modifier.isPublic(constructor.getModifiers())) {
                continue;
            }
            for (Class<?> parameter : constructor.getParameterTypes()) {
                checkType(clazz, constructor, "构造参数", parameter, violations);
            }
        }
    }

    private static void inspectFields(Class<?> clazz, List<String> violations) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isPublic(field.getModifiers())) {
                continue;
            }
            checkType(clazz, field, "公开字段", field.getType(), violations);
        }
    }

    private static void checkType(Class<?> owner, Object member, String role, Class<?> type, List<String> violations) {
        if (type == null) {
            return;
        }
        Class<?> component = type.isArray() ? type.getComponentType() : type;
        String name = component.getName();
        if (FORBIDDEN_EXACT.contains(name) || FORBIDDEN_PREFIXES.stream().anyMatch(name::startsWith)) {
            violations.add(owner.getSimpleName() + "." + member + " " + role + " 泄漏 " + name);
        }
    }
}
