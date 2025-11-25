package io.github.kxng0109.aiprcopilot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Service for constructing {@code Prompt} objects to analyze Git diffs.
 * <p>
 * This service reads system-level prompt templates and combines them with user-provided
 * parameters to create structured prompts that adhere to the required analysis details.
 */
@Service
@Slf4j
class PromptBuilderService {

    @Value("${prcopilot.prompts.system-prompt}")
    private Resource systemPromptResource;

    /**
     * Builds a {@code Prompt} for analyzing a Git diff based on the provided parameters.
     * <p>
     * Combines system-level prompt templates with dynamically generated user messages
     * to construct a structured {@code Prompt} for diff analysis.
     *
     * @param language         the programming language associated with the diff, must not be {@code null} or empty
     * @param style            the style or tone to use in the analysis, must not be {@code null} or empty
     * @param diff             the Git diff content to be analyzed, must not be {@code null} or empty
     * @param maxSummaryLength the optional maximum length for the summary in the analysis, may be {@code null}
     * @param requestId        the optional request identifier to trace this analysis, may be {@code null} or blank
     * @return a {@code Prompt} object containing structured messages ready for diff analysis, never {@code null}
     */
    public Prompt buildDiffAnalysisPrompt(
            String language,
            String style,
            String diff,
            Integer maxSummaryLength,
            String requestId
    ) {
        SystemPromptTemplate promptTemplate = new SystemPromptTemplate(loadSystemPrompt());
        Message systemMessage = promptTemplate.createMessage(
                Map.of("language", language, "style", style)
        );

        StringBuilder userContent = new StringBuilder();
        userContent.append("Please analyze this Git diff with strict adherence to instructions.\n");
        userContent.append("language: ").append(language).append("\n");
        userContent.append("style: ").append(style).append("\n");
        if (maxSummaryLength != null) {
            userContent.append("maxSummaryLength: ").append(maxSummaryLength).append("\n");
        }
        if (requestId != null && !requestId.isBlank()) {
            userContent.append("requestId: ").append(requestId).append("\n");
        }
        userContent.append("Diff: ```").append(diff).append("\n```");

        UserMessage userMessage = new UserMessage(userContent.toString());

        return new Prompt(
                List.of(systemMessage, userMessage)
        );
    }


    /**
     * Loads the system prompt content from a resource.
     *
     * @return the loaded system prompt content as a {@code String}, never {@code null}
     * @throws RuntimeException if an I/O error occurs while reading the resource
     */
    private String loadSystemPrompt() {
        try {
            return new String(systemPromptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Could not load system prompt: {}", e.getMessage(), e);
            throw new RuntimeException("Could not load system prompt.", e);
        }
    }
}
