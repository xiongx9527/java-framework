package dev.langchain4j.service;

import dev.langchain4j.Internal;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static dev.langchain4j.service.IllegalConfigurationException.illegalConfiguration;

@Internal
public final class InternalReflectionVariableResolver {
    private InternalReflectionVariableResolver() { }

    public static Map<String, Object> findTemplateVariables(String template, Method method, Object[] args) {
        if (args == null) return Collections.emptyMap();
        Parameter[] parameters = method.getParameters();
        Map<String, Object> variables = new HashMap<String, Object>();
        for (int i = 0; i < args.length; i++) {
            Parameter parameter = parameters[i];
            if (InvocationParameters.class.isAssignableFrom(parameter.getType())) continue;
            Object value = args[i];
            variables.put(ParameterNameResolver.name(parameter), value);
            if (value instanceof Map<?, ?>) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    if (entry.getKey() != null && entry.getKey().getClass() == String.class) {
                        variables.put((String) entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        if (template.contains("{{it}}") && !variables.containsKey("it")) {
            variables.put("it", getValueOfVariableIt(parameters, args));
        }
        return variables;
    }

    private static String getValueOfVariableIt(Parameter[] parameters, Object[] args) {
        if (args.length == 1) {
            Parameter parameter = parameters[0];
            if (!parameter.isAnnotationPresent(MemoryId.class)
                    && !parameter.isAnnotationPresent(UserMessage.class)
                    && !parameter.isAnnotationPresent(UserName.class)
                    && (!parameter.isAnnotationPresent(V.class) || isAnnotatedWithIt(parameter))) {
                return asString(args[0]);
            }
        }
        for (int i = 0; i < args.length; i++) {
            if (isAnnotatedWithIt(parameters[i])) return asString(args[i]);
        }
        throw illegalConfiguration("Error: cannot find the value of the prompt template variable \"{{it}}\".");
    }

    private static boolean isAnnotatedWithIt(Parameter parameter) {
        V annotation = parameter.getAnnotation(V.class);
        return annotation != null && "it".equals(annotation.value());
    }

    static String asString(Object arg) {
        if (arg == null) return "null";
        if (arg.getClass().isArray()) return arrayAsString(arg);
        if (arg.getClass().isAnnotationPresent(StructuredPrompt.class)) {
            return StructuredPromptProcessor.toPrompt(arg).text();
        }
        return arg.toString();
    }

    private static String arrayAsString(Object arg) {
        StringBuilder builder = new StringBuilder("[");
        int length = Array.getLength(arg);
        for (int i = 0; i < length; i++) {
            builder.append(asString(Array.get(arg, i)));
            if (i < length - 1) builder.append(", ");
        }
        return builder.append(']').toString();
    }
}
