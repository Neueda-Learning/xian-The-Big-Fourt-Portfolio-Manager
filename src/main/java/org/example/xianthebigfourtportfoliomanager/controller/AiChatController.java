package org.example.xianthebigfourtportfoliomanager.controller;

import jakarta.validation.Valid;
import org.example.xianthebigfourtportfoliomanager.dto.AiChatRequest;
import org.example.xianthebigfourtportfoliomanager.dto.AiChatResponse;
import org.example.xianthebigfourtportfoliomanager.dto.AiModelOptionsResponse;
import org.example.xianthebigfourtportfoliomanager.service.AiChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @RestController 表示该类提供 REST 接口，返回值会自动转成 JSON。
@RestController
// @RequestMapping 统一定义当前控制器的基础路径。
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    // GET /api/ai/models 只返回可公开的智谱模型选项，供前端下拉框加载。
    // 这里不会返回 API Key，也不会让浏览器直接访问智谱接口。
    @GetMapping("/models")
    public ResponseEntity<AiModelOptionsResponse> models() {
        return ResponseEntity.ok(aiChatService.getModelOptions());
    }

    // @PostMapping 表示处理 POST 请求；
    // @RequestBody 把请求 JSON 绑定到 Java 对象；
    // @Valid 触发参数校验。
    // POST /api/ai/chat 接收前端提交的“所选模型 + 用户问题”，真正调用智谱的逻辑仍放在 Service 中。
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return ResponseEntity.ok(aiChatService.chat(request.getModel(), request.getMessage()));
    }
}

