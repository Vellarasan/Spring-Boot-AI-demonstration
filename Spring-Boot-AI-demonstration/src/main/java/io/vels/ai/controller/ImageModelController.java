package io.vels.ai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
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
import java.util.Optional;

@RestController
@RequestMapping("api/v1/image")
public class ImageModelController {

    Logger log = LoggerFactory.getLogger(ImageModelController.class);

    // The ChatClient bean is configured under config/ChatClientConfig
    private final ChatClient chatClient;

    //
    private final ImageModel imageModel;

    @Value("classpath:/prompts/image-caption-generation-prompt.st")
    private String imageCaptionGenerationPrompt;

    public ImageModelController(@Qualifier("openAiChatClient") ChatClient chatClient, ImageModel imageModel) {
        this.chatClient = chatClient;
        this.imageModel = imageModel;
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
        return chatResponse != null ? chatResponse.getResult().getOutput().getText() : "No response received! Please try again.";

    }

    @GetMapping("/generate-image")
    public String generate(@RequestParam(value = "desc", defaultValue = "Generate an image of male angel holding a girl baby inside a dense forest") String prompt) {

        ImagePrompt imagePrompt = new ImagePrompt(prompt);
        ImageResponse imageResponse = imageModel.call(imagePrompt);
        return resolveImageContent(imageResponse);

    }

    private String resolveImageContent(ImageResponse imageResponse) {
        Image image = imageResponse.getResult().getOutput();
        return Optional
                .ofNullable(image.getUrl())
                .orElseGet(image::getB64Json);
    }
}
