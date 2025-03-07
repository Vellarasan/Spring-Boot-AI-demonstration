# Spring-Boot AI Demonstration

Welcome to the Spring-Boot AI Demonstration project! This repository showcases an integrated solution using Spring Boot to illustrate the power of incorporating artificial intelligence capabilities, specifically through language and image models. Whether you’re a seasoned AI developer or just exploring the potential of AI integrations, this demonstration will help you understand the basic implementations and configurations necessary to leverage AI functionalities in your applications.

## Project Requirements

To utilize this Spring Boot AI demonstration project, ensure you meet the following prerequisites:

- Java Development Kit (JDK) version 23
- Apache Maven (the wrapper is provided for convenience)
- A valid OpenAI API key

## Dependencies

This project leverages a variety of dependencies to enhance its capabilities, including but not limited to:

- Spring Boot Starter Web: For building web applications
- Spring Boot Starter AOP: For aspect-oriented programming capabilities
- Spring AI Ollama and OpenAI Spring Boot Starters: For integrating AI models
- Spring Boot Starter Test: For testing purposes

## Getting Started

To get up and running quickly, follow these steps:

1. **Set Environment Variables**: Ensure your environment includes a valid OpenAI API key as follows:
    ```bash
    export OPENAI_API_KEY=your_openai_api_key_here
    ```

2. **Configure Application**: Verify your configurations in `application.yml` are set correctly, particularly paths related to prompts and vector stores.

## How to Run the Application

The application includes a RESTful service that can be accessed via a web client. Follow these commands to launch and interact with the application:

1. **Build and Package**:
    ```bash
    ./mvnw clean package
    ```

2. **Run the Application**:
    ```bash
    ./mvnw spring-boot:run
    ```

3. **Access Endpoints**:
   - Chat endpoint: `GET /api/v1/chat/simple-chat` - Interact with AI to get responses on dynamic inputs.
   - Image generation endpoint: `GET /api/v1/image/generate-caption` - Use this to generate captions for images.

## Relevant Code Examples

Here's a snippet demonstrating how the `ChatModelController` interacts with AI models for generating chat responses:

```java
@GetMapping("/simple-chat")
public String simpleChat(@RequestParam(value = "question", defaultValue = "Tell me a joke") String question) {
    ChatResponse chatResponse = chatClient.prompt(question).call().chatResponse();
    return chatResponse != null ? chatResponse.getResult().getOutput().getContent() : "No response received. Try again";
}
```

## Logging with Aspects
An example of logging configuration through aspects is showcased in `LoggingAspect.java` to monitor method entries and exits, providing insights into method calls and arguments:
```
@Around("allPublicMethods()")
private Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
    log.info("Entering Method >> {}() - with Args :: {}", methodName, Arrays.toString(args));
    Object result = joinPoint.proceed();
    log.info("Exiting Method << {}()", methodName);
    return result;
}
```

## Conclusion
This project illustrates a practical implementation of AI with Spring Boot, leveraging modern techniques in aspect-oriented programming and AI model integration. Whether you are looking to extend your application’s capabilities with AI or seeking a reference implementation, this repository serves as a comprehensive guide. Explore, modify, and build upon this demonstration to suit your specific AI application needs.

Feel free to explore the codebase and contribute enhancements or ideas!

### RELEASE NOTES:
### FEATURE
- Added DALL-E configuration for generating more realistic images.
- Added pgvector store for persisting the vector tokens.
- Added endpoint to accept any PDF URL and answer questions from it.
- Updated the Spring AI dependency version to support `@Tool` and Action calling.
  - Implemented basic tool and action-flow integration.
- Enhanced project capabilities for handling multiple models.
  - Included support for Ollama with the deepseek-r1 model.
  - Enabled manual model switching between OpenAI and Ollama.
- Enhanced model to gpt-4o for improved image processing.
- Enhanced project with RAG (Retrieval-augmented generation) using SimpleVectorStore.
- Enhanced project with prompt templates and output parsers.
  - Included string template files for prompts.
- Introduced basic chat integration with OpenAI.

### BUG-FIX
- Identified and addressed minor issues with Ollama on OutputParser when using Map.

### REFACTOR
- Basic code cleanup.
  - Removed unnecessary commented code.
- Introduced APO changes for logging around public methods.
- Minor refactors.

### DOCUMENTATION
- Created README.md file for project documentation.

### TODO
- Clean up many commented codes.
