package com.streamguard.queryapi.controller;

import com.streamguard.queryapi.model.SecurityEvent;
import com.streamguard.queryapi.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Security Event queries
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Security event query endpoints")
public class EventController {

    private final QueryService queryService;

    @GetMapping
    @Operation(summary = "Get latest security events", description = "Returns the most recent security events up to the specified limit")
    public ResponseEntity<List<SecurityEvent>> getLatestEvents(
            @Parameter(description = "Maximum number of events to return", example = "100")
            @RequestParam(defaultValue = "100") int limit) {
        List<SecurityEvent> events = queryService.getLatestEvents(limit);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get event by ID", description = "Returns a specific security event by its ID")
    public ResponseEntity<SecurityEvent> getEventById(
            @Parameter(description = "Event ID", example = "evt_1696723200_001")
            @PathVariable String eventId) {
        SecurityEvent event = queryService.getEventById(eventId);
        if (event != null) {
            return ResponseEntity.ok(event);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/count")
    @Operation(summary = "Get total event count", description = "Returns the total number of security events stored")
    public ResponseEntity<Long> getEventCount() {
        long count = queryService.getEventCount();
        return ResponseEntity.ok(count);
    }
}
