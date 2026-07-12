package com.xiongx9527.fw.plus.common.validator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommonsValidatorHolderSourceTest {

    public void testHolderDoesNotUseObjectUtilsForMapEmptiness() throws Exception {
        String source = new String(Files.readAllBytes(sourcePath()), StandardCharsets.UTF_8);

        if (source.contains("import org.apache.commons.lang3.ObjectUtils;") || source.contains("ObjectUtils.isEmpty(")) {
            throw new AssertionError("CommonsValidatorHolder should not use ObjectUtils for map emptiness");
        }
    }

    private Path sourcePath() {
        Path modulePath = Paths.get("src/main/java/com/xiongx9527/fw/plus/common/validator/CommonsValidatorHolder.java");
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        return Paths.get("fw-plus-project/fw-plus-common/src/main/java/com/xiongx9527/fw/plus/common/validator/CommonsValidatorHolder.java");
    }
}
