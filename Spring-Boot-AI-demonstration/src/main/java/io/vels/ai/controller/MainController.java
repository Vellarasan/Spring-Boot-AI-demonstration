package io.vels.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
public class MainController {

    Logger log = LoggerFactory.getLogger(MainController.class);

    private final ChatClient chatClient;

    public MainController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/chat")
    public String chat() {
        log.debug("Invoking the Chat AI...");
        Prompt prompt = new Prompt("what is an AI?");
        ChatResponse chatResponse = chatClient.prompt(prompt)
                .call()
                .chatResponse();
        return chatResponse != null ? chatResponse
                .getResult()
                .getOutput()
                .getContent() : "Unable to Complete the Chat! Please try again";
    }
}
