package io.vels.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/image")
public class ImageModelController {

    Logger log = LoggerFactory.getLogger(ImageModelController.class);

    // The ChatClient bean is configured under config/ChatClientConfig
    private final ChatClient chatClient;

    @Value("classpath:/prompts/image-caption-generation-prompt.st")
    private String imageCaptionGenerationPrompt;

    public ImageModelController(@Qualifier("ollamaChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }


    @GetMapping("/generate-caption")
    public String generateCaption(@RequestParam(value = "platform", defaultValue = "instagram") String platformName) {

        log.info("Constructing Prompt...");

        // Getting the image from the resources and convert it as byte array
        ClassPathResource imageResource = new ClassPathResource("images/img.png");

        // Construct the User Prompt
        Message message =
                new PromptTemplate(imageCaptionGenerationPrompt, Map.of("platform", platformName))
                        .createMessage(List.of(new Media(MimeTypeUtils.IMAGE_PNG, imageResource)));

        Prompt prompt = new Prompt(message);

        ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();
        return chatResponse != null ? chatResponse.getResult().getOutput().getContent() : "No response received! Please try again.";

    }
}
