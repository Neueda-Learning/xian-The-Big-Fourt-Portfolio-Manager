package org.example.xianthebigfourtportfoliomanager.dto;

// 返回给前端的 AI 文本结果，保持结构简单。
public class AiChatResponse {
    private String answer;
    private String provider;
    private String model;

    public AiChatResponse() {
    }

    public AiChatResponse(String answer, String provider, String model) {
        this.answer = answer;
        this.provider = provider;
        this.model = model;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}

