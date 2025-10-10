#ifndef STREAMGUARD_AI_ANALYZER_H
#define STREAMGUARD_AI_ANALYZER_H

#include <string>
#include <optional>
#include <memory>
#include "event.h"

namespace streamguard {

/**
 * AI-powered threat analysis result
 */
struct ThreatAnalysis {
    std::string event_id;
    std::string severity;           // LOW, MEDIUM, HIGH, CRITICAL
    std::string attack_type;        // Brute Force, DDoS, Malware, etc.
    std::string description;        // Natural language explanation
    std::vector<std::string> recommendations;  // Actions to take
    double confidence;              // 0.0 to 1.0
    int64_t timestamp;              // Unix timestamp

    // Convert to JSON string for storage
    std::string toJson() const;

    // Parse from JSON string
    static std::optional<ThreatAnalysis> fromJson(const std::string& json);
};

/**
 * AI Analyzer for threat detection using OpenAI GPT-4o-mini
 *
 * Generates natural language threat assessments for high-risk security events.
 *
 * Features:
 * - OpenAI GPT-4o-mini integration for cost-effective analysis
 * - Context-aware analysis (considers event details)
 * - MITRE ATT&CK framework alignment
 * - Actionable recommendations
 * - Error handling with graceful degradation
 *
 * @author Jose Ortuno
 * @version 1.0
 */
class AIAnalyzer {
public:
    /**
     * Constructor
     * @param api_key OpenAI API key
     * @param model Model to use (default: gpt-4o-mini)
     */
    explicit AIAnalyzer(const std::string& api_key,
                       const std::string& model = "gpt-4o-mini");

    /**
     * Destructor
     */
    ~AIAnalyzer() = default;

    // Disable copy and move
    AIAnalyzer(const AIAnalyzer&) = delete;
    AIAnalyzer& operator=(const AIAnalyzer&) = delete;
    AIAnalyzer(AIAnalyzer&&) = delete;
    AIAnalyzer& operator=(AIAnalyzer&&) = delete;

    /**
     * Analyze a security event for threats
     *
     * @param event The security event to analyze
     * @return Threat analysis result, or std::nullopt if analysis fails
     */
    std::optional<ThreatAnalysis> analyze(const Event& event);

    /**
     * Check if analyzer is enabled and ready
     */
    bool isEnabled() const { return enabled_; }

    /**
     * Get total successful analyses
     */
    uint64_t getSuccessCount() const { return success_count_; }

    /**
     * Get total failed analyses
     */
    uint64_t getErrorCount() const { return error_count_; }

private:
    std::string api_key_;
    std::string model_;
    std::string api_endpoint_;
    bool enabled_;

    // Statistics
    uint64_t success_count_;
    uint64_t error_count_;

    /**
     * Build the prompt for the LLM
     */
    std::string buildPrompt(const Event& event) const;

    /**
     * Make HTTP request to OpenAI API
     */
    std::optional<std::string> makeAPIRequest(const std::string& prompt);

    /**
     * Parse OpenAI API response
     */
    std::optional<ThreatAnalysis> parseResponse(const std::string& response,
                                                const Event& event);

    /**
     * Map threat score to severity level
     */
    std::string getSeverityLevel(double threat_score) const;
};

} // namespace streamguard

#endif // STREAMGUARD_AI_ANALYZER_H
