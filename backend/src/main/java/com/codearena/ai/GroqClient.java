package com.codearena.ai;

import com.codearena.dto.ChatMessage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin client for Groq's OpenAI-compatible chat completions API. The API key
 * is read from configuration (backed by the {@code GROQ_API_KEY} env var) and
 * never leaves the backend.
 */
@Component
public class GroqClient {

    private final RestClient http;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public GroqClient(
            @Value("${codearena.ai.provider:off}") String provider,
            @Value("${codearena.ai.groq.api-key:}") String apiKey,
            @Value("${codearena.ai.groq.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${codearena.ai.model:openai/gpt-oss-120b}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = "groq".equalsIgnoreCase(provider) && apiKey != null && !apiKey.isBlank();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(45));
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /** Whether a usable Groq configuration is present. */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sends a chat completion request and returns the assistant's text.
     * Throws on any transport/API error so callers can fall back gracefully.
     */
    public String chat(String systemPrompt, List<ChatMessage> conversation, double temperature, int maxTokens) {
        var messages = new java.util.ArrayList<Map<String, String>>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(conversation.stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .collect(Collectors.toList()));

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", temperature,
                "max_tokens", maxTokens);

        GroqResponse resp = http.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(GroqResponse.class);

        if (resp == null || resp.choices() == null || resp.choices().isEmpty()) {
            throw new IllegalStateException("Empty AI response");
        }
        return resp.choices().get(0).message().content();
    }

    // ----- Response mapping (OpenAI-compatible) -----
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GroqResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String role, String content) {
    }
}
