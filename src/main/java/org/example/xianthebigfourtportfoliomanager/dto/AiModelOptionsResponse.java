package org.example.xianthebigfourtportfoliomanager.dto;

import java.util.List;

// 返回给前端的安全模型选项：只包含提供商、允许选择的模型列表和默认模型，不包含任何敏感配置。
public class AiModelOptionsResponse {
    private String provider;
    private List<String> models;
    private String defaultModel;

    public AiModelOptionsResponse() {
    }

    public AiModelOptionsResponse(String provider, List<String> models, String defaultModel) {
        this.provider = provider;
        this.models = models;
        this.defaultModel = defaultModel;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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
}

