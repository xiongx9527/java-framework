package dev.langchain4j.agentic.supervisor;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ResponseAgent {

    @UserMessage(
            "You are a response evaluator that is provided with two responses to a user request.\n"
                    + "Your role is to score the two responses based on their relevance for the user request.\n"
                    + "\n"
                    + "For each of the two responses, response1 and response2, you will return a score, respectively score 1 and score 2,\n"
                    + "between 0.0 and 1.0, where 0.0 means the response is completely irrelevant to the user request,\n"
                    + "and 1.0 means the response is perfectly relevant to the user request.\n"
                    + "\n"
                    + "Return only the score and nothing else, without any additional text or explanation.\n"
                    + "\n"
                    + "The user request is: '{{request}}'.\n"
                    + "The first response is: '{{response1}}'.\n"
                    + "The second response is: '{{response2}}'.\n")
    ResponseScore scoreResponses(@V("request") String request, @V("response1") String response1, @V("response2") String response2);
}
