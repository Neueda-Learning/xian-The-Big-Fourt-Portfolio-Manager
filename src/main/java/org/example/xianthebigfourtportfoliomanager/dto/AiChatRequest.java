package org.example.xianthebigfourtportfoliomanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiChatRequest {

    // model 来自前端下拉框；即使前端已限制，后端仍会再次校验是否为空、是否在允许列表中。
    @NotBlank(message = "model 不能为空")
    private String model;

    // message 来自用户输入框；后端仍要校验，避免绕过前端直接提交空内容或超长内容。
    @NotBlank(message = "message 不能为空")
    @Size(max = 2000, message = "message 长度不能超过 2000 个字符")
    private String message;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

