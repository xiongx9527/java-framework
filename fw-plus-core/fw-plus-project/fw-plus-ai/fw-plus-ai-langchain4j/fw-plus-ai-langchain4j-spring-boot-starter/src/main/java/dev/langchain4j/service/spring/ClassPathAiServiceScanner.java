//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.langchain4j.service.spring;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

class ClassPathAiServiceScanner extends ClassPathBeanDefinitionScanner {
    ClassPathAiServiceScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
        super(registry, useDefaultFilters);
        this.addIncludeFilter(new AnnotationTypeFilter(AiService.class));
    }

    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
        return annotationMetadata.isInterface() && annotationMetadata.isIndependent();
    }
}
