package org.example.xianthebigfourtportfoliomanager.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public class AiChatRequest {
    // model comes from the frontend dropdown; the backend re-validates it against the allow-list.
    @NotBlank(message = "model must not be blank")
    private String model;
    // message comes from the user input field; the backend re-validates to prevent empty or oversized content.
    @NotBlank(message = "message must not be blank")
    @Size(max = 2000, message = "message must not exceed 2000 characters")
    private String message;
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}