package com.xiongx9527.fw.plus.ai.autoconfigure.spring.adapter;

import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.spring.adapter.SpringAiAlibabaModelAdapter;
import com.xiongx9527.fw.plus.ai.spring.adapter.SpringAiModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.spring.adapter.SpringAiProviderBinding;
import com.xiongx9527.fw.plus.ai.spring.adapter.SpringAiProviderResolutionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class BeanBackedSpringAiModelProviderAdapter implements SpringAiModelProviderAdapter {

    private static final String BEAN_NAME = "beanName";

    private final String framework;
    private final AiModelType type;
    private final SpringAiBeanCatalog catalog;
    private final List<SpringAiProviderBinding> bindings;

    BeanBackedSpringAiModelProviderAdapter(String framework, AiModelType type, SpringAiBeanCatalog catalog,
                                           List<SpringAiProviderBinding> bindings) {
        this.framework = framework;
        this.type = type;
        this.catalog = catalog;
        this.bindings = bindings == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(bindings));
    }

    @Override
    public boolean supports(AiModelDefinition definition) {
        if (!matchesDefinition(definition)) {
            return false;
        }
        String explicitBeanName = beanName(definition);
        if (explicitBeanName != null) {
            return supportsExplicitBean(definition, explicitBeanName);
        }
        List<SpringAiProviderBinding> relevant = relevantBindings(definition);
        if (relevant.isEmpty()) {
            return false;
        }
        List<SpringAiProviderBinding> matches = matchingBindings(definition, relevant);
        if (matches.size() != 1) {
            throw resolutionFailure(matches.size(), beanNames(relevant));
        }
        return true;
    }

    @Override
    public Object create(AiModelDefinition definition) {
        if (!matchesDefinition(definition)) {
            throw new AiModelException("Spring AI provider adapter does not support " + context(definition));
        }
        String explicitBeanName = beanName(definition);
        if (explicitBeanName != null) {
            if (!supportsExplicitBean(definition, explicitBeanName)) {
                throw new AiModelException("Spring AI model bean does not match provider: " + context(definition)
                        + ", beanRef=" + catalog.beanRef(explicitBeanName)
                        + ", candidates=" + catalog.candidateCount(type));
            }
            return catalog.get(explicitBeanName, type);
        }
        List<SpringAiProviderBinding> relevant = relevantBindings(definition);
        List<SpringAiProviderBinding> matches = matchingBindings(definition, relevant);
        if (matches.size() != 1) {
            throw resolutionFailure(matches.size(), beanNames(relevant));
        }
        return catalog.get(matches.get(0).getBeanName(), type);
    }

    private boolean matchesDefinition(AiModelDefinition definition) {
        return definition != null && framework.equalsIgnoreCase(definition.getFramework())
                && type == definition.getType();
    }

    private boolean supportsExplicitBean(AiModelDefinition definition, String beanName) {
        if (!catalog.contains(beanName, type)) {
            return false;
        }
        boolean dashScope = catalog.isDashScope(beanName, type);
        if (SpringAiAlibabaModelAdapter.FRAMEWORK.equals(framework)) {
            return dashScope && "dashscope".equalsIgnoreCase(definition.getProvider());
        }
        return !dashScope;
    }

    private List<SpringAiProviderBinding> relevantBindings(AiModelDefinition definition) {
        List<SpringAiProviderBinding> relevant = new ArrayList<>();
        for (SpringAiProviderBinding binding : bindings) {
            if (binding != null && binding.matches(framework, definition.getProvider(), type)
                    && !containsBinding(relevant, binding.getBeanName())) {
                relevant.add(binding);
            }
        }
        return relevant;
    }

    private List<SpringAiProviderBinding> matchingBindings(AiModelDefinition definition,
            List<SpringAiProviderBinding> relevant) {
        List<SpringAiProviderBinding> matches = new ArrayList<>();
        for (SpringAiProviderBinding binding : relevant) {
            if (supportsExplicitBean(definition, binding.getBeanName())) {
                matches.add(binding);
            }
        }
        return matches;
    }

    private SpringAiProviderResolutionException resolutionFailure(int candidates, List<String> beanNames) {
        List<String> refs = new ArrayList<>(beanNames.size());
        for (String beanName : beanNames) {
            refs.add(catalog.beanRef(beanName));
        }
        Collections.sort(refs);
        return new SpringAiProviderResolutionException(candidates, refs);
    }

    private List<String> beanNames(List<SpringAiProviderBinding> bindings) {
        List<String> names = new ArrayList<>(bindings.size());
        for (SpringAiProviderBinding binding : bindings) {
            names.add(binding.getBeanName());
        }
        return names;
    }

    private boolean containsBinding(List<SpringAiProviderBinding> matches, String beanName) {
        for (SpringAiProviderBinding match : matches) {
            if (match.getBeanName().equals(beanName)) {
                return true;
            }
        }
        return false;
    }

    private String beanName(AiModelDefinition definition) {
        Object value = definition.getProperties().get(BEAN_NAME);
        if (value == null) {
            value = definition.getProperties().get("bean-name");
        }
        if (value == null) {
            return null;
        }
        String beanName = String.valueOf(value).trim();
        return beanName.isEmpty() ? null : beanName;
    }

    private String context(AiModelDefinition definition) {
        return "key=" + definition.getKey() + ", framework=" + definition.getFramework()
                + ", provider=" + definition.getProvider() + ", type=" + definition.getType();
    }
}
