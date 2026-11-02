package com.xiongx9527.fw.plus.ai.askdata.capability;

/** 输入缺失（§6.38.6：缺 schemaContext 等显式输入时明确报错）。 */
public class MissingInputException extends SqlGenerationException {
    public MissingInputException(String message) {
        super(message);
    }
}
