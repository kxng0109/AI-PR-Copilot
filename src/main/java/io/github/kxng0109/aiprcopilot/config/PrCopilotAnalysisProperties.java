package io.github.kxng0109.aiprcopilot.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for pull request copilot analysis.
 * <p>
 * Provides configurable options for analyzing pull request diffs,
 * including constraints on maximum diff size, default language,
 * default style, and whether to include raw model output.
 *
 * <p>Must be loaded using {@code @ConfigurationProperties} with the prefix {@code prcopilot.analysis}.
 *
 * <p>Validation constraints ensure valid property values during initialization.
 *
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "prcopilot.analysis")
public class PrCopilotAnalysisProperties {

    @Min(value = 1, message = "Maximum diff characters must be greater than 1")
    private int maxDiffChars;

    @NotBlank(message = "Default language can not be blank")
    private String defaultLanguage;

    @NotBlank(message = "Default language can not be blank")
    private String defaultStyle;

    private boolean includeRawModelOutput;
}
