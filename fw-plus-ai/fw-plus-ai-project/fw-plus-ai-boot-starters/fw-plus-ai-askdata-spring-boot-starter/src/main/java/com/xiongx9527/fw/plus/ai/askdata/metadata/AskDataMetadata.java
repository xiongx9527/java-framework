package com.xiongx9527.fw.plus.ai.askdata.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AskDataMetadata {

    private final String dialect;
    private final List<AskDataTable> tables;
    private final List<String> terms;
    private final List<String> examples;
    private final List<String> promptFragments;

    public AskDataMetadata(String dialect, List<AskDataTable> tables, List<String> terms,
            List<String> examples, List<String> promptFragments) {
        this.dialect = dialect;
        this.tables = copy(tables);
        this.terms = copy(terms);
        this.examples = copy(examples);
        this.promptFragments = copy(promptFragments);
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? Collections.<T>emptyList()
                : Collections.unmodifiableList(new ArrayList<T>(values));
    }

    public String getDialect() { return dialect; }
    public List<AskDataTable> getTables() { return tables; }
    public List<String> getTerms() { return terms; }
    public List<String> getExamples() { return examples; }
    public List<String> getPromptFragments() { return promptFragments; }
}
