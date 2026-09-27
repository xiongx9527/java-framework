package dev.langchain4j.data.message;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.langchain4j.Internal;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.audio.Audio;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.pdf.PdfFile;
import dev.langchain4j.data.video.Video;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Jackson codec retaining the 0.35 ChatMessage model's type-discriminator behavior. */
@Internal
public class JacksonChatMessageJsonCodec implements ChatMessageJsonCodec {

    private static final ObjectMapper OBJECT_MAPPER = mapper();

    private static SimpleModule chatMessageModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(ChatMessage.class, new ChatMessageSerializer());
        module.addDeserializer(ChatMessage.class, new ChatMessageDeserializer());
        module.addDeserializer(UserMessage.class, new UserMessageDeserializer());
        module.addDeserializer(AiMessage.class, new AiMessageDeserializer());
        module.addDeserializer(ToolExecutionResultMessage.class, new ToolResultDeserializer());
        return module;
    }

    private static ObjectMapper mapper() {
        return chatMessageJsonMapperBuilder().build();
    }

    /** Configures the Jackson view used by the scope-aware codecs. */
    public static JsonMapper.Builder chatMessageJsonMapperBuilder() {
        return JsonMapper.builder()
                .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .addModule(chatMessageModule())
                .addMixIn(ChatMessage.class, ChatMessageMixin.class)
                .addMixIn(UserMessage.class, UserMessageMixin.class)
                .addMixIn(AiMessage.class, AiMessageMixin.class)
                .addMixIn(ToolExecutionRequest.class, ToolExecutionRequestMixin.class)
                .addMixIn(ToolExecutionResultMessage.class, ToolExecutionResultMessageMixin.class)
                .addMixIn(Content.class, ContentMixin.class)
                .addMixIn(TextContent.class, TextContentMixin.class)
                .addMixIn(ImageContent.class, ImageContentMixin.class)
                .addMixIn(Image.class, ImageMixin.class)
                .addMixIn(AudioContent.class, AudioContentMixin.class)
                .addMixIn(Audio.class, AudioMixin.class)
                .addMixIn(VideoContent.class, VideoContentMixin.class)
                .addMixIn(Video.class, VideoMixin.class)
                .addMixIn(PdfFileContent.class, PdfFileContentMixin.class)
                .addMixIn(PdfFile.class, PdfFileMixin.class);
    }

    @Override
    public ChatMessage messageFromJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, ChatMessage.class);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public List<ChatMessage> messagesFromJson(String json) {
        if (json == null) return emptyList();
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            List<ChatMessage> result = new ArrayList<ChatMessage>();
            if (node != null && node.isArray()) {
                for (JsonNode message : node) result.add(messageFromJson(message.toString()));
            }
            return result;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public String messageToJson(ChatMessage message) {
        try { return OBJECT_MAPPER.writeValueAsString(message); }
        catch (JsonProcessingException exception) { throw new RuntimeException(exception); }
    }

    @Override
    public String messagesToJson(List<ChatMessage> messages) {
        try { return OBJECT_MAPPER.writeValueAsString(messages); }
        catch (JsonProcessingException exception) { throw new RuntimeException(exception); }
    }

    abstract static class ChatMessageMixin {
        @JsonProperty
        public abstract ChatMessageType type();
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    abstract static class UserMessageMixin {
        @JsonCreator
        UserMessageMixin() { }
    }

    abstract static class AiMessageMixin {
        @JsonCreator
        AiMessageMixin() { }
    }

    abstract static class ToolExecutionRequestMixin {
        @JsonCreator
        ToolExecutionRequestMixin() { }
    }

    abstract static class ToolExecutionResultMessageMixin {
        @JsonCreator
        ToolExecutionResultMessageMixin() { }
        abstract Boolean isError();
    }

    abstract static class ContentMixin {
        @JsonProperty
        public abstract ContentType type();
    }

    abstract static class TextContentMixin {
        TextContentMixin(@JsonProperty("text") String text) { }
    }

    abstract static class ImageContentMixin {
        ImageContentMixin(@JsonProperty("image") Image image,
                          @JsonProperty("detailLevel") ImageContent.DetailLevel detailLevel) { }
    }

    abstract static class ImageMixin {
        ImageMixin() { }
    }

    abstract static class AudioContentMixin {
        AudioContentMixin(@JsonProperty("audio") Audio audio) { }
    }

    abstract static class AudioMixin {
        AudioMixin() { }
    }

    abstract static class VideoContentMixin {
        VideoContentMixin(@JsonProperty("video") Video video) { }
    }

    abstract static class VideoMixin {
        VideoMixin() { }
    }

    abstract static class PdfFileContentMixin {
        PdfFileContentMixin(@JsonProperty("pdfFile") PdfFile pdfFile) { }
    }

    abstract static class PdfFileMixin {
        PdfFileMixin() { }
    }

    private static final class ChatMessageSerializer extends JsonSerializer<ChatMessage> {
        @Override public void serialize(ChatMessage value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            JsonNode node;
            if (value instanceof UserMessage) {
                UserMessage user = (UserMessage) value;
                com.fasterxml.jackson.databind.node.ObjectNode object = mapper.createObjectNode();
                if (user.name() != null) object.put("name", user.name());
                if (user.contents() != null && user.contents().size() == 1 && user.contents().get(0) instanceof TextContent) {
                    object.put("text", ((TextContent) user.contents().get(0)).text());
                } else {
                    com.fasterxml.jackson.databind.node.ArrayNode contents = mapper.createArrayNode();
                    if (user.contents() != null) {
                        for (Content content : user.contents()) {
                            com.fasterxml.jackson.databind.node.ObjectNode contentNode =
                                    (com.fasterxml.jackson.databind.node.ObjectNode) mapper.valueToTree(content);
                            contentNode.put("type", content.type().toString());
                            contents.add(contentNode);
                        }
                    }
                    object.set("contents", contents);
                }
                node = object;
            } else if (value instanceof AiMessage) {
                com.fasterxml.jackson.databind.node.ObjectNode object = mapper.createObjectNode();
                AiMessage ai = (AiMessage) value;
                if (ai.text() != null) object.put("text", ai.text());
                if (ai.toolExecutionRequests() != null) object.set("toolExecutionRequests", mapper.valueToTree(ai.toolExecutionRequests()));
                node = object;
            } else if (value instanceof SystemMessage) {
                com.fasterxml.jackson.databind.node.ObjectNode object = mapper.createObjectNode();
                object.put("text", ((SystemMessage) value).text());
                node = object;
            } else if (value instanceof ToolExecutionResultMessage) {
                ToolExecutionResultMessage result = (ToolExecutionResultMessage) value;
                com.fasterxml.jackson.databind.node.ObjectNode object = mapper.createObjectNode();
                object.put("id", result.id());
                object.put("toolName", result.toolName());
                object.put("text", result.text());
                node = object;
            } else {
                node = mapper.valueToTree(value);
            }
            ((com.fasterxml.jackson.databind.node.ObjectNode) node).put("type", value.type().toString());
            generator.writeTree(node);
        }
    }

    private static final class ChatMessageDeserializer extends JsonDeserializer<ChatMessage> {
        @Override public ChatMessage deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            String type = node.get("type").asText();
            if ("USER".equals(type)) return new UserMessageDeserializer().fromNode(node);
            if ("AI".equals(type)) return new AiMessageDeserializer().fromNode(node);
            if ("SYSTEM".equals(type)) return new SystemMessage(node.get("text").asText());
            if ("TOOL_EXECUTION_RESULT".equals(type)) return new ToolResultDeserializer().fromNode(node);
            throw new IOException("Unknown chat message type: " + type);
        }
    }

    private static final class UserMessageDeserializer extends JsonDeserializer<UserMessage> {
        @Override public UserMessage deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return fromNode(parser.getCodec().readTree(parser));
        }
        UserMessage fromNode(JsonNode node) {
            JsonNode text = node.get("text");
            JsonNode name = node.get("name");
            if (text != null && !text.isNull()) {
                return name == null || name.isNull()
                        ? UserMessage.from(text.asText())
                        : UserMessage.from(name.asText(), text.asText());
            }
            List<Content> contents = new ArrayList<Content>();
            JsonNode array = node.get("contents");
            if (array != null && array.isArray()) {
                for (JsonNode c : array) {
                    String contentType = c.has("type") ? c.get("type").asText() : "TEXT";
                    if ("TEXT".equals(contentType)) {
                        JsonNode textContent = c.get("text");
                        if (textContent != null && !textContent.isNull()) {
                            contents.add(new TextContent(textContent.asText()));
                        }
                    } else if ("IMAGE".equals(contentType)) {
                        JsonNode image = c.get("image");
                        ImageContent.DetailLevel detailLevel = imageDetailLevel(c);
                        contents.add(image.has("url") && !image.get("url").isNull()
                                ? new ImageContent(image.get("url").asText(), detailLevel)
                                : new ImageContent(Image.builder()
                                        .base64Data(image.get("base64Data").asText())
                                        .mimeType(image.has("mimeType") ? image.get("mimeType").asText() : null)
                                        .build(), detailLevel));
                    } else if ("AUDIO".equals(contentType)) {
                        JsonNode audio = c.get("audio");
                        contents.add(audio.has("url") && !audio.get("url").isNull()
                                ? new AudioContent(audio.get("url").asText())
                                : new AudioContent(Audio.builder()
                                        .base64Data(audio.get("base64Data").asText())
                                        .mimeType(audio.has("mimeType") ? audio.get("mimeType").asText() : null)
                                        .build()));
                    } else if ("VIDEO".equals(contentType)) {
                        JsonNode video = c.get("video");
                        contents.add(video.has("url") && !video.get("url").isNull()
                                ? new VideoContent(video.get("url").asText())
                                : new VideoContent(Video.builder()
                                        .base64Data(video.get("base64Data").asText())
                                        .mimeType(video.has("mimeType") ? video.get("mimeType").asText() : null)
                                        .build()));
                    } else if ("PDF".equals(contentType)) {
                        JsonNode pdf = c.get("pdfFile");
                        contents.add(pdf.has("url") && !pdf.get("url").isNull()
                                ? new PdfFileContent(pdf.get("url").asText())
                                : new PdfFileContent(PdfFile.builder()
                                        .base64Data(pdf.get("base64Data").asText())
                                        .build()));
                    } else if ("TEXT_FILE".equals(contentType)) {
                        throw new IllegalArgumentException("TEXT_FILE content cannot be reconstructed by the 0.35 Core bridge");
                    } else {
                        throw new IllegalArgumentException("Unknown content type: " + contentType);
                    }
                }
            }
            return name == null || name.isNull()
                    ? UserMessage.from(contents)
                    : UserMessage.from(name.asText(), contents);
        }

        private static ImageContent.DetailLevel imageDetailLevel(JsonNode content) {
            JsonNode detailLevel = content.get("detailLevel");
            if (detailLevel == null || detailLevel.isNull()) {
                return ImageContent.DetailLevel.LOW;
            }
            try {
                return ImageContent.DetailLevel.valueOf(detailLevel.asText());
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unknown image detail level: " + detailLevel.asText(), exception);
            }
        }
    }

    private static final class AiMessageDeserializer extends JsonDeserializer<AiMessage> {
        @Override public AiMessage deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return fromNode(parser.getCodec().readTree(parser));
        }
        AiMessage fromNode(JsonNode node) {
            String text = node.has("text") && !node.get("text").isNull() ? node.get("text").asText() : null;
            List<ToolExecutionRequest> requests = new ArrayList<ToolExecutionRequest>();
            JsonNode tools = node.get("toolExecutionRequests");
            if (tools != null && tools.isArray()) {
                for (JsonNode tool : tools) {
                    requests.add(ToolExecutionRequest.builder()
                            .id(tool.has("id") ? tool.get("id").asText() : null)
                            .name(tool.has("name") ? tool.get("name").asText() : null)
                            .arguments(tool.has("arguments") ? tool.get("arguments").asText() : null)
                            .build());
                }
            }
            return requests.isEmpty() ? AiMessage.from(text) : AiMessage.from(text, requests);
        }
    }

    private static final class ToolResultDeserializer extends JsonDeserializer<ToolExecutionResultMessage> {
        @Override public ToolExecutionResultMessage deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return fromNode(parser.getCodec().readTree(parser));
        }
        ToolExecutionResultMessage fromNode(JsonNode node) {
            return new ToolExecutionResultMessage(node.get("id").asText(), node.get("toolName").asText(), node.get("text").asText());
        }
    }
}
