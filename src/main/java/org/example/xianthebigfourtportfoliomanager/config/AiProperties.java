package org.example.xianthebigfourtportfoliomanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

// 读取 application.properties 中 app.ai.* 配置。
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
    // 是否启用 AI 功能。
    private boolean enabled;
    // 当前项目的 AI 提供商固定为 zhipu，这里只作为后端校验与展示用途。
    private String provider;
    // 智谱开放平台基础地址。
    private String baseUrl;
    // Chat Completions 路径，默认 /chat/completions。
    private String chatPath;
    // API Key 从环境变量读取，只在后端调用智谱接口时使用，前端永远拿不到这个值。
    private String apiKey;
    // models 是后端允许前端选择的智谱模型白名单，Spring Boot 会把逗号分隔配置自动转成 List。
    private List<String> models = new ArrayList<>();
    // defaultModel 表示页面首次打开时默认选中的模型。
    private String defaultModel;
    private Double temperature;
    private Integer maxTokens;
    private Integer connectTimeout;
    private Integer readTimeout;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getChatPath() {
        return chatPath;
    }

    public void setChatPath(String chatPath) {
        this.chatPath = chatPath;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public List<String> getModels() {
        return models;
    }

    public void setModels(List<String> models) {
        this.models = models;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }
}

