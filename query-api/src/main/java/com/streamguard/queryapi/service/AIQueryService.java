package com.streamguard.queryapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamguard.queryapi.model.SecurityEvent;
import com.streamguard.queryapi.model.ThreatAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

/**
 * AI-powered natural language query service.
 *
 * Translates natural language questions into API calls using OpenAI GPT-4o-mini,
 * executes the queries, and provides AI-generated summaries of the results.
 *
 * @author Jose Ortuno
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIQueryService {

    private final QueryService queryService;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String model;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    /**
     * Process a natural language query about security events.
     *
     * @param query Natural language question
     * @return Map containing results and AI summary
     */
    public Map<String, Object> processQuery(String query) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OpenAI API key not configured");
        }

        log.info("[AIQuery] Processing query: {}", query);

        // Step 1: Use AI to determine query intent and parameters
        QueryIntent intent = determineIntent(query);
        log.info("[AIQuery] Determined intent: {}", intent.getAction());

        // Step 2: Execute the appropriate query based on intent
        Object results = executeQuery(intent);
        log.info("[AIQuery] Query executed successfully");

        // Step 3: Generate AI summary of results
        String summary = generateSummary(query, intent, results);
        log.info("[AIQuery] Generated AI summary");

        // Step 4: Build response
        Map<String, Object> response = new HashMap<>();
        response.put("query", query);
        response.put("intent", intent.getAction());
        response.put("results", results);
        response.put("summary", summary);
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * Determine the user's intent from natural language query.
     */
    private QueryIntent determineIntent(String query) {
        String systemPrompt = """
            You are a security query analyzer. Analyze the user's question and determine what they want to query.

            Available query types:
            - recent_events: Get recent security events
            - high_threats: Get high-threat events
            - failed_logins: Get failed authentication attempts
            - suspicious_ips: Get events from suspicious IP addresses
            - file_access: Get file access events
            - stats: Get statistical summary
            - analysis_by_severity: Get AI analyses by severity level

            Respond ONLY with valid JSON in this exact format:
            {
              "action": "<query_type>",
              "parameters": {
                "limit": <number>,
                "min_score": <0.0-1.0>,
                "severity": "<LOW|MEDIUM|HIGH|CRITICAL>",
                "event_type": "<auth_attempt|file_access|network_connection|process_execution|data_exfiltration>"
              }
            }

            Only include parameters that are relevant to the query.
            """;

        String response = callOpenAI(systemPrompt, query);

        try {
            // Extract JSON from response (might be wrapped in markdown)
            String jsonStr = extractJSON(response);
            JsonNode json = objectMapper.readTree(jsonStr);

            QueryIntent intent = new QueryIntent();
            intent.setAction(json.get("action").asText());

            if (json.has("parameters")) {
                JsonNode params = json.get("parameters");
                Map<String, Object> parameters = new HashMap<>();

                if (params.has("limit")) parameters.put("limit", params.get("limit").asInt());
                if (params.has("min_score")) parameters.put("min_score", params.get("min_score").asDouble());
                if (params.has("severity")) parameters.put("severity", params.get("severity").asText());
                if (params.has("event_type")) parameters.put("event_type", params.get("event_type").asText());

                intent.setParameters(parameters);
            }

            return intent;
        } catch (Exception e) {
            log.error("[AIQuery] Failed to parse intent: {}", e.getMessage());
            // Fallback to recent events
            QueryIntent fallback = new QueryIntent();
            fallback.setAction("recent_events");
            fallback.setParameters(Map.of("limit", 10));
            return fallback;
        }
    }

    /**
     * Execute the query based on determined intent.
     */
    private Object executeQuery(QueryIntent intent) {
        int limit = (int) intent.getParameters().getOrDefault("limit", 10);

        return switch (intent.getAction()) {
            case "recent_events" -> queryService.getLatestEvents(limit);

            case "high_threats" -> {
                double minScore = (double) intent.getParameters().getOrDefault("min_score", 0.7);
                yield queryService.getEventsByThreatScore(minScore, limit);
            }

            case "failed_logins" -> {
                // Filter for auth_attempt events with high threat scores
                yield queryService.getEventsByThreatScore(0.5, limit).stream()
                    .filter(e -> e.getEventType() != null &&
                                 e.getEventType().toLowerCase().contains("auth"))
                    .toList();
            }

            case "stats" -> queryService.getStatsSummary();

            case "analysis_by_severity" -> {
                String severity = (String) intent.getParameters().getOrDefault("severity", "HIGH");
                yield queryService.getAnalysesBySeverity(severity, limit);
            }

            default -> queryService.getLatestEvents(limit);
        };
    }

    /**
     * Generate AI summary of query results.
     */
    private String generateSummary(String originalQuery, QueryIntent intent, Object results) {
        String resultsJson;
        try {
            resultsJson = objectMapper.writeValueAsString(results);
            // Limit size for API call
            if (resultsJson.length() > 4000) {
                resultsJson = resultsJson.substring(0, 4000) + "... (truncated)";
            }
        } catch (Exception e) {
            resultsJson = results.toString();
        }

        String systemPrompt = """
            You are a security analyst assistant. Provide a concise, actionable summary of the query results.
            Focus on:
            - Key findings and patterns
            - Notable security concerns
            - Recommended actions (if applicable)

            Keep your response to 2-3 sentences maximum.
            """;

        String userPrompt = String.format(
            "User asked: \"%s\"\n\nQuery results:\n%s\n\nProvide a brief, actionable summary.",
            originalQuery,
            resultsJson
        );

        return callOpenAI(systemPrompt, userPrompt);
    }

    /**
     * Call OpenAI API with system and user prompts.
     */
    private String callOpenAI(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ));
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 500);

            WebClient webClient = webClientBuilder
                .baseUrl(OPENAI_API_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

            String response = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(TIMEOUT)
                .block();

            JsonNode jsonResponse = objectMapper.readTree(response);
            return jsonResponse.get("choices").get(0).get("message").get("content").asText();

        } catch (Exception e) {
            log.error("[AIQuery] OpenAI API call failed: {}", e.getMessage());
            return "Unable to generate AI summary: " + e.getMessage();
        }
    }

    /**
     * Extract JSON from potentially markdown-wrapped response.
     */
    private String extractJSON(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end >= 0) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    /**
     * Query intent determined by AI.
     */
    @lombok.Data
    public static class QueryIntent {
        private String action;
        private Map<String, Object> parameters = new HashMap<>();
    }
}
