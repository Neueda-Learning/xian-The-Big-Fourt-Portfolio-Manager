package org.example.xianthebigfourtportfoliomanager.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;
// Reads all app.ai.* entries from application.properties.
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
    // Whether the AI feature is enabled.
    private boolean enabled;
    // The AI provider for this project is fixed as "zhipu"; used for backend validation and display only.
    private String provider;
    // Base URL of the Zhipu open platform.
    private String baseUrl;
    // Chat Completions path; defaults to /chat/completions.
    private String chatPath;
    // API key is read from an environment variable and used only on the backend; the frontend never receives it.
    private String apiKey;
    // models is the backend allow-list of Zhipu models the frontend may select;
    // Spring Boot converts the comma-separated value into a List automatically.
    private List<String> models = new ArrayList<>();
    // defaultModel is the model pre-selected when the page first loads.
    private String defaultModel;
    private Double temperature;
    private Integer maxTokens;
    private Integer connectTimeout;
    private Integer readTimeout;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getChatPath() { return chatPath; }
    public void setChatPath(String chatPath) { this.chatPath = chatPath; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public List<String> getModels() { return models; }
    public void setModels(List<String> models) { this.models = models; }
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Integer getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Integer connectTimeout) { this.connectTimeout = connectTimeout; }
    public Integer getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Integer readTimeout) { this.readTimeout = readTimeout; }
}