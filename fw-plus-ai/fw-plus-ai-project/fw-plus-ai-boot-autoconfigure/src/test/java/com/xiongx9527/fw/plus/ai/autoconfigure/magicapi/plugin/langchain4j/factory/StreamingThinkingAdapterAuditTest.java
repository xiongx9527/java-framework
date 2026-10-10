package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory;

import dev.langchain4j.community.model.CohereStreamingChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.community.model.oracle.oci.genai.OciGenAiStreamingChatModel;
import dev.langchain4j.community.model.qianfan.QianfanStreamingChatModel;
import dev.langchain4j.community.model.xinference.XinferenceStreamingChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.bedrock.BedrockStreamingChatModel;
import dev.langchain4j.model.github.GitHubModelsStreamingChatModel;
import dev.langchain4j.model.google.genai.GoogleGenAiStreamingChatModel;
import dev.langchain4j.model.localai.LocalAiStreamingChatModel;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialResponsesStreamingChatModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialStreamingChatModel;
import dev.langchain4j.model.vertexai.gemini.VertexAiGeminiStreamingChatModel;
import dev.langchain4j.model.watsonx.WatsonxStreamingChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.Handle;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;

class StreamingThinkingAdapterAuditTest {

    private static final String CALLBACK_METHOD = "onPartialThinking";

    @Test
    void lockedProviderSdkArtifactsMatchTheDocumentedThinkingCallbackMatrix() throws Exception {
        assertUnsupported(AzureOpenAiStreamingChatModel.class);
        assertUnsupported(GitHubModelsStreamingChatModel.class);
        assertUnsupported(LocalAiStreamingChatModel.class);
        assertUnsupported(OciGenAiStreamingChatModel.class);
        assertUnsupported(QianfanStreamingChatModel.class);
        assertUnsupported(VertexAiGeminiStreamingChatModel.class);
    }

    @Test
    void supportedAdaptersLinkTheirConfiguredModelToTheThinkingResponseHandler() throws Exception {
        assertHandler(AnthropicStreamingChatModel.class,
                "dev.langchain4j.model.anthropic.internal.client.AnthropicClient",
                "dev.langchain4j.model.anthropic.internal.client.DefaultAnthropicClient",
                "dev.langchain4j.model.anthropic.internal.client.DefaultAnthropicClient$2");
        assertHandler(BedrockStreamingChatModel.class, null,
                BedrockStreamingChatModel.class.getName(),
                BedrockStreamingChatModel.class.getName() + "$1");
        assertHandler(CohereStreamingChatModel.class,
                "dev.langchain4j.community.model.client.CohereClient",
                "dev.langchain4j.community.model.client.CohereClient",
                "dev.langchain4j.community.model.client.chat.streaming.CohereServerSentEventListener");
        assertHandler(GoogleGenAiStreamingChatModel.class, null,
                GoogleGenAiStreamingChatModel.class.getName(),
                GoogleGenAiStreamingChatModel.class.getName());
        assertHandler(MistralAiStreamingChatModel.class,
                "dev.langchain4j.model.mistralai.internal.client.MistralAiClient",
                "dev.langchain4j.model.mistralai.internal.client.DefaultMistralAiClient",
                "dev.langchain4j.model.mistralai.internal.client.MistralAiServerSentEventListener");
        assertHandler(OllamaStreamingChatModel.class,
                "dev.langchain4j.model.ollama.OllamaClient",
                "dev.langchain4j.model.ollama.OllamaClient",
                "dev.langchain4j.model.ollama.OllamaClient$2");
        assertHandler(OpenAiStreamingChatModel.class, null,
                "dev.langchain4j.model.openai.internal.ChatCompletionEventDispatcher",
                "dev.langchain4j.model.openai.internal.ChatCompletionEventDispatcher");
        assertHandler(QwenStreamingChatModel.class, null,
                QwenStreamingChatModel.class.getName(),
                QwenStreamingChatModel.class.getName() + "$1");
        assertHandler(WatsonxStreamingChatModel.class, null,
                "dev.langchain4j.model.watsonx.WatsonxChatBase",
                "dev.langchain4j.model.watsonx.WatsonxChatBase$1");
        // community 1.21.0-beta31 起：Xinference 流式模型读取 Delta.reasoningContent 并回调 onPartialThinking
        assertHandler(XinferenceStreamingChatModel.class, null,
                XinferenceStreamingChatModel.class.getName(),
                XinferenceStreamingChatModel.class.getName());
        assertHandler(ZhipuAiStreamingChatModel.class,
                "dev.langchain4j.community.model.zhipu.ZhipuAiClient",
                "dev.langchain4j.community.model.zhipu.ZhipuAiClient",
                "dev.langchain4j.community.model.zhipu.ZhipuAiClient$2");
    }

