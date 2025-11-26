package io.github.kxng0109.aiprcopilot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration class for initializing AI chat clients and options.
 *
 * <p>Integrates with multiple AI providers, including OpenAI, Anthropic, Gemini, and Ollama.
 * Automatically selects and configures the primary and optional fallback clients and options
 * based on {@code MultiAiConfigurationProperties}.
 *
 * <p>This configuration requires valid properties for the desired providers to be set.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class AiChatClientConfig {

    private final MultiAiConfigurationProperties multiAiConfigurationProperties;

    private final OpenAiChatModel openAiChatModel;
    private final AnthropicChatModel anthropicChatModel;
    private final VertexAiGeminiChatModel vertexAiGeminiChatModel;
    private final OllamaChatModel ollamaChatModel;

    /**
     * Constructs the primary {@code ChatClient} based on the selected AI provider.
     *
     * <p>Determines the appropriate AI chat model by consulting the
     * {@code multiAiConfigurationProperties} and builds a {@code ChatClient} instance using it.
     *
     * @return the primary {@code ChatClient} instance, never {@code null}
     */
    @Primary
    @Bean
    public ChatClient primaryChatClient() {
        ChatModel primaryChatModel = chooseChatModel(multiAiConfigurationProperties.getProvider());
        return ChatClient.builder(primaryChatModel).build();
    }

    /**
     * Constructs the primary {@code ChatOptions} based on the configured AI provider.
     *
     * <p>Determines the appropriate chat configuration for the selected provider by consulting
     * {@code multiAiConfigurationProperties} and initializing a {@code ChatOptions} instance
     * with the relevant parameters.
     *
     * @return the primary {@code ChatOptions} instance, never {@code null}
     */
    @Primary
    @Bean
    public ChatOptions primaryChatOptions() {
        return constructChatOption(multiAiConfigurationProperties.getProvider());
    }

    /**
     * Constructs a fallback {@code ChatClient} instance based on the configured AI provider.
     *
     * <p>Auto-fallback mechanism ensures the availability of a {@code ChatClient} even when
     * the primary provider is unavailable. The fallback provider must be explicitly
     * configured via {@code multiAiConfigurationProperties}.
     *
     * @return the fallback {@code ChatClient} instance, never {@code null}
     * @throws IllegalStateException if auto-fallback is enabled but no fallback provider is configured
     */
    @Bean
    @ConditionalOnProperty(name = "prcopilot.ai.auto-fallback", havingValue = "true")
    public ChatClient fallbackChatClient() {
        if (multiAiConfigurationProperties.getFallbackProvider() == null) {
            throw new IllegalStateException(
                    "Auto-fallback is enabled but no fallback provider is configured. Please set PRCOPILOT_AI_FALLBACK_PROVIDER or disable auto-fallback."
            );
        }

        ChatModel fallBackChatModel = chooseChatModel(multiAiConfigurationProperties.getFallbackProvider());
        return ChatClient.builder(fallBackChatModel).build();
    }

    /**
     * Constructs a fallback {@code ChatOptions} instance based on the configured AI provider.
     *
     * <p>Creates a {@code ChatOptions} instance if the auto fallback mechanism is enabled and
     * a fallback provider is specified in {@code multiAiConfigurationProperties}.
     *
     * @return the fallback {@code ChatOptions} instance, never {@code null}
     * @throws IllegalStateException if auto fallback is enabled but no fallback provider is configured
     */
    @Bean
    @ConditionalOnProperty(name = "prcopilot.ai.auto-fallback", havingValue = "true")
    public ChatOptions fallbackChatOptions() {
        if (multiAiConfigurationProperties.getFallbackProvider() == null) {
            throw new IllegalStateException(
                    "Auto-fallback is enabled but no fallback provider is configured. Please set PRCOPILOT_AI_FALLBACK_PROVIDER or disable auto-fallback."
            );
        }

        return constructChatOption(multiAiConfigurationProperties.getFallbackProvider());
    }


    /**
     * Determines the appropriate chat model based on the specified AI provider.
     *
     * @param provider the {@code AiProvider} indicating which chat model to select; must not be {@code null}
     * @return the corresponding {@code ChatModel} for the specified {@code provider}, never {@code null}
     * @throws IllegalArgumentException if the specified {@code provider} is unsupported or
     *                                  if the corresponding chat model is not properly configured
     */
    private ChatModel chooseChatModel(AiProvider provider) {
        return switch (provider) {
            case OPENAI -> {
                if (openAiChatModel == null) {
                    throw new IllegalArgumentException(
                            "OpenAI provider is selected but not configured. Set OPENAI_API_KEY." +
                                    "Check .env.example for more details."
                    );
                }

                yield openAiChatModel;
            }

            case ANTHROPIC -> {
                if (anthropicChatModel == null) {
                    throw new IllegalArgumentException(
                            "Anthropic provider is selected but not configured. Set ANTHROPIC_API_KEY." +
                                    "Check .env.example for more details."
                    );
                }

                yield anthropicChatModel;
            }

            case GEMINI -> {
                if (vertexAiGeminiChatModel == null) {
                    throw new IllegalArgumentException(
                            "Gemini provider is selected but not configured. Set up GCP credentials." +
                                    "Check .env.example for more details."
                    );
                }

                yield vertexAiGeminiChatModel;
            }

            case OLLAMA -> {
                if (ollamaChatModel == null) {
                    throw new IllegalArgumentException(
                            "Ollama provider is selected but not configured. Set up OLLAMA_MODEL and ensure Ollama is running." +
                                    "Check .env.example for more details.");
                }

                yield ollamaChatModel;
            }
        };
    }

    /**
     * Constructs a {@code ChatOptions} instance configured for the specified {@code AiProvider}.
     *
     * <p>Determines the appropriate options for the given provider by utilizing
     * {@code multiAiConfigurationProperties} to configure parameters such as temperature
     * and token limits.
     *
     * @param provider the {@code AiProvider} for which the chat options are to be created; must not be {@code null}
     * @return a {@code ChatOptions} instance configured for the given {@code provider}, never {@code null}
     * @throws IllegalArgumentException if the specified {@code provider} is unsupported
     */
    private ChatOptions constructChatOption(AiProvider provider) {
        return switch (provider) {
            case OPENAI -> OpenAiChatOptions.builder()
                                            .temperature(multiAiConfigurationProperties.getTemperature())
                                            .maxTokens(multiAiConfigurationProperties.getMaxTokens())
                                            .build();

            case ANTHROPIC -> AnthropicChatOptions.builder()
                                                  .temperature(multiAiConfigurationProperties.getTemperature())
                                                  .maxTokens(multiAiConfigurationProperties.getMaxTokens())
                                                  .build();

            case GEMINI -> VertexAiGeminiChatOptions.builder()
                                                    .temperature(multiAiConfigurationProperties.getTemperature())
                                                    .maxOutputTokens(multiAiConfigurationProperties.getMaxTokens())
                                                    .build();

            case OLLAMA -> OllamaChatOptions.builder()
                                            .temperature(multiAiConfigurationProperties.getTemperature())
                                            .numPredict(multiAiConfigurationProperties.getMaxTokens())
                                            .build();
        };
    }
}
