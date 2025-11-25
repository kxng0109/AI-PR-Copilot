package io.github.kxng0109.aiprcopilot.service;

import io.github.kxng0109.aiprcopilot.error.CustomApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.nio.channels.UnresolvedAddressException;

/**
 * Service class for interacting with AI models via a client library.
 * <p>
 * Provides methods to call AI models with specific inputs, configurations, and error handling.
 */
@Service
@Slf4j
class AiChatService {

    /**
     * Executes a call to an AI model using the specified prompt, client, and options.
     *
     * @param prompt      the input for the AI model, must not be {@code null}
     * @param chatClient  the client to interact with the AI model, must not be {@code null}
     * @param chatOptions additional configuration for the AI model invocation, must not be {@code null}
     * @return the response from the AI model, never {@code null}
     * @throws CustomApiException if the remote service address is unresolved or access to the resource fails
     * @throws RuntimeException   if an unexpected error occurs during the remote call
     */
    public ChatResponse callAiModel(Prompt prompt, ChatClient chatClient, ChatOptions chatOptions) {
        try {
            return chatClient.prompt(prompt)
                             .options(chatOptions)
                             .call()
                             .chatResponse();
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
