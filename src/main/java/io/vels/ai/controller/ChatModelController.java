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

    private final ChatClient convChatClient;

    // The VectorStore bean is configured under config/RagConfig or from the application property for pgVector Store
    private final VectorStore vectorStore;

    @Value("classpath:/prompts/simple-prompts.st")
    private Resource simplePromptTemplate;

    @Value("classpath:/prompts/rag-prompt.st")
    private Resource ragPromptTemplate;

    @Value("classpath:/prompts/pdf-rag-prompt.st")
    private Resource ragPromptTemplateForPdf;

    public ChatModelController(ChatClient chatClient,
                               @Qualifier("convChatClient") ChatClient convChatClient,
                               VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.convChatClient = convChatClient;
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

    /**
     * Handles GET requests at the "/chatUsingRag" endpoint and initiates a chat operation using the RAG model.
     * It takes a question as a request parameter and uses it to search for similar vectors in the document store.
     * It then constructs a prompt template and initiates a chat operation with the AI client.
     * If a valid chat response is received, it is returned as text, otherwise, a default message is returned.
     *
     * @param question The question to be asked to the AI model. It is used to search for similar vectors in the document store.
     *                 If no question is provided, the default question "What is the latest release note version?" is used.
     * @return A string representing the AI's response to the question. If no similar vectors are found, returns
     * "No similarities identified. Please try again with different input". If no response is received from the AI,
     * returns "No response from AI. Please try again!".
     */
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

    @GetMapping("/conv/withPdf")
    public String convWithPdf(@RequestParam(value = "pdfUrl", required = false) String pdfUrl,
                              @RequestParam(value = "q", defaultValue = "Summarize what is covered for iphone?") String question) {

        // Read the pdf file
        PagePdfDocumentReader pagePdfDocumentReader = null;
        if (pdfUrl != null || pdfUrl != "") {
            pagePdfDocumentReader = new PagePdfDocumentReader(pdfUrl);
            // Add the documents to PGVector
            TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
            vectorStore.accept(tokenTextSplitter.split(pagePdfDocumentReader.get()));
            log.info("Vector DB loaded with the PDF file provided");
        } else {
            log.info("Conversation mode enabled since no pdf file is provided");
        }

        log.info("Constructing the Prompt...");
        // Prompt construction
        PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplateForPdf);
        Prompt prompt = promptTemplate.create(Map.of("input", question, "documents", findSimilarities(question)));

        log.info("Calling the OpenAI with similarities...");
        return convChatClient.prompt(prompt).call().content();
    }

    /**
     * Fetches a set of documents that are similar to the input question string by using a similarity search on a vector store.
     * It then formats the content of these documents into a single string.
     *
     * @param question The question string to be used for finding similar documents in the vector store.
     *                 This is the query that will be used for the similarity search.
     * @return A single string that is a concatenation of the formatted content of all the documents found
     * to be similar to the input question string.
     */
    private String findSimilarities(String question) {

        log.info("Loading similarities for the question:::: " + question);
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(3).build());

        return documents.stream()
                .map(eachDoc -> eachDoc.getFormattedContent())
                .collect(Collectors.joining());
    }

    /**
     * This method invokes the AI chat client with a given question and uses DateTimeTools for the interaction.
     * It makes a call to the chat client and returns the content of the response.
     *
     * @param question The question that is to be asked to the AI chat client. It should not be null.
     * @return The content of the response from the AI chat client after asking the question. If the chat client
     * doesn't return a response, this method will return null.
     */
    private String invokeAIClientWithTools(String question) {
        return chatClient
                .prompt(question)
                .tools(new DateTimeTools())
                .call()
                .content();
    }

    /**
     * This method is used to invoke the OpenAI client with a given prompt.
     * The method logs the initiation of the OpenAI invocation and then proceeds to call the client.
     * The response from the OpenAI client is returned as a ChatResponse object.
     *
     * @param prompt The Prompt object which contains the information needed to initiate a conversation with the OpenAI.
     *               It is expected to be a non-null value.
     * @return A ChatResponse object which encapsulates the response from the OpenAI client. The returned object includes
     * details about the chat interactions.
     */
    private ChatResponse invokeAIClientWithPrompt(Prompt prompt) {
        log.info("Invoking the OpenAI...");
        return chatClient.prompt(prompt).call().chatResponse();
    }
}
