package com.terrarent.controller;

import com.terrarent.dto.ai.AiAssistantRequest;
import com.terrarent.service.AiAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Assistant", description = "Secure proxy routes for AI assistant requests")
@SecurityRequirement(name = "bearerAuth")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @Operation(summary = "Secure AI assistant proxy",
            description = "Forwards frontend AI requests to the configured Gemini/OpenAI endpoint without exposing the API key.")
    @PostMapping("/assistant")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> proxyAssistant(@Valid @RequestBody AiAssistantRequest request) {
        String response = aiAssistantService.proxyAssistantRequest(request);
        return ResponseEntity.ok(response);
    }
}
