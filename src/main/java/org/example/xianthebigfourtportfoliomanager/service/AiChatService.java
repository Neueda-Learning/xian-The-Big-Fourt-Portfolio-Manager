package org.example.xianthebigfourtportfoliomanager.service;

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
 * AI 问答核心服务。
 * 作用：接收前端问题 -> 调用外部 AI 接口 -> 提取文本回答 -> 返回给前端。
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    // 系统提示词用于限制模型行为：只允许知识问答，不允许执行业务操作。
    private static final String SYSTEM_PROMPT = "你是投资组合管理系统中的金融知识问答助手。你只能提供教育性和信息性的文字回答，不构成投资建议。你不能替用户执行交易，不能修改投资组合、持仓或数据库，也不能声称已经完成任何操作。";

    private static final String DISCLAIMER = "AI回答仅用于教育和信息参考，不构成任何投资建议。";

    private final RestTemplate aiRestTemplate;
    private final AiProperties aiProperties;

    public AiChatService(@Qualifier("aiRestTemplate") RestTemplate aiRestTemplate, AiProperties aiProperties) {
        this.aiRestTemplate = aiRestTemplate;
        this.aiProperties = aiProperties;
    }

    /**
     * 对外统一入口：Controller 只调用这个方法，不直接访问外部 AI。
     */
    public AiChatResponse chat(String model, String message) {
        if (!aiProperties.isEnabled()) {
            return new AiChatResponse("AI功能未开启，请联系管理员。", normalizedProvider(), resolveDefaultModel());
        }

        String configError = validateConfiguration();
        if (configError != null) {
            return new AiChatResponse(configError, normalizedProvider(), resolveDefaultModel());
        }

        // 前端提交的 model 不能直接信任；后端必须做 trim 和白名单校验，防止用户绕过页面自行构造请求。
        String selectedModel = sanitizeModel(model);
        if (selectedModel == null) {
            return new AiChatResponse("请选择有效的智谱模型。", normalizedProvider(), resolveDefaultModel());
        }
        // 同一个智谱 API Key 可以访问账号中已开通的多个模型，但后端仍要限制为配置中允许的列表。
        if (!getAllowedModels().contains(selectedModel)) {
            return new AiChatResponse("不支持该智谱模型：" + selectedModel, normalizedProvider(), resolveDefaultModel());
        }

        // 再次在服务层兜底，避免前端绕过校验直接请求后端。
        String sanitizedMessage = sanitizeMessage(message);
        if (sanitizedMessage == null) {
            return new AiChatResponse("问题不能为空且不能超过2000个字符。", normalizedProvider(), selectedModel);
        }

        String endpoint = buildEndpoint(aiProperties.getBaseUrl(), aiProperties.getChatPath());

        // 使用 Map 直接组装 OpenAI 兼容请求体，减少额外 DTO 文件。
        // 这里会把前端选中的模型原样放入请求体 model 字段，从而切换不同的智谱大模型。
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
        // Bearer Token 来自环境变量，不写死在代码中。
        headers.setBearerAuth(aiProperties.getApiKey().trim());

        try {
            // 通过 RestTemplate 调用外部 AI Chat Completions 接口。
            ResponseEntity<Map> response = aiRestTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            // 智谱返回 OpenAI 兼容结构，这里只解析 choices[0].message.content 作为最终答案。
            String answer = extractContent(response.getBody());
            if (!StringUtils.hasText(answer)) {
                return new AiChatResponse("AI暂时没有返回有效回答", normalizedProvider(), selectedModel);
            }

            return new AiChatResponse(
                    appendDisclaimer(answer.trim()),
                    normalizedProvider(),
                    selectedModel
            );
        } catch (HttpStatusCodeException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            if (status == HttpStatus.TOO_MANY_REQUESTS) {
                return new AiChatResponse("AI服务请求过于频繁，请稍后再试", normalizedProvider(), selectedModel);
            }
            if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
                return new AiChatResponse("AI认证失败，请检查API配置", normalizedProvider(), selectedModel);
            }
            if (status != null && status.is5xxServerError()) {
                return new AiChatResponse("AI服务暂时不可用，请稍后重试", normalizedProvider(), selectedModel);
            }
            return new AiChatResponse("AI服务请求失败，请稍后重试", normalizedProvider(), selectedModel);
        } catch (ResourceAccessException ex) {
            // 超时一般会落在 ResourceAccessException 中，这里给前端返回友好文本而不是底层异常。
            if (isTimeout(ex)) {
                return new AiChatResponse("AI服务请求超时，请稍后重试", normalizedProvider(), selectedModel);
            }
            log.warn("AI network access failed for provider={}", normalizedProvider());
            return new AiChatResponse("AI服务暂时不可用，请稍后重试", normalizedProvider(), selectedModel);
        } catch (RestClientException ex) {
            log.warn("AI client error for provider={}", normalizedProvider());
            return new AiChatResponse("AI服务暂时不可用，请稍后重试", normalizedProvider(), selectedModel);
        }
    }

    // GET /api/ai/models 会调用这里，把后端配置中的安全模型选项返回给前端。
    public AiModelOptionsResponse getModelOptions() {
        List<String> models = getAllowedModels();
        return new AiModelOptionsResponse(normalizedProvider(), models, resolveDefaultModel());
    }

    // 配置项来自 application.properties + 环境变量；当前实现只允许 provider 固定为 zhipu。
    private String validateConfiguration() {
        String provider = normalizedProvider();
        if (!"zhipu".equals(provider)) {
            return "AI provider 配置无效，当前仅支持 zhipu";
        }
        if (!StringUtils.hasText(aiProperties.getBaseUrl())) {
            return "AI base URL 未配置";
        }
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            return "AI API key 未配置";
        }
        if (getAllowedModels().isEmpty()) {
            return "AI models 未配置";
        }
        return null;
    }

    // 把配置中的模型列表做 trim、去空值、去重，作为后端允许列表。
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
        // 如果 defaultModel 配错了，就回退到第一个可用模型，避免页面初始化或聊天请求直接崩溃。
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

