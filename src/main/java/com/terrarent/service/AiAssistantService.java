package com.terrarent.service;

import com.terrarent.dto.ai.AiAssistantRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiAssistantService {

    @Value("${ai.provider:gemini}")
    private String defaultProvider;

    @Value("${gemini.api.base-url:https://api.openai.com/v1/chat/completions}")
    private String geminiBaseUrl;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.model:gemini-1.0}")
    private String geminiDefaultModel;

    @Value("${openai.api.base-url:https://api.openai.com/v1/chat/completions}")
    private String openAiBaseUrl;

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String openAiDefaultModel;

    private final RestTemplate restTemplate = new RestTemplate();

    public String proxyAssistantRequest(AiAssistantRequest request) {
        String provider = resolveProvider(request.getApiSelection());
        String apiKey = resolveApiKey(provider);
        String requestUrl = resolveBaseUrl(provider);
        String model = resolveModel(request.getModel(), provider);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>(request.getPayload());
        if (!body.containsKey("model")) {
            body.put("model", model);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.POST, entity, String.class);
        return response.getBody();
    }

    private String resolveProvider(String apiSelection) {
        if (apiSelection == null || apiSelection.isBlank()) {
            return defaultProvider.toLowerCase(Locale.ROOT).trim();
        }
        return apiSelection.toLowerCase(Locale.ROOT).trim();
    }

    private String resolveBaseUrl(String provider) {
        return switch (provider) {
            case "openai" -> openAiBaseUrl;
            case "gemini" -> geminiBaseUrl;
            default -> throw new IllegalArgumentException("Unsupported AI provider: " + provider);
        };
    }

    private String resolveApiKey(String provider) {
        String key = switch (provider) {
            case "openai" -> openAiApiKey;
            case "gemini" -> geminiApiKey;
            default -> null;
        };
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("API key is not configured for provider: " + provider);
        }
        return key;
    }

    private String resolveModel(String requestedModel, String provider) {
        if (requestedModel != null && !requestedModel.isBlank()) {
            return requestedModel;
        }
        return switch (provider) {
            case "openai" -> openAiDefaultModel;
            case "gemini" -> geminiDefaultModel;
            default -> throw new IllegalArgumentException("Unsupported AI provider: " + provider);
        };
    }
}
