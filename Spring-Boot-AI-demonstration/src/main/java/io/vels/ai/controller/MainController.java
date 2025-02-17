package io.vels.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("api/v1")
public class MainController {

    Logger log = LoggerFactory.getLogger(MainController.class);

    private final ChatClient chatClient;

    @Value("classpath:/prompts/simple-prompts.st")
    private Resource simplePromptsResource;

    public MainController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/chatResponseAsMap")
    public Map<String, Object> chatResponseAsMap(@RequestParam(value = "utility-name", defaultValue = "financial") String utilityName) {


        log.info("Constructing Prompt...");

        MapOutputConverter mapOutputConverter = new MapOutputConverter();
        String format = mapOutputConverter.getFormat();
        Prompt prompt = new PromptTemplate(simplePromptsResource)
                .create(Map.of("utility-name", utilityName, "format", format));

        log.info("Invoking the OpenAI...");
        ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();

        if (chatResponse != null) {
            Generation generation = chatResponse.getResult();
            return mapOutputConverter.convert(generation.getOutput().getContent());
        }
        return Collections.emptyMap();
    }
}
