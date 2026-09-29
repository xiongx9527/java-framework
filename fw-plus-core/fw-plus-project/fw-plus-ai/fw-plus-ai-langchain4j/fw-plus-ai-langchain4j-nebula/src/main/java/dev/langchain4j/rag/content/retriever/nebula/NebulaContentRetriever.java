package dev.langchain4j.rag.content.retriever.nebula;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.graph.nebula.NebulaGraph;
import dev.langchain4j.store.graph.nebula.NebulaUtils;
import lombok.Builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

/**
 * 从 {@link NebulaGraph} 检索内容的 {@link ContentRetriever}。
 * 它把自然语言问题转成 NebulaGraph 的 nGQL 查询，执行后把结果转成 {@link Content}。
 */
public class NebulaContentRetriever implements ContentRetriever {

    private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from(
            "Based on the NebulaGraph schema below, write an nGQL query that would answer the user's question:\n"
                    + "{{schema}}\n\n"
                    + "Question: {{question}}\n"
                    + "nGQL query:\n");

    private static final Pattern BACKTICKS_PATTERN = Pattern.compile("```(.*?)```", Pattern.MULTILINE | Pattern.DOTALL);

    private final NebulaGraph graph;

    private final ChatModel chatLanguageModel;

    private final PromptTemplate promptTemplate;

    @Builder
    public NebulaContentRetriever(NebulaGraph graph,
                                  ChatModel chatLanguageModel,
                                  PromptTemplate promptTemplate) {

        this.graph = ensureNotNull(graph, "graph");
        this.chatLanguageModel = ensureNotNull(chatLanguageModel, "chatLanguageModel");
        this.promptTemplate = getOrDefault(promptTemplate, DEFAULT_PROMPT_TEMPLATE);
    }

    @Override
    public List<Content> retrieve(Query query) {

        String question = query.text();
        String schema = graph.getSchema();
        String nGqlQuery = generateNebulaQuery(schema, question);
        List<String> response = executeQuery(nGqlQuery);
        return response.stream().map(Content::from).collect(Collectors.toList());
    }

    private String generateNebulaQuery(String schema, String question) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("schema", schema);
        variables.put("question", question);
        Prompt nGqlPrompt = promptTemplate.apply(variables);
        String nGqlQuery = chatLanguageModel.chat(nGqlPrompt.text());
        Matcher matcher = BACKTICKS_PATTERN.matcher(nGqlQuery);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return nGqlQuery;
    }

    private List<String> executeQuery(String nGqlQuery) {

        return graph.executeRead(nGqlQuery).stream()
                .flatMap(record -> record.values().stream())
                .map(NebulaUtils::toText)
                .filter(text -> !text.isEmpty())
                .collect(Collectors.toList());
    }
}
