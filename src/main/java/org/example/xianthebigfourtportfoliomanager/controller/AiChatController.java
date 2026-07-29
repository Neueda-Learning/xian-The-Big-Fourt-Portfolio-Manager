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
// @RestController marks this class as a REST controller; return values are automatically serialised to JSON.
@RestController
// @RequestMapping sets the base path for all endpoints in this controller.
@RequestMapping("/api/ai")
public class AiChatController {
    private final AiChatService aiChatService;
    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
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
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return ResponseEntity.ok(aiChatService.chat(request.getModel(), request.getMessage()));
    }
}