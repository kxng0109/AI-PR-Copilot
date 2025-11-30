package io.github.kxng0109.aiprcopilot.service;

import io.github.kxng0109.aiprcopilot.config.MultiAiConfigurationProperties;
import io.github.kxng0109.aiprcopilot.error.CustomApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service class for interacting with AI models via a client library.
 * <p>
 * Provides methods to call AI models with specific inputs, configurations, and error handling.
 */
@Service
@Slf4j
@RequiredArgsConstructor
class AiChatService {

    private final MultiAiConfigurationProperties aiConfigurationProperties;

    /**
     * Executes a call to an AI model using the specified prompt, client, and options.
     *
     * @param prompt      the prompt to send to the AI model, must not be {@code null}
     * @param chatClient  the {@code ChatClient} used to interact with the AI model, must not be {@code null}
     * @param chatOptions the options for configuring the AI call, must not be {@code null}
     * @return the {@code ChatResponse} from the AI model, never {@code null}
     * @throws CustomApiException if the request fails due to timeouts, address resolution issues, or resource access errors
     * @throws RuntimeException   if any unexpected errors occur during the call
     */
    public ChatResponse callAiModel(Prompt prompt, ChatClient chatClient, ChatOptions chatOptions) {
        log.debug("Request timeout set: {}", aiConfigurationProperties.getTimeoutMillis());
        try {
            return CompletableFuture.supplyAsync(() ->
                                                         chatClient.prompt(prompt)
                                                                   .options(chatOptions)
                                                                   .call()
                                                                   .chatResponse()
            ).get(aiConfigurationProperties.getTimeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.error("AI Model timed out after {} milliseconds", aiConfigurationProperties.getTimeoutMillis());
            throw new CustomApiException("AI Model request timed out", HttpStatus.GATEWAY_TIMEOUT, e);
        } catch (UnresolvedAddressException e) {
            log.error("Failed to resolve remote service address: {}", e.getMessage(), e);
            throw new CustomApiException("Failed to resolve remote service address: " + e.getMessage(),
                                         HttpStatus.BAD_GATEWAY, e
            );
        } catch (ResourceAccessException e) {
            log.error("Failed to access remote resource: {}", e.getMessage(), e);
            HttpStatus status = e.getCause() instanceof java.net.SocketTimeoutException
                    ? HttpStatus.GATEWAY_TIMEOUT
                    : HttpStatus.BAD_GATEWAY;
            throw new CustomApiException("Failed to access remote resource: " + e.getMessage(), status, e);
        } catch (Exception e) {
            log.error("Unexpected error during remote call: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error during remote call: " + e.getMessage(), e);
        }
    }
}
