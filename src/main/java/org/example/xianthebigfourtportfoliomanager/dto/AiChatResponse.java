package org.example.xianthebigfourtportfoliomanager.dto;
// Simple response DTO returned to the frontend with the AI text answer.
public class AiChatResponse {
    private String answer;
    private String provider;
    private String model;
    public AiChatResponse() {}
    public AiChatResponse(String answer, String provider, String model) {
        this.answer = answer;
        this.provider = provider;
        this.model = model;
    }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}