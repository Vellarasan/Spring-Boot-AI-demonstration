package io.vels.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1")
public class MainController {

    Logger log = LoggerFactory.getLogger(MainController.class);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("classpath:/prompts/simple-prompts.st")
    private Resource simplePromptTemplate;

    @Value("classpath:/prompts/rag-prompt.st")
    private Resource ragPromptTemplate;

    public MainController(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    @GetMapping("/chatResponseAsMap")
    public Map<String, Object> chatResponseAsMap(@RequestParam(value = "utility-name", defaultValue = "financial") String utilityName) {


        log.info("Constructing Prompt...");

        MapOutputConverter mapOutputConverter = new MapOutputConverter();
        String format = mapOutputConverter.getFormat();
        Prompt prompt = new PromptTemplate(simplePromptTemplate)
                .create(Map.of("utility-name", utilityName, "format", format));

        ChatResponse chatResponse = invokeOpenAI(prompt);

        if (chatResponse != null) {
            Generation generation = chatResponse.getResult();
            return mapOutputConverter.convert(generation.getOutput().getContent());
        }
        return Collections.emptyMap();
    }


    @GetMapping("chatUsingRag")
    public String chatUsingRag(@RequestParam(value = "q", defaultValue = "What is the latest release note version?") String question) {

        // Get the similar vectors using the simple vector store
        List<Document> similarDocuments = vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(2).build());

        if (similarDocuments == null) {
            return "No similarities identified. Please try again with different input";
        }
        List<String> documentsList = similarDocuments.stream().map(Document::getText).toList();

        // Constructing the Prompts
        PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);

        Prompt prompt = promptTemplate.create(Map.of("input", question, "documents", String.join("\n", documentsList)));

        ChatResponse chatResponse = invokeOpenAI(prompt);

        if (chatResponse != null) {
            return chatResponse.getResult().getOutput().getContent();
        }

        return "No response from AI. Please try again!";
    }

    private ChatResponse invokeOpenAI(Prompt prompt) {
        log.info("Invoking the OpenAI...");
        return chatClient.prompt(prompt).call().chatResponse();
    }
}
