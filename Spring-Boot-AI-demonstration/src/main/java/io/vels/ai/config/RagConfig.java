package io.vels.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Configuration
public class RagConfig {

    Logger log = LoggerFactory.getLogger(RagConfig.class);

    @Value("classpath:/docs/spring-ai-upgrade-notes.txt")
    private Resource springAiReleaseNotes;

    @Value("${io.vels.ai.vectorstore.filename}")
    private String vectorStoreFileName;

    /**
     * Switch between ollamaEmbeddingModel or openAiEmbeddingModel based on need.
     *
     * @param embeddingModel
     * @return
     */
    @Bean
    SimpleVectorStore simpleVectorStore(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();

        // Get the doc for embedding...
        File vectorStoreFile = getVectorStoreFile();

        if (vectorStoreFile.exists()) {
            log.info("Vector Store File already exist, loading it now...");
            simpleVectorStore.load(vectorStoreFile);
        } else {
            log.info("Vector Store File not found, loading the document to vector store...");

            // Read the text file
            TextReader textReader = new TextReader(springAiReleaseNotes);

            // Setting basic meta data
            textReader.getCustomMetadata().put("filename", "spring-ai-release-notes.txt");
            List<Document> documents = textReader.get();

            // Splitting the documents
            log.info("Splitting the documents...");
            TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
            List<Document> splitDocs = tokenTextSplitter.split(documents);

            // Adding the Split Documents to the Simple Vector Store
            log.info("Adding the split documents to the vector store");
            simpleVectorStore.add(splitDocs);
            simpleVectorStore.save(vectorStoreFile);
        }
        return simpleVectorStore;
    }

    private File getVectorStoreFile() {
        Path path = Paths.get("Spring-Boot-AI-demonstration", "src", "main", "resources", "data");
        String absolutePath = path.toFile().getAbsolutePath() + "/" + vectorStoreFileName;
        return new File(absolutePath);
    }

}