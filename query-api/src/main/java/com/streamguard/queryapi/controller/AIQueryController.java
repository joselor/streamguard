package com.streamguard.queryapi.controller;

import com.streamguard.queryapi.service.AIQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for AI-powered natural language queries.
 *
 * Allows security analysts to query the system using natural language instead of
 * writing complex API calls or database queries.
 *
 * @author Jose Ortuno
 * @version 1.0
 */
@RestController
@RequestMapping("/api/threats")
@RequiredArgsConstructor
@Tag(name = "AI Query", description = "Natural language query interface")
public class AIQueryController {

    private final AIQueryService aiQueryService;

    @PostMapping("/ask")
    @Operation(
        summary = "Ask a question in natural language",
        description = """
            Query security events using natural language. The AI will:
            1. Understand your question
            2. Translate it to appropriate API calls
            3. Execute the queries
            4. Provide a summary of findings

            Example questions:
            - "Show me the most recent security events"
            - "What are the highest threat events?"
            - "Show failed login attempts"
            - "Give me statistics about security events"
            - "Show critical severity threats"
            """
    )
    public ResponseEntity<Map<String, Object>> askQuestion(
            @Parameter(description = "Natural language question about security events")
            @RequestBody QueryRequest request) {

        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Question cannot be empty"));
        }

        try {
            Map<String, Object> response = aiQueryService.processQuery(request.getQuestion());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to process query: " + e.getMessage()));
        }
    }

    /**
     * Request body for natural language query.
     */
    @Data
    public static class QueryRequest {
        @Parameter(description = "Natural language question", example = "Show me the most recent security events")
        private String question;
    }
}
