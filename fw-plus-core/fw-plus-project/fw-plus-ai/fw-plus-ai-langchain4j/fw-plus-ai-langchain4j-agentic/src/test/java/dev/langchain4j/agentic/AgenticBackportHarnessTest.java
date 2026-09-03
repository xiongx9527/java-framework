package dev.langchain4j.agentic;

import dev.langchain4j.agentic.internal.CollectionHelperTest;
import dev.langchain4j.agentic.internal.StringHelperTest;
import dev.langchain4j.agentic.internal.Task2a3RuntimeContractTest;
import org.junit.jupiter.api.Test;

class AgenticBackportHarnessTest {

    @Test
    void collectionHelperContract() {
        CollectionHelperTest.main(new String[0]);
    }

    @Test
    void stringHelperContract() {
        StringHelperTest.main(new String[0]);
    }

    @Test
    void task2a1SourceContract() throws Exception {
        Task2a1SourceContractTest.main(new String[0]);
    }

    @Test
    void task2a1RuntimeContract() throws Exception {
        Task2a1RuntimeContractTest.main(new String[0]);
    }

    @Test
    void task2a2SourceContract() throws Exception {
        Task2a2SourceContractTest.main(new String[0]);
    }

    @Test
    void task2a2RuntimeContract() throws Exception {
        Task2a2RuntimeContractTest.main(new String[0]);
    }

    @Test
    void task2a3MetadataContract() throws Exception {
        Task2a3MetadataContractTest.main(new String[0]);
    }

    @Test
    void task2a3RuntimeContract() throws Exception {
        Task2a3RuntimeContractTest.main(new String[0]);
    }
}
