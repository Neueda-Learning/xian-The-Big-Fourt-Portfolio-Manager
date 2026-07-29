package org.example.xianthebigfourtportfoliomanager.controller;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.example.xianthebigfourtportfoliomanager.dto.AiChatRequest;
import org.example.xianthebigfourtportfoliomanager.dto.AiChatResponse;
import org.example.xianthebigfourtportfoliomanager.dto.AiModelOptionsResponse;
import org.example.xianthebigfourtportfoliomanager.service.AiChatService;
import org.example.xianthebigfourtportfoliomanager.service.UserAiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
// @RestController marks this class as a REST controller; return values are automatically serialised to JSON.
@RestController
// @RequestMapping sets the base path for all endpoints in this controller.
@RequestMapping("/api/ai")
public class AiChatController {
    private static final int MAX_API_KEY_LENGTH = 500;
    private final AiChatService aiChatService;
    private final UserAiKeyService userAiKeyService;
    public AiChatController(AiChatService aiChatService, UserAiKeyService userAiKeyService) {
        this.aiChatService = aiChatService;
        this.userAiKeyService = userAiKeyService;
    }
    // GET /api/ai/models returns only safe, publicly shareable Zhipu model options for the frontend dropdown.
    // No API key is returned here; the browser never calls the Zhipu API directly.
    @GetMapping("/models")
    public ResponseEntity<AiModelOptionsResponse> models() {
        return ResponseEntity.ok(aiChatService.getModelOptions());
    }
    // POST /api/ai/chat receives the selected model and the user question from the frontend.
    // @RequestBody binds the JSON body to a Java object; @Valid triggers constraint validation.
    // The actual call to Zhipu is delegated entirely to the Service layer.
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request, HttpSession session) {
        return ResponseEntity.ok(aiChatService.chat(request.getModel(), request.getMessage(), session));
    }

    @PostMapping("/settings/api-key")
    public ResponseEntity<Map<String, String>> saveApiKey(@RequestBody Map<String, String> payload, HttpSession session) {
        String apiKey = payload == null ? null : payload.get("apiKey");
        if (!StringUtils.hasText(apiKey)) {
            return ResponseEntity.badRequest().body(Map.of("message", "API key must not be empty."));
        }
        String trimmed = apiKey.trim();
        if (trimmed.length() > MAX_API_KEY_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("message", "API key must be at most 500 characters."));
        }

        // 只把用户密钥放进当前 HttpSession，不回写配置文件。
        userAiKeyService.saveUserApiKey(session, trimmed);
        return ResponseEntity.ok(Map.of("message", "API key saved."));
    }

    @DeleteMapping("/settings/api-key")
    public ResponseEntity<Map<String, String>> clearApiKey(HttpSession session) {
        userAiKeyService.clearUserApiKey(session);
        return ResponseEntity.ok(Map.of("message", "API key cleared."));
    }

    @GetMapping("/settings/api-key")
    public ResponseEntity<Map<String, Object>> apiKeyStatus(HttpSession session) {
        AiChatService.ApiKeyStatus status = aiChatService.readApiKeyStatus(session);
        return ResponseEntity.ok(Map.of(
                "userKeyConfigured", status.isUserKeyConfigured(),
                "systemKeyConfigured", status.isSystemKeyConfigured(),
                "activeKeySource", status.getActiveKeySource()
        ));
    }
}