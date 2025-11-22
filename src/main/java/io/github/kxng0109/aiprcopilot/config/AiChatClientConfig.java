package io.github.kxng0109.aiprcopilot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChatClientConfig {

    @Bean
    public ChatOptions openAiChatOptions(AiGenerationProperties aiGenerationProperties) {
        return OpenAiChatOptions.builder()
                .temperature(aiGenerationProperties.getTemperature())
                .maxTokens(aiGenerationProperties.getMaxTokens())
                .build();
    }

    @Bean
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
