//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.langchain4j.service.spring;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.internal.Exceptions;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.IllegalConfigurationException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.context.annotation.Bean;

public class AiServicesAutoConfig {
    @Bean
    BeanFactoryPostProcessor aiServicesRegisteringBeanFactoryPostProcessor() {
        return (beanFactory) -> {
            String[] chatModels = beanFactory.getBeanNamesForType(ChatModel.class);
            String[] streamingChatModels = beanFactory.getBeanNamesForType(StreamingChatModel.class);
            String[] chatMemories = beanFactory.getBeanNamesForType(ChatMemory.class);
            String[] chatMemoryProviders = beanFactory.getBeanNamesForType(ChatMemoryProvider.class);
            String[] contentRetrievers = beanFactory.getBeanNamesForType(ContentRetriever.class);
            String[] retrievalAugmentors = beanFactory.getBeanNamesForType(RetrievalAugmentor.class);
            Set<String> tools = new HashSet();

            for(String beanName : beanFactory.getBeanDefinitionNames()) {
                try {
                    Class<?> beanClass = Class.forName(beanFactory.getBeanDefinition(beanName).getBeanClassName());

                    for(Method beanMethod : beanClass.getDeclaredMethods()) {
                        if (beanMethod.isAnnotationPresent(Tool.class)) {
                            tools.add(beanName);
                        }
                    }
                } catch (Exception var18) {
                }
            }

            String[] aiServices = beanFactory.getBeanNamesForAnnotation(AiService.class);

            for(String aiService : aiServices) {
                Class<?> aiServiceClass = beanFactory.getType(aiService);
                GenericBeanDefinition aiServiceBeanDefinition = new GenericBeanDefinition();
                aiServiceBeanDefinition.setBeanClass(AiServiceFactory.class);
                aiServiceBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(aiServiceClass);
                MutablePropertyValues propertyValues = aiServiceBeanDefinition.getPropertyValues();
                AiService aiServiceAnnotation = (AiService)aiServiceClass.getAnnotation(AiService.class);
                addBeanReference(ChatModel.class, aiServiceAnnotation, aiServiceAnnotation.chatModel(), chatModels, "chatModel", "chatModel", propertyValues);
                addBeanReference(StreamingChatModel.class, aiServiceAnnotation, aiServiceAnnotation.streamingChatModel(), streamingChatModels, "streamingChatModel", "streamingChatModel", propertyValues);
                addBeanReference(ChatMemory.class, aiServiceAnnotation, aiServiceAnnotation.chatMemory(), chatMemories, "chatMemory", "chatMemory", propertyValues);
                addBeanReference(ChatMemoryProvider.class, aiServiceAnnotation, aiServiceAnnotation.chatMemoryProvider(), chatMemoryProviders, "chatMemoryProvider", "chatMemoryProvider", propertyValues);
                addBeanReference(ContentRetriever.class, aiServiceAnnotation, aiServiceAnnotation.contentRetriever(), contentRetrievers, "contentRetriever", "contentRetriever", propertyValues);
                addBeanReference(RetrievalAugmentor.class, aiServiceAnnotation, aiServiceAnnotation.retrievalAugmentor(), retrievalAugmentors, "retrievalAugmentor", "retrievalAugmentor", propertyValues);
                if (aiServiceAnnotation.wiringMode() == AiServiceWiringMode.EXPLICIT) {
                    propertyValues.add("tools", toManagedList(Arrays.asList(aiServiceAnnotation.tools())));
                } else {
                    if (aiServiceAnnotation.wiringMode() != AiServiceWiringMode.AUTOMATIC) {
                        throw Exceptions.illegalArgument("Unknown wiring mode: " + aiServiceAnnotation.wiringMode(), new Object[0]);
                    }

                    propertyValues.add("tools", toManagedList(tools));
                }

                BeanDefinitionRegistry registry = (BeanDefinitionRegistry)beanFactory;
                registry.removeBeanDefinition(aiService);
                registry.registerBeanDefinition(lowercaseFirstLetter(aiService), aiServiceBeanDefinition);
            }

        };
    }

    private static void addBeanReference(Class<?> beanType, AiService aiServiceAnnotation, String customBeanName, String[] beanNames, String annotationAttributeName, String factoryPropertyName, MutablePropertyValues propertyValues) {
        if (aiServiceAnnotation.wiringMode() == AiServiceWiringMode.EXPLICIT) {
            if (Utils.isNotNullOrBlank(customBeanName)) {
                propertyValues.add(factoryPropertyName, new RuntimeBeanReference(customBeanName));
            }
        } else {
            if (aiServiceAnnotation.wiringMode() != AiServiceWiringMode.AUTOMATIC) {
                throw Exceptions.illegalArgument("Unknown wiring mode: " + aiServiceAnnotation.wiringMode(), new Object[0]);
            }

            if (beanNames.length == 1) {
                propertyValues.add(factoryPropertyName, new RuntimeBeanReference(beanNames[0]));
            } else if (beanNames.length > 1) {
                throw conflict(beanType, beanNames, annotationAttributeName);
            }
        }

    }

    private static IllegalConfigurationException conflict(Class<?> beanType, Object[] beanNames, String attributeName) {
        return IllegalConfigurationException.illegalConfiguration("Conflict: multiple beans of type %s are found: %s. Please specify which one you wish to wire in the @AiService annotation like this: @AiService(wiringMode = EXPLICIT, %s = \"<beanName>\").", new Object[]{beanType.getName(), Arrays.toString(beanNames), attributeName});
    }

    private static String lowercaseFirstLetter(String text) {
        if (Utils.isNullOrBlank(text)) {
            return text;
        } else {
            String var10000 = text.substring(0, 1).toLowerCase();
            return var10000 + text.substring(1);
        }
    }

    private static ManagedList<RuntimeBeanReference> toManagedList(Collection<String> beanNames) {
        ManagedList<RuntimeBeanReference> managedList = new ManagedList();

        for(String beanName : beanNames) {
            managedList.add(new RuntimeBeanReference(beanName));
        }

        return managedList;
    }
}
