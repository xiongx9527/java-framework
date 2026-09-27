package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ReflectiveLangChain4jEmbeddingStoreProviderAdapter
        implements LangChain4jEmbeddingStoreProviderAdapter {

    private static final String CONSTRUCTOR_ARGS = "constructorArgs";

    private final LangChain4jEmbeddingStoreProviderSpec spec;
    private final ApplicationContext applicationContext;

    ReflectiveLangChain4jEmbeddingStoreProviderAdapter(
            LangChain4jEmbeddingStoreProviderSpec spec,
            ApplicationContext applicationContext) {
        if (spec == null) {
            throw new IllegalArgumentException("spec must not be null");
        }
        this.spec = spec;
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean supports(AiEmbeddingStoreDefinition definition) {
        if (definition == null || !spec.getProviders().contains(
                ProviderAdapterSupport.normalizeProvider(definition.getProvider()))) {
            return false;
        }
        for (String className : spec.getRequiredClassNames()) {
            if (!ProviderAdapterSupport.classPresent(className)) {
                return false;
            }
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EmbeddingStore<TextSegment> create(AiEmbeddingStoreDefinition definition) {
        try {
            if (!supports(definition)) {
                throw new IllegalArgumentException("Provider is unavailable or unsupported");
            }
            ProviderAdapterSupport.validateRequirements(spec, definition);
            Class<?> storeClass = ProviderAdapterSupport.loadClass(spec.getStoreClassName());
            Object created = create(storeClass, definition);
            if (!(created instanceof EmbeddingStore)) {
                throw new IllegalStateException(storeClass.getName() + " is not an EmbeddingStore");
            }
            return (EmbeddingStore<TextSegment>) created;
        } catch (AiEmbeddingStoreException e) {
            throw e;
        } catch (Exception e) {
            throw ProviderAdapterSupport.exception(definition, e.getMessage(), e);
        }
    }

    private Object create(Class<?> storeClass, AiEmbeddingStoreDefinition definition) throws Exception {
        List<LangChain4jEmbeddingStoreProviderSpec.CreationPlan> matched =
                new ArrayList<LangChain4jEmbeddingStoreProviderSpec.CreationPlan>();
        for (LangChain4jEmbeddingStoreProviderSpec.CreationPlan plan : spec.getCreationPlans()) {
            if (ProviderAdapterSupport.matches(plan.getCondition(), definition)) {
                matched.add(plan);
            }
        }
        if (matched.size() != 1) {
            throw new IllegalArgumentException("Expected one matching creation plan, found=" + matched.size());
        }
        LangChain4jEmbeddingStoreProviderSpec.CreationPlan plan = matched.get(0);
        if (plan.getKind() == LangChain4jEmbeddingStoreProviderSpec.CreationKind.CONSTRUCTOR) {
            return ProviderAdapterSupport.construct(storeClass, plan.getConstructorArguments(),
                    definition, applicationContext);
        }
        if (plan.getKind() == LangChain4jEmbeddingStoreProviderSpec.CreationKind.RAW_CONSTRUCTOR_ARGS) {
            Object constructorArgs = ProviderAdapterSupport.property(definition, CONSTRUCTOR_ARGS);
            if (!(constructorArgs instanceof List)) {
                throw new IllegalArgumentException(CONSTRUCTOR_ARGS + " must be a list");
            }
            return ProviderAdapterSupport.constructRaw(storeClass, (List<?>) constructorArgs, applicationContext);
        }
        if (plan.getKind() == LangChain4jEmbeddingStoreProviderSpec.CreationKind.AUTO) {
            Method builder = findBuilder(storeClass);
            if (builder == null) {
                return ProviderAdapterSupport.construct(storeClass,
                        Collections.<LangChain4jEmbeddingStoreProviderSpec.Value>emptyList(),
                        definition, applicationContext);
            }
            Object target = builder.invoke(null);
            target = configurePassthrough(target, definition,
                    Collections.<LangChain4jEmbeddingStoreProviderSpec.Invocation>emptyList());
            return ProviderAdapterSupport.invokeNoArg(target, "build");
        }

        Object target = ProviderAdapterSupport.invokeStaticNoArg(storeClass, plan.getFactoryMethod());
        if (plan.isPassthrough()) {
            target = configurePassthrough(target, definition, plan.getInvocations());
        }
        for (LangChain4jEmbeddingStoreProviderSpec.Invocation invocation : plan.getInvocations()) {
            if (ProviderAdapterSupport.matches(invocation.getCondition(), definition)) {
                target = ProviderAdapterSupport.invoke(target, invocation.getMethod(),
                        invocation.getArguments(), definition, applicationContext);
            }
        }
        return ProviderAdapterSupport.invokeNoArg(target, plan.getTerminalMethod());
    }

    private Object configurePassthrough(Object target, AiEmbeddingStoreDefinition definition,
                                        List<LangChain4jEmbeddingStoreProviderSpec.Invocation> invocations)
            throws Exception {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        if (definition.getProperties() != null) {
            properties.putAll(definition.getProperties());
        }
        properties.remove(CONSTRUCTOR_ARGS);
        for (LangChain4jEmbeddingStoreProviderSpec.Invocation invocation : invocations) {
            for (LangChain4jEmbeddingStoreProviderSpec.Value value : invocation.getArguments()) {
                properties.keySet().removeAll(value.getSources());
            }
        }
        List<String> names = new ArrayList<String>(properties.keySet());
        Collections.sort(names);
        for (String name : names) {
            if (properties.get(name) != null) {
                target = ProviderAdapterSupport.invoke(target, name,
                        Collections.singletonList(LangChain4jEmbeddingStoreProviderSpec.property(name)),
                        definition, applicationContext);
            }
        }
        return target;
    }

    private Method findBuilder(Class<?> storeClass) {
        List<Method> builders = new ArrayList<Method>();
        for (Method method : storeClass.getMethods()) {
            if ("builder".equals(method.getName()) && method.getParameterTypes().length == 0
                    && Modifier.isStatic(method.getModifiers())) {
                builders.add(method);
            }
        }
        if (builders.isEmpty()) {
            return null;
        }
        if (builders.size() > 1) {
            throw new IllegalArgumentException("Ambiguous builder method on " + storeClass.getName());
        }
        return builders.get(0);
    }
}
