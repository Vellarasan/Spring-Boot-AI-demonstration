package io.vels.ai.controller;

import io.vels.ai.tools.DateTimeTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/chat")
public class ChatModelController {

    Logger log = LoggerFactory.getLogger(ChatModelController.class);

    // The ChatClient bean is configured under config/ChatClientConfig
    private final ChatClient chatClient;

    // The VectorStore bean is configured under config/RagConfig or from the application property for pgVector Store
    private final VectorStore vectorStore;

    @Value("classpath:/prompts/simple-prompts.st")
    private Resource simplePromptTemplate;

    @Value("classpath:/prompts/rag-prompt.st")
    private Resource ragPromptTemplate;

    @Value("classpath:/prompts/pdf-rag-prompt.st")
    private Resource ragPromptTemplateForPdf;

    public ChatModelController(@Qualifier("openAiChatClient") ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    @GetMapping("/simple-chat")
    public String simpleChat(@RequestParam(value = "question", defaultValue = "Tell me a joke") String question) {
        ChatResponse chatResponse = chatClient.prompt(question).call().chatResponse();
        return chatResponse != null ? chatResponse.getResult().getOutput().getText() : "No response received. Try again";
    }

    @GetMapping("/chatResponseAsMap")
    public Map<String, Object> chatResponseAsMap(@RequestParam(value = "utility-name", defaultValue = "financial") String utilityName) {


        log.info("Constructing Prompt...");

        MapOutputConverter mapOutputConverter = new MapOutputConverter();
        String format = mapOutputConverter.getFormat();
        Prompt prompt = new PromptTemplate(simplePromptTemplate)
                .create(Map.of("utility-name", utilityName, "format", format));

        ChatResponse chatResponse = invokeAIClientWithPrompt(prompt);

        if (chatResponse != null) {
            Generation generation = chatResponse.getResult();
            return mapOutputConverter.convert(generation.getOutput().getText());
        }
        return Collections.emptyMap();
    }

    @GetMapping("/chatUsingRag")
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

        ChatResponse chatResponse = invokeAIClientWithPrompt(prompt);

        if (chatResponse != null) {
            return chatResponse.getResult().getOutput().getText();
        }

        return "No response from AI. Please try again!";
    }

    @GetMapping("/withToolCalling")
    public String chatWithFunction(
            @RequestParam(value = "question", defaultValue = "What is the current time in Allen, Texas?") String question) {

        return invokeAIClientWithTools(question);
    }

    @GetMapping("/withTakeAction")
    public String chatWithTakeAction(
            @RequestParam(value = "question", defaultValue = "Set the alarm to 4 hours from now") String question) {

        return invokeAIClientWithTools(question);
    }

    @GetMapping("/withPgVector")
    public String withPgVectorStore(@RequestParam(value = "pdfUrl", defaultValue = "https://xml.apache.org/xindice/license.pdf") String pdfUrl,
                                    @RequestParam(value = "q", defaultValue = "Summarize what is covered for iphone?") String question) {

        // Read the pdf file
        PagePdfDocumentReader pagePdfDocumentReader = new PagePdfDocumentReader(pdfUrl);

        // Add the documents to PGVector
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        vectorStore.accept(tokenTextSplitter.split(pagePdfDocumentReader.get()));

        log.info("Vector DB loaded with the PDF file provided");

        log.info("Constructing the Prompt...");
        // Prompt construction
        PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplateForPdf);
        Prompt prompt = promptTemplate.create(Map.of("input", question, "documents", findSimilarities(question)));

        log.info("Calling the OpenAI with similarities...");
        return chatClient.prompt(prompt).call().content();

    }

    private String findSimilarities(String question) {

        log.info("Loading similarities for the question:::: " + question);
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(3).build());

        return documents.stream()
                .map(eachDoc -> eachDoc.getFormattedContent())
                .collect(Collectors.joining());
    }

    private String invokeAIClientWithTools(String question) {
        return chatClient
                .prompt(question)
                .tools(new DateTimeTools())
                .call()
                .content();
    }

    private ChatResponse invokeAIClientWithPrompt(Prompt prompt) {
        log.info("Invoking the OpenAI...");
        return chatClient.prompt(prompt).call().chatResponse();
    }
}
