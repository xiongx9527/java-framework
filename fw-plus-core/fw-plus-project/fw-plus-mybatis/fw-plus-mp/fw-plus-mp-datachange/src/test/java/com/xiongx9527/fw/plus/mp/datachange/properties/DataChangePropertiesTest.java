package com.xiongx9527.fw.plus.mp.datachange.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataChangePropertiesTest {

    @Test
    void defaultPhysicalDeleteMethodsUsePhysicalDeleteById() {
        DataChangeProperties properties = new DataChangeProperties();

        assertTrue(properties.getSingleEntityPhysicalDeleteMethods().contains("physicalDeleteById"));
        assertFalse(properties.getSingleEntityPhysicalDeleteMethods().contains("uphysicalDeleteById"));
    }

    @Test
    void defaultLogicDeleteMethodsDoNotContainBlankMethodName() {
        DataChangeProperties properties = new DataChangeProperties();

        assertTrue(properties.getSingleEntityLogicDeleteMethods().contains("deleteById"));
        assertFalse(properties.getSingleEntityLogicDeleteMethods().contains(""));
    }

    @Test
    void legacyTypoNamedPropertiesRemainBindable() {
        DataChangeProperties properties = new DataChangeProperties();

        properties.setSultipelEntityPhysicalDeleteMethods(java.util.Collections.singletonList("customPhysicalDelete"));
        properties.setSultipelEntityLogicDeleteMethods(java.util.Collections.singletonList("customLogicDelete"));

        assertTrue(properties.getSultipelEntityPhysicalDeleteMethods().contains("customPhysicalDelete"));
        assertTrue(properties.getSultipelEntityLogicDeleteMethods().contains("customLogicDelete"));
    }
}
