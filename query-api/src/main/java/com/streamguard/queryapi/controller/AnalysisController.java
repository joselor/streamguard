package com.streamguard.queryapi.controller;

import com.streamguard.queryapi.model.ThreatAnalysis;
import com.streamguard.queryapi.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for AI Threat Analysis queries
 */
@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
@Tag(name = "Threat Analyses", description = "AI threat analysis query endpoints")
public class AnalysisController {

    private final QueryService queryService;

    @GetMapping
    @Operation(summary = "Get latest threat analyses", description = "Returns the most recent AI threat analyses up to the specified limit")
    public ResponseEntity<List<ThreatAnalysis>> getLatestAnalyses(
            @Parameter(description = "Maximum number of analyses to return", example = "100")
            @RequestParam(defaultValue = "100") int limit) {
        List<ThreatAnalysis> analyses = queryService.getLatestAnalyses(limit);
        return ResponseEntity.ok(analyses);
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "Get analysis by event ID", description = "Returns the AI threat analysis for a specific event")
    public ResponseEntity<ThreatAnalysis> getAnalysisByEventId(
            @Parameter(description = "Event ID", example = "evt_1696723200_001")
            @PathVariable String eventId) {
        ThreatAnalysis analysis = queryService.getAnalysisByEventId(eventId);
        if (analysis != null) {
            return ResponseEntity.ok(analysis);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/severity/{severity}")
    @Operation(summary = "Get analyses by severity", description = "Returns AI threat analyses filtered by severity level")
    public ResponseEntity<List<ThreatAnalysis>> getAnalysesBySeverity(
            @Parameter(description = "Severity level (LOW, MEDIUM, HIGH, CRITICAL)", example = "HIGH")
            @PathVariable String severity,
            @Parameter(description = "Maximum number of analyses to return", example = "100")
            @RequestParam(defaultValue = "100") int limit) {
        List<ThreatAnalysis> analyses = queryService.getAnalysesBySeverity(severity, limit);
        return ResponseEntity.ok(analyses);
    }

    @GetMapping("/count")
    @Operation(summary = "Get total analysis count", description = "Returns the total number of AI threat analyses stored")
    public ResponseEntity<Long> getAnalysisCount() {
        long count = queryService.getAnalysisCount();
        return ResponseEntity.ok(count);
    }
}