    @Test
    void openAiOfficialResponseHandlerIsSeparateFromTheConfiguredFactoryModel() throws Exception {
        assertThat(inspect(OpenAiOfficialStreamingChatModel.class).hasCallbackInvocation()).isFalse();
        assertThat(inspect(OpenAiOfficialStreamingChatModel.class)
                .references(OpenAiOfficialResponsesStreamingChatModel.class.getName())).isFalse();
        String responseHandler = OpenAiOfficialResponsesStreamingChatModel.class.getName()
                + "$ResponsesEventHandler";
        assertThat(inspect(OpenAiOfficialResponsesStreamingChatModel.class).references(responseHandler)).isTrue();
        assertThat(inspect(responseHandler).hasCallbackInvocation()).isTrue();
    }

    private static void assertUnsupported(Class<?> sdkModel) throws Exception {
        assertThat(artifactContainsCallback(sdkModel))
                .as("%s has no thinking callback mapping", sdkModel.getName())
                .isFalse();
    }

    private static void assertHandler(Class<?> sdkModel, String modelReference, String handlerOwner,
            String handlerClass) throws Exception {
        Class<?> owner = Class.forName(handlerOwner);
        if (modelReference != null) {
            assertThat(inspect(sdkModel).references(modelReference))
                    .as("%s links to its SDK response client", sdkModel.getName())
                    .isTrue();
            assertThat(Class.forName(modelReference).isAssignableFrom(owner))
                    .as("%s implements the model's response client", owner.getName())
                    .isTrue();
        } else if (!sdkModel.getName().equals(handlerOwner) && !owner.isAssignableFrom(sdkModel)) {
            assertThat(inspect(sdkModel).references(handlerOwner))
                    .as("%s links to its thinking response handler", sdkModel.getName())
                    .isTrue();
        }
        if (!handlerOwner.equals(handlerClass)) {
            assertThat(inspect(owner).references(handlerClass))
                    .as("%s links to its thinking response handler", handlerOwner)
                    .isTrue();
        }
        assertThat(inspect(handlerClass).hasCallbackInvocation())
                .as("%s invokes onPartialThinking", handlerClass)
                .isTrue();
    }

    private static boolean artifactContainsCallback(Class<?> sdkModel) throws Exception {
        File jarFile = jarFileOf(sdkModel);
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    try (InputStream input = jar.getInputStream(entry)) {
                        if (inspect(input).hasCallbackInvocation()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static Inspection inspect(Class<?> type) throws IOException {
        return inspect(type.getClassLoader(), type.getName());
    }

    private static Inspection inspect(String className) throws IOException {
        return inspect(Thread.currentThread().getContextClassLoader(), className);
    }

    private static Inspection inspect(ClassLoader classLoader, String className) throws IOException {
        String resource = className.replace('.', '/') + ".class";
        try (InputStream input = classLoader.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Class resource was not found: " + resource);
            }
            return inspect(input);
        }
    }

    private static Inspection inspect(InputStream input) throws IOException {
        final Set<String> references = new HashSet<String>();
        final boolean[] callbackInvocation = {false};
        ClassReader reader = new ClassReader(input);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                references.add(name);
                addClassReference(references, superName);
                if (interfaces != null) {
                    Collections.addAll(references, interfaces);
                }
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature,
                    Object value) {
                addDescriptorReferences(references, Type.getType(descriptor));
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                addDescriptorReferences(references, Type.getMethodType(descriptor));
                if (exceptions != null) {
                    Collections.addAll(references, exceptions);
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        addClassReference(references, type);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDescriptor) {
                        addClassReference(references, owner);
                        addDescriptorReferences(references, Type.getType(fieldDescriptor));
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                            String methodDescriptor, boolean isInterface) {
                        addClassReference(references, owner);
                        addDescriptorReferences(references, Type.getMethodType(methodDescriptor));
                        callbackInvocation[0] |= CALLBACK_METHOD.equals(methodName);
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
                            Object... bootstrapMethodArguments) {
                        addDescriptorReferences(references, Type.getMethodType(descriptor));
                        addClassReference(references, bootstrapMethodHandle.getOwner());
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return new Inspection(references, callbackInvocation[0]);
    }

    private static void addDescriptorReferences(Set<String> references, Type descriptor) {
        if (descriptor.getSort() == Type.METHOD) {
            for (Type argument : descriptor.getArgumentTypes()) {
                addDescriptorReferences(references, argument);
            }
            addDescriptorReferences(references, descriptor.getReturnType());
        } else if (descriptor.getSort() == Type.ARRAY) {
            addDescriptorReferences(references, descriptor.getElementType());
        } else if (descriptor.getSort() == Type.OBJECT) {
            references.add(descriptor.getInternalName());
        }
    }

    private static void addClassReference(Set<String> references, String className) {
        if (className != null) {
            references.add(className);
        }
    }

    private static File jarFileOf(Class<?> sdkModel) throws Exception {
        URL location = sdkModel.getProtectionDomain().getCodeSource().getLocation();
        return new File(location.toURI());
    }

    private static final class Inspection {

        private final Set<String> references;
        private final boolean callbackInvocation;

        private Inspection(Set<String> references, boolean callbackInvocation) {
            this.references = references;
            this.callbackInvocation = callbackInvocation;
        }

        private boolean references(String className) {
            return references.contains(className.replace('.', '/'));
        }

        private boolean hasCallbackInvocation() {
            return callbackInvocation;
        }
    }
}
