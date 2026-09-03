package dev.langchain4j.service.output;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.internal.Json;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.TypeUtils;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.service.TypeUtils.getRawClass;
import static dev.langchain4j.service.TypeUtils.resolveFirstGenericParameterClass;
import static dev.langchain4j.service.TypeUtils.resolveFirstGenericParameterType;
import static dev.langchain4j.service.TypeUtils.typeHasRawClass;

public class ServiceOutputParser {
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("(?s)\\{.*\\}|\\[.*\\]");
    private final OutputParserFactory outputParserFactory;

    public ServiceOutputParser() { this(new DefaultOutputParserFactory()); }
    ServiceOutputParser(OutputParserFactory factory) {
        outputParserFactory = ensureNotNull(factory, "outputParserFactory");
    }

    public Object parse(Response<AiMessage> response, Type returnType) {
        if (typeHasRawClass(returnType, Result.class)) {
            returnType = resolveFirstGenericParameterClass(returnType);
        }
        Class<?> rawClass = getRawClass(returnType);
        if (rawClass == Response.class) return response;
        return parseText(returnType, rawClass, response.content().text(), response.content());
    }

    public Object parse(ChatResponse response, Type returnType) {
        if (typeHasRawClass(returnType, Result.class)) {
            returnType = resolveFirstGenericParameterType(returnType);
        }
        Class<?> rawClass = getRawClass(returnType);
        if (rawClass == Response.class) {
            return Response.from(response.aiMessage(), response.tokenUsage(), response.finishReason());
        }
        if (rawClass == void.class || rawClass == Void.class) return null;
        return parseText(returnType, rawClass, response.aiMessage().text(), response.aiMessage());
    }

    public Object parseText(Type returnType, String text) {
        return parseText(returnType, getRawClass(returnType), text, null);
    }

    private Object parseText(Type returnType, Class<?> rawClass, String text, AiMessage aiMessage) {
        if (rawClass == AiMessage.class) return aiMessage;
        if (rawClass == String.class) return text;
        Class<?> typeArgument = resolveFirstGenericParameterClass(returnType);
        Optional<OutputParser<?>> parser = outputParserFactory.get(rawClass, typeArgument);
        if (parser.isPresent()) return parser.get().parse(text);
        try {
            return Json.fromJson(text, rawClass);
        } catch (RuntimeException exception) {
            return Json.fromJson(extractJsonBlock(text), rawClass);
        }
    }

    public Optional<JsonSchema> jsonSchema(Type returnType) {
        return JsonSchemas.jsonSchemaFrom(returnType);
    }

    public String outputFormatInstructions(Type returnType) {
        if (typeHasRawClass(returnType, Result.class)) {
            returnType = resolveFirstGenericParameterClass(returnType);
        }
        Class<?> rawClass = getRawClass(returnType);
        Class<?> typeArgument = TypeUtils.resolveFirstGenericParameterClass(returnType);
        if (rawClass == String.class || rawClass == AiMessage.class || rawClass == TokenStream.class
                || rawClass == Response.class || rawClass == Map.class) return "";
        if (returnType == void.class || returnType == Void.class) return "";
        Optional<OutputParser<?>> parser = outputParserFactory.get(rawClass, typeArgument);
        if (parser.isPresent()) {
            String instructions = parser.get().formatInstructions();
            if (rawClass == List.class || rawClass == Set.class || rawClass.isEnum()
                    || instructions.startsWith("\nYou must")) return instructions;
            return "\nYou must answer strictly in the following format: " + instructions;
        }
        return "\nYou must answer strictly in the following JSON format.";
    }

    private String extractJsonBlock(String text) {
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : text;
    }
}
