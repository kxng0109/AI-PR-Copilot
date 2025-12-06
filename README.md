# AI PR Copilot

A self hosted AI powered code audit and pull request analysis service with multi provider support. This REST API
analyzes Git diffs using language models to provide structured reviews, identify risks, and suggest test cases.

[![CI](https://github.com/kxng0109/AI-PR-Copilot/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/kxng0109/AI-PR-Copilot/actions/workflows/ci.yml)

> Note: This project is under active development. Features and APIs may change as the project evolves.

## Table of Contents

- [Features](#features)
- [Supported AI Providers](#supported-ai-providers)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
    - [Clone the Repository](#clone-the-repository)
    - [Configuration](#configuration)
    - [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
    - [Analyze Diff Endpoint](#analyze-diff-endpoint)
    - [Example Request](#example-request)
    - [Example Response](#example-response)
- [Configuration Reference](#configuration-reference)
    - [AI Provider Configuration](#ai-provider-configuration)
    - [Analysis Settings](#analysis-settings)
    - [Fallback Configuration](#fallback-configuration)
- [Provider Setup Guides](#provider-setup-guides)
    - [OpenAI](#openai)
    - [Anthropic Claude](#anthropic-claude)
    - [Google Gemini](#google-gemini)
    - [Ollama](#ollama)
- [Error Handling](#error-handling)
- [Health Checks](#health-checks)
- [CI](#ci)
- [Testing](#testing)
- [Architecture Overview](#architecture-overview)
- [License](#license)

## Features

- Multi provider AI support: OpenAI, Anthropic Claude, Google Gemini, Ollama
- Optional automatic fallback between providers
- Structured code analysis: title, summary, details, risks, suggested tests, touched files, metadata
- Configurable language, style, temperature, token limits, and raw model output inclusion
- Validation and diff size limits with centralized error handling
- OpenAPI documentation via Swagger UI
- Startup validation for provider configuration

## Supported AI Providers

| Provider      | Models                                    | Authentication                 |
|---------------|-------------------------------------------|--------------------------------|
| OpenAI        | GPT-4o, GPT-4o-mini, GPT-3.5-turbo        | API Key                        |
| Anthropic     | Claude Sonnet 4, Claude Opus 4            | API Key                        |
| Google Gemini | Gemini 2.0 Flash, Gemini Pro              | GCP Project ID and credentials |
| Ollama        | Local models (Qwen, Llama, Mistral, etc.) | Local installation             |

## Prerequisites

- Java 25 or higher
- Maven 3.6 or higher
- API key for at least one provider or a running Ollama instance

## Getting Started

### Clone the Repository

```bash
git clone https://github.com/kxng0109/ai-pr-copilot.git
cd ai-pr-copilot
```

### Configuration

1) Copy the example environment file:

```bash
cp .env.example .env
```

2) Edit `.env` and set your provider credentials. Minimal example:

```bash
PRCOPILOT_AI_PROVIDER=openai
OPENAI_API_KEY=sk-your-openai-key-here
```

See [Provider Setup Guides](#provider-setup-guides) for details per provider.

### Running the Application

Build and run:

```bash
mvn clean install
mvn spring-boot:run
```

Default base URL: `http://localhost:8080`

Verify health:

```bash
curl http://localhost:8080/actuator/health
```

## API Documentation

Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI spec:

```
http://localhost:8080/api-docs
```

### Analyze Diff Endpoint

- Method: `POST /api/v1/analyze-diff`
- Content Type: `application/json`

### Example Request

```bash
curl -X POST http://localhost:8080/api/v1/analyze-diff \
  -H "Content-Type: application/json" \
  -d '{
    "diff": "diff --git a/src/main/UserService.java b/src/main/UserService.java\nindex abc123..def456 100644\n--- a/src/main/UserService.java\n+++ b/src/main/UserService.java\n@@ -10,7 +10,7 @@ public class UserService {\n-    public User getUser(String id) {\n-        return database.findById(id);\n+    public User getUser(String id) throws UserNotFoundException {\n+        return database.findById(id).orElseThrow(() -> new UserNotFoundException(id));\n     }\n }",
    "language": "en",
    "style": "conventional-commits",
    "maxSummaryLength": 500,
    "requestId": "req-12345"
  }'
```

### Example Response

```json
{
	"title": "refactor: improve error handling in UserService",
	"summary": "Modified getUser method to throw UserNotFoundException when user is not found instead of returning null.",
	"details": "The getUser method signature now includes a throws clause for UserNotFoundException. The implementation uses Optional.orElseThrow() to raise an exception when the database query returns empty, replacing the previous behavior of returning null.",
	"risks": [
		"Breaking change: existing callers must now handle UserNotFoundException",
		"No null checks are present in the diff; ensure all callers are updated",
		"UserNotFoundException is not defined in this diff; verify it exists in the codebase"
	],
	"suggestedTests": [
		"Test that getUser throws UserNotFoundException when user does not exist",
		"Test that getUser returns valid User object when user exists",
		"Integration test to verify exception propagates correctly through call stack"
	],
	"touchedFiles": [
		"src/main/UserService.java"
	],
	"analysisNotes": "This is a common refactoring pattern to improve error handling. Ensure backward compatibility is considered if this is a public API.",
	"metadata": {
		"modelName": "gpt-4o",
		"provider": "openai",
		"modelLatencyMs": 1247,
		"tokensUsed": 312
	},
	"requestId": "req-12345",
	"rawModelOutput": null
}
```

**Request Parameters:**

| Field              | Type    | Required | Description                                     |
|--------------------|---------|----------|-------------------------------------------------|
| `diff`             | string  | Yes      | Git diff in unified format                      |
| `language`         | string  | No       | Analysis language (default `en`)                |
| `style`            | string  | No       | Analysis style (default `conventional-commits`) |
| `maxSummaryLength` | integer | No       | Maximum summary length                          |
| `requestId`        | string  | No       | Optional request identifier                     |

**Response Fields:**

| Field            | Type   | Description                              |
|------------------|--------|------------------------------------------|
| `title`          | string | Short technical title of the change      |
| `summary`        | string | Concise description of what changed      |
| `details`        | string | Detailed breakdown of the implementation |
| `risks`          | array  | Specific risks or issues                 |
| `suggestedTests` | array  | Recommended test cases                   |
| `touchedFiles`   | array  | Modified files                           |
| `analysisNotes`  | string | Additional notes or caveats              |
| `metadata`       | object | AI call metadata                         |
| `requestId`      | string | Echo of the request ID                   |
| `rawModelOutput` | string | Raw model output if enabled              |

## Configuration Reference

Configuration can be set via environment variables or `application.yml`. See `.env.example` for the full list.

### AI Provider Configuration

```bash
PRCOPILOT_AI_PROVIDER=openai
AI_TEMPERATURE=0.1
AI_MAX_TOKENS=1024
AI_TIMEOUT_MILLIS=30000
```

### Analysis Settings

```bash
PRCOPILOT_ANALYSIS_MAX_DIFF_CHARS=50000
PRCOPILOT_ANALYSIS_DEFAULT_LANGUAGE=en
PRCOPILOT_ANALYSIS_DEFAULT_STYLE=conventional-commits
PRCOPILOT_ANALYSIS_INCLUDE_RAW_MODEL_OUTPUT=false
```

### Fallback Configuration

```bash
PRCOPILOT_AI_AUTO_FALLBACK=true
PRCOPILOT_AI_FALLBACK_PROVIDER=anthropic
OPENAI_API_KEY=sk-your-openai-key
ANTHROPIC_API_KEY=sk-ant-your-anthropic-key
```

## Provider Setup Guides

### OpenAI

```bash
PRCOPILOT_AI_PROVIDER=openai
OPENAI_API_KEY=sk-your-openai-key-here
OPENAI_MODEL=gpt-4o
```

### Anthropic Claude

```bash
PRCOPILOT_AI_PROVIDER=anthropic
ANTHROPIC_API_KEY=sk-ant-your-anthropic-key-here
ANTHROPIC_MODEL=claude-sonnet-4-0
```

### Google Gemini

```bash
PRCOPILOT_AI_PROVIDER=gemini
GEMINI_PROJECT_ID=your-gcp-project-id
GEMINI_LOCATION=us-central1
GEMINI_MODEL=gemini-2.0-flash
```

Follow Google Cloud Vertex AI documentation for authentication and project setup.

### Ollama

```bash
docker run -d -p 11434:11434 ollama/ollama
ollama pull qwen3:4b
PRCOPILOT_AI_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen3:4b
```

## Error Handling

Structured errors via `GlobalExceptionHandler`:

- 400 for validation errors or unreadable body
- 404 for unknown endpoint
- 405 for unsupported method
- 413 for oversized diff
- 422 for invalid model output
- 500 for unexpected errors
- 502 or 504 for upstream access or timeout

Example:

```json
{
	"timestamp": "...",
	"statusCode": 400,
	"error": "Bad Request",
	"message": "{diff=Diff must not be blank}",
	"path": "/api/v1/analyze-diff",
	"requestId": null
}
```

## Health Checks

The service exposes Spring Boot Actuator endpoints for monitoring:

**Health Check:**

```bash
curl http://localhost:8080/actuator/health
```

**Application Info:**

```bash
curl http://localhost:8080/actuator/info
```

## CI

`ci.yml` runs on pushes and pull requests. Steps: checkout, set up Temurin Java 25, cache the Maven repository, run
`mvn -B verify`.

## Testing

Unit and integration tests are included. Run:

```bash
mvn test
```

## Architecture Overview

- Controller: `DiffAnalysisController`
- Services: `DiffAnalysisService`, `AiChatService`, `PromptBuilderService`, `DiffResponseMapperService`
- Configuration and validation: `MultiAiConfigurationProperties`, `PrCopilotAnalysisProperties`,
  `PrCopilotLoggingProperties`, startup checks in `AppStartupCheck`
- Error handling: `GlobalExceptionHandler`
- Uses Spring AI to switch between providers

## License

This project is under active development. License information will be added in a future release.

---
Project Status: Active Development. For questions or issues, please open an issue on the repository.