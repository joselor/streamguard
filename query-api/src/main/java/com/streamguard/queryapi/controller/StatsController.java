package com.streamguard.queryapi.controller;

import com.streamguard.queryapi.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Statistics queries
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "Statistical summary endpoints")
public class StatsController {

    private final QueryService queryService;

    @GetMapping("/summary")
    @Operation(summary = "Get statistics summary", description = "Returns aggregated statistics including event counts, threat scores, and analysis counts")
    public ResponseEntity<QueryService.StatsSummary> getStatsSummary() {
        QueryService.StatsSummary summary = queryService.getStatsSummary();
        return ResponseEntity.ok(summary);
    }
}
