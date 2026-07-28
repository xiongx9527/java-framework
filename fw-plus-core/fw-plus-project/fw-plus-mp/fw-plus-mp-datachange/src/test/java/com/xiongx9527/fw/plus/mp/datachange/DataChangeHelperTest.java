package com.xiongx9527.fw.plus.mp.datachange;

import com.xiongx9527.fw.plus.mp.datachange.properties.DataChangeProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(OutputCaptureExtension.class)
class DataChangeHelperTest {

    @Test
    void formatValueReturnsEmptyStringForNullValueOrFormat() {
        DataChangeProperties.FieldFormat format = new DataChangeProperties.FieldFormat();

        assertEquals("", DataChangeHelper.formatValue(null, format));
        assertEquals("", DataChangeHelper.formatValue("value", (DataChangeProperties.FieldFormat) null));
        assertEquals("", DataChangeHelper.formatValue(null, new DataChangeProperties.DataChangeField()));
        assertEquals("", DataChangeHelper.formatValue("value", (DataChangeProperties.DataChangeField) null));
    }

    @Test
    void formatValueFallsBackToOriginalValueWhenDatePatternInvalid(CapturedOutput output) {
        Date date = new Date(0L);
        DataChangeProperties.FieldFormat format = new DataChangeProperties.FieldFormat();
        format.setDateFormat("invalid-pattern-[");

        assertEquals(date, DataChangeHelper.formatValue(date, format));

        DataChangeProperties.DataChangeField field = new DataChangeProperties.DataChangeField();
        field.setDateFormat("invalid-pattern-[");

        assertEquals(date, DataChangeHelper.formatValue(date, field));
        assertFalse((output.getOut() + output.getErr()).contains("Illegal pattern component"));
    }

    @Test
    void formatValueUsesConfiguredLiteralValueFirst() {
        DataChangeProperties.FieldFormat format = new DataChangeProperties.FieldFormat();
        format.setValue("已格式化");

        assertEquals("已格式化", DataChangeHelper.formatValue("raw", format));
    }
}
