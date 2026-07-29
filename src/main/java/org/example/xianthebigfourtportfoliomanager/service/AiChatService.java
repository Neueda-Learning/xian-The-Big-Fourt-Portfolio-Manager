package org.example.xianthebigfourtportfoliomanager.service;
import jakarta.servlet.http.HttpSession;
import org.example.xianthebigfourtportfoliomanager.config.AiProperties;
import org.example.xianthebigfourtportfoliomanager.dto.AiChatResponse;
import org.example.xianthebigfourtportfoliomanager.dto.AiModelOptionsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * Core AI Q&A service.
 * Receives a question from the frontend -> calls the external AI API -> extracts the text answer -> returns it to the frontend.
 */
@Service
public class AiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final String KEY_SOURCE_USER = "user";
    private static final String KEY_SOURCE_SYSTEM = "system";
    private static final String KEY_SOURCE_NONE = "none";
    // System prompt restricts model behaviour: knowledge Q&A only; no business operations are permitted.
    private static final String SYSTEM_PROMPT = "You are a financial knowledge Q&A assistant in a portfolio management system. You may only provide educational and informational text responses, which do not constitute investment advice. You must not execute trades on behalf of users, modify portfolios, holdings, or databases, or claim to have completed any operations.";
    private static final String DISCLAIMER = "AI responses are for educational and informational purposes only and do not constitute investment advice.";
    private final RestTemplate aiRestTemplate;
    private final AiProperties aiProperties;
    private final UserAiKeyService userAiKeyService;
    public AiChatService(@Qualifier("aiRestTemplate") RestTemplate aiRestTemplate, AiProperties aiProperties, UserAiKeyService userAiKeyService) {
        this.aiRestTemplate = aiRestTemplate;
        this.aiProperties = aiProperties;
        this.userAiKeyService = userAiKeyService;
    }
    /**
     * Unified public entry point: the Controller calls only this method and never accesses the external AI directly.
     */
    public AiChatResponse chat(String model, String message, HttpSession session) {
        if (!aiProperties.isEnabled()) {
            return new AiChatResponse("AI feature is not enabled. Please contact the administrator.", normalizedProvider(), resolveDefaultModel());
        }
        String configError = validateConfiguration();
        if (configError != null) {
            return new AiChatResponse(configError, normalizedProvider(), resolveDefaultModel());
        }
        // The model submitted by the frontend cannot be trusted directly; trim and validate against the allow-list.
        String selectedModel = sanitizeModel(model);
        if (selectedModel == null) {
            return new AiChatResponse("Please select a valid Zhipu model.", normalizedProvider(), resolveDefaultModel());
        }
        // A single Zhipu API key can access multiple enabled models, but the backend restricts to the configured allow-list.
        if (!getAllowedModels().contains(selectedModel)) {
            return new AiChatResponse("Unsupported Zhipu model: " + selectedModel, normalizedProvider(), resolveDefaultModel());
        }
        // Second-layer guard in the service to catch any requests that bypass frontend validation.
        String sanitizedMessage = sanitizeMessage(message);
        if (sanitizedMessage == null) {
            return new AiChatResponse("Question must not be empty and must not exceed 2000 characters.", normalizedProvider(), selectedModel);
        }
        ActiveApiKey activeApiKey = resolveActiveApiKey(session);
        if (!activeApiKey.configured()) {
            return new AiChatResponse("No AI API key is configured. Please add your API key in Settings.", normalizedProvider(), selectedModel);
        }
        String endpoint = buildEndpoint(aiProperties.getBaseUrl(), aiProperties.getChatPath());
        // Build an OpenAI-compatible request body using a Map to avoid extra DTO files.
        // The model selected by the frontend is placed directly into the "model" field to switch Zhipu models.
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", selectedModel);
        requestBody.put("temperature", aiProperties.getTemperature());
        requestBody.put("max_tokens", aiProperties.getMaxTokens());
        requestBody.put("stream", false);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", sanitizedMessage)
        );
        requestBody.put("messages", messages);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 中文注释：优先使用当前会话中的个人 Key，若不存在再回退到系统默认 Key。
        headers.setBearerAuth(activeApiKey.value());
        try {
            // Call the external AI Chat Completions endpoint via RestTemplate.
            ResponseEntity<Map> response = aiRestTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );
            // Zhipu returns an OpenAI-compatible structure; only choices[0].message.content is needed.
            String answer = extractContent(response.getBody());
            if (!StringUtils.hasText(answer)) {
                return new AiChatResponse("AI did not return a valid response.", normalizedProvider(), selectedModel);
            }
            return new AiChatResponse(
                    appendDisclaimer(answer.trim()),
                    normalizedProvider(),
                    selectedModel
            );
        } catch (HttpStatusCodeException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            if (status == HttpStatus.TOO_MANY_REQUESTS) {
                return new AiChatResponse(apiKeyUnavailableMessage(activeApiKey.source()), normalizedProvider(), selectedModel);
            }
            if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
                return new AiChatResponse(apiKeyUnavailableMessage(activeApiKey.source()), normalizedProvider(), selectedModel);
            }
            if (status != null && status.is5xxServerError()) {
                return new AiChatResponse(apiKeyUnavailableMessage(activeApiKey.source()), normalizedProvider(), selectedModel);
            }
            return new AiChatResponse(apiKeyUnavailableMessage(activeApiKey.source()), normalizedProvider(), selectedModel);
        } catch (ResourceAccessException ex) {
            // Timeouts typically surface as a ResourceAccessException; return a friendly message instead of the raw exception.
            if (isTimeout(ex)) {
                return new AiChatResponse(apiKeyUnavailableMessage(activeApiKey.source()), normalizedProvider(), selectedModel);
            }
            log.warn("AI network access failed for provider={}", normalizedProvider());
            return new AiChatResponse(apiKeyUnavailableMessage(activeApiKey.source()), normalizedProvider(), selectedModel);
        } catch (RestClientException ex) {
            log.warn("AI client error for provider={}", normalizedProvider());
            return new AiChatResponse(apiKeyUnavailableMessage(activeApiKey.source()), normalizedProvider(), selectedModel);
        }
    }
    // Called by GET /api/ai/models to return the safe set of model options to the frontend.
    public AiModelOptionsResponse getModelOptions() {
        List<String> models = getAllowedModels();
        return new AiModelOptionsResponse(normalizedProvider(), models, resolveDefaultModel());
    }
    // Configuration comes from application.properties + environment variables; the provider is fixed as "zhipu".
    private String validateConfiguration() {
        String provider = normalizedProvider();
        if (!"zhipu".equals(provider)) {
            return "Invalid AI provider configuration. Only 'zhipu' is supported.";
        }
        if (!StringUtils.hasText(aiProperties.getBaseUrl())) {
            return "AI base URL is not configured.";
        }
        if (getAllowedModels().isEmpty()) {
            return "AI models are not configured.";
        }
        return null;
    }

    public ApiKeyStatus readApiKeyStatus(HttpSession session) {
        String userApiKey = userAiKeyService.getUserApiKey(session);
        String systemApiKey = aiProperties.getApiKey();
        boolean userKeyConfigured = StringUtils.hasText(userApiKey);
        boolean systemKeyConfigured = StringUtils.hasText(systemApiKey);
        String source = userKeyConfigured ? KEY_SOURCE_USER : (systemKeyConfigured ? KEY_SOURCE_SYSTEM : KEY_SOURCE_NONE);
        return new ApiKeyStatus(userKeyConfigured, systemKeyConfigured, source);
    }

    private ActiveApiKey resolveActiveApiKey(HttpSession session) {
        String userApiKey = userAiKeyService.getUserApiKey(session);
        String systemApiKey = aiProperties.getApiKey();

        if (StringUtils.hasText(userApiKey)) {
            return new ActiveApiKey(userApiKey.trim(), KEY_SOURCE_USER);
        }
        if (StringUtils.hasText(systemApiKey)) {
            return new ActiveApiKey(systemApiKey.trim(), KEY_SOURCE_SYSTEM);
        }
        return new ActiveApiKey("", KEY_SOURCE_NONE);
    }

    private String apiKeyUnavailableMessage(String source) {
        if (KEY_SOURCE_USER.equals(source)) {
            return "Your AI API key is invalid or unavailable. Please check it in Settings.";
        }
        return "The system AI API key is unavailable. Please add your own API key in Settings.";
    }

    public static class ApiKeyStatus {
        private final boolean userKeyConfigured;
        private final boolean systemKeyConfigured;
        private final String activeKeySource;

        public ApiKeyStatus(boolean userKeyConfigured, boolean systemKeyConfigured, String activeKeySource) {
            this.userKeyConfigured = userKeyConfigured;
            this.systemKeyConfigured = systemKeyConfigured;
            this.activeKeySource = activeKeySource;
        }

        public boolean isUserKeyConfigured() {
            return userKeyConfigured;
        }

        public boolean isSystemKeyConfigured() {
            return systemKeyConfigured;
        }

        public String getActiveKeySource() {
            return activeKeySource;
        }
    }

    private static class ActiveApiKey {
        private final String value;
        private final String source;

        private ActiveApiKey(String value, String source) {
            this.value = value;
            this.source = source;
        }

        private String value() {
            return value;
        }

        private String source() {
            return source;
        }

        private boolean configured() {
            return StringUtils.hasText(value);
        }
    }
    // Trims, removes blanks, and de-duplicates the configured model list to produce the backend allow-list.
    private List<String> getAllowedModels() {
        List<String> models = aiProperties.getModels();
        if (models == null || models.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> normalizedModels = models.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        return Collections.unmodifiableList(normalizedModels);
    }
    private String sanitizeModel(String model) {
        if (!StringUtils.hasText(model)) {
            return null;
        }
        return model.trim();
    }
    private String sanitizeMessage(String message) {
        if (message == null) return null;
        String trimmed = message.trim();
        if (trimmed.isEmpty() || trimmed.length() > 2000) return null;
        return trimmed;
    }
    private String normalizedProvider() {
        String provider = aiProperties.getProvider();
        if (provider == null) {
            return "";
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }
    private String resolveDefaultModel() {
        String configuredDefault = sanitizeModel(aiProperties.getDefaultModel());
        List<String> allowedModels = getAllowedModels();
        if (configuredDefault != null && allowedModels.contains(configuredDefault)) {
            return configuredDefault;
        }
        // If defaultModel is misconfigured, fall back to the first available model to prevent crashes.
        return allowedModels.isEmpty() ? "" : allowedModels.get(0);
    }
    private String buildEndpoint(String baseUrl, String chatPath) {
        String normalizedBase = baseUrl.trim();
        String normalizedPath = StringUtils.hasText(chatPath) ? chatPath.trim() : "/chat/completions";
        boolean baseEndsWithSlash = normalizedBase.endsWith("/");
        boolean pathStartsWithSlash = normalizedPath.startsWith("/");
        if (baseEndsWithSlash && pathStartsWithSlash) {
            return normalizedBase + normalizedPath.substring(1);
        }
        if (!baseEndsWithSlash && !pathStartsWithSlash) {
            return normalizedBase + "/" + normalizedPath;
        }
        return normalizedBase + normalizedPath;
    }
    @SuppressWarnings("unchecked")
    private String extractContent(Map body) {
        if (body == null) {
            return null;
        }
        Object choicesObj = body.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }
        Object firstObj = choices.get(0);
        if (!(firstObj instanceof Map<?, ?> firstChoice)) {
            return null;
        }
        Object messageObj = firstChoice.get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap)) {
            return null;
        }
        Object contentObj = messageMap.get("content");
        String content = contentObj instanceof String ? (String) contentObj : null;
        if (!StringUtils.hasText(content)) {
            return null;
        }
        return content;
    }
    private String appendDisclaimer(String answer) {
        if (answer.contains(DISCLAIMER)) {
            return answer;
        }
        return answer + "\n\n" + DISCLAIMER;
    }
    private boolean isTimeout(ResourceAccessException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof SocketTimeoutException) {
            return true;
        }
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("timed out") || normalized.contains("timeout");
    }
}