package com.terrarent.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for the secure AI assistant proxy")
public class AiAssistantRequest {

    @NotNull(message = "Payload is required")
    @Schema(description = "Raw AI request payload to forward to the secure backend proxy")
    private Map<String, Object> payload;

    @Schema(description = "Optional model name override for the AI provider", example = "gemini-1.0")
    private String model;

    @Schema(description = "Optional AI provider selection", example = "gemini", allowableValues = {"gemini", "openai"})
    private String apiSelection;
}
