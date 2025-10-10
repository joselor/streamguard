#include "ai_analyzer.h"
#include <curl/curl.h>
#include <nlohmann/json.hpp>
#include <sstream>
#include <iostream>
#include <chrono>
#include <ctime>

using json = nlohmann::json;

namespace streamguard {

// Callback for libcurl to write response data
static size_t WriteCallback(void* contents, size_t size, size_t nmemb, void* userp) {
    ((std::string*)userp)->append((char*)contents, size * nmemb);
    return size * nmemb;
}

// ThreatAnalysis implementation
std::string ThreatAnalysis::toJson() const {
    json j;
    j["event_id"] = event_id;
    j["severity"] = severity;
    j["attack_type"] = attack_type;
    j["description"] = description;
    j["recommendations"] = recommendations;
    j["confidence"] = confidence;
    j["timestamp"] = timestamp;
    return j.dump();
}

std::optional<ThreatAnalysis> ThreatAnalysis::fromJson(const std::string& json_str) {
    try {
        auto j = json::parse(json_str);
        ThreatAnalysis analysis;
        analysis.event_id = j["event_id"];
        analysis.severity = j["severity"];
        analysis.attack_type = j["attack_type"];
        analysis.description = j["description"];
        analysis.recommendations = j["recommendations"].get<std::vector<std::string>>();
        analysis.confidence = j["confidence"];
        analysis.timestamp = j["timestamp"];
        return analysis;
    } catch (const std::exception& e) {
        std::cerr << "[AIAnalyzer] Failed to parse ThreatAnalysis JSON: " << e.what() << std::endl;
        return std::nullopt;
    }
}

// AIAnalyzer implementation
AIAnalyzer::AIAnalyzer(const std::string& api_key, const std::string& model)
    : api_key_(api_key),
      model_(model),
      api_endpoint_("https://api.openai.com/v1/chat/completions"),
      enabled_(!api_key.empty()),
      success_count_(0),
      error_count_(0)
{
    if (enabled_) {
        std::cout << "[AIAnalyzer] Initialized with model: " << model_ << std::endl;
    } else {
        std::cout << "[AIAnalyzer] Disabled (no API key provided)" << std::endl;
    }
}

std::optional<ThreatAnalysis> AIAnalyzer::analyze(const Event& event) {
    if (!enabled_) {
        return std::nullopt;
    }

    // Build the prompt
    std::string prompt = buildPrompt(event);

    // Make API request
    auto response = makeAPIRequest(prompt);
    if (!response) {
        error_count_++;
        return std::nullopt;
    }

    // Parse response
    auto analysis = parseResponse(*response, event);
    if (analysis) {
        success_count_++;
    } else {
        error_count_++;
    }

    return analysis;
}

std::string AIAnalyzer::buildPrompt(const Event& event) const {
    std::stringstream ss;

    ss << "You are a cybersecurity analyst. Analyze this security event and provide a detailed threat assessment.\n\n";
    ss << "EVENT DETAILS:\n";
    ss << "- Event ID: " << event.event_id << "\n";
    ss << "- Type: " << eventTypeToString(event.event_type) << "\n";
    ss << "- User: " << event.user << "\n";
    ss << "- Source IP: " << event.source_ip << "\n";

    if (!event.destination_ip.empty()) {
        ss << "- Destination IP: " << event.destination_ip << "\n";
    }
    if (!event.metadata.file_path.empty()) {
        ss << "- File Path: " << event.metadata.file_path << "\n";
    }
    if (!event.metadata.domain.empty()) {
        ss << "- Domain: " << event.metadata.domain << "\n";
    }
    if (!event.metadata.process_name.empty()) {
        ss << "- Process: " << event.metadata.process_name << "\n";
    }

    ss << "- Threat Score: " << event.threat_score << " (0.0-1.0)\n";
    ss << "- Timestamp: " << event.timestamp << "\n\n";

    ss << "Provide your analysis in the following JSON format:\n";
    ss << "{\n";
    ss << "  \"attack_type\": \"<type of attack, e.g., Brute Force, DDoS, Malware, etc.>\",\n";
    ss << "  \"description\": \"<2-3 sentence natural language explanation of the threat>\",\n";
    ss << "  \"recommendations\": [\"<action 1>\", \"<action 2>\", \"<action 3>\"],\n";
    ss << "  \"confidence\": <0.0-1.0>\n";
    ss << "}\n\n";
    ss << "Consider MITRE ATT&CK tactics and techniques in your analysis. ";
    ss << "Be specific and actionable.";

    return ss.str();
}

std::optional<std::string> AIAnalyzer::makeAPIRequest(const std::string& prompt) {
    CURL* curl = curl_easy_init();
    if (!curl) {
        std::cerr << "[AIAnalyzer] Failed to initialize CURL" << std::endl;
        return std::nullopt;
    }

    std::string response_string;
    struct curl_slist* headers = nullptr;

    try {
        // Build request body
        json request_body;
        request_body["model"] = model_;
        request_body["messages"] = json::array({
            {
                {"role", "system"},
                {"content", "You are a cybersecurity threat analyst specializing in security event analysis."}
            },
            {
                {"role", "user"},
                {"content", prompt}
            }
        });
        request_body["temperature"] = 0.3;  // Lower temperature for more focused responses
        request_body["max_tokens"] = 500;

        std::string request_str = request_body.dump();

        // Set headers
        headers = curl_slist_append(headers, "Content-Type: application/json");
        std::string auth_header = "Authorization: Bearer " + api_key_;
        headers = curl_slist_append(headers, auth_header.c_str());

        // Configure CURL
        curl_easy_setopt(curl, CURLOPT_URL, api_endpoint_.c_str());
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, request_str.c_str());
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response_string);
        curl_easy_setopt(curl, CURLOPT_TIMEOUT, 30L);  // 30 second timeout

        // Perform request
        CURLcode res = curl_easy_perform(curl);

        if (res != CURLE_OK) {
            std::cerr << "[AIAnalyzer] CURL request failed: " << curl_easy_strerror(res) << std::endl;
            curl_slist_free_all(headers);
            curl_easy_cleanup(curl);
            return std::nullopt;
        }

        long http_code = 0;
        curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &http_code);

        if (http_code != 200) {
            std::cerr << "[AIAnalyzer] API request failed with HTTP " << http_code << std::endl;
            std::cerr << "[AIAnalyzer] Response: " << response_string << std::endl;
            curl_slist_free_all(headers);
            curl_easy_cleanup(curl);
            return std::nullopt;
        }

        curl_slist_free_all(headers);
        curl_easy_cleanup(curl);
        return response_string;

    } catch (const std::exception& e) {
        std::cerr << "[AIAnalyzer] Exception in makeAPIRequest: " << e.what() << std::endl;
        if (headers) curl_slist_free_all(headers);
        curl_easy_cleanup(curl);
        return std::nullopt;
    }
}

std::optional<ThreatAnalysis> AIAnalyzer::parseResponse(const std::string& response,
                                                       const Event& event) {
    try {
        auto response_json = json::parse(response);

        // Extract the content from OpenAI response
        if (!response_json.contains("choices") || response_json["choices"].empty()) {
            std::cerr << "[AIAnalyzer] Invalid API response: no choices" << std::endl;
            return std::nullopt;
        }

        std::string content = response_json["choices"][0]["message"]["content"];

        // Parse the JSON within the content
        // The LLM should return pure JSON, but might wrap it in markdown
        size_t json_start = content.find('{');
        size_t json_end = content.rfind('}');

        if (json_start == std::string::npos || json_end == std::string::npos) {
            std::cerr << "[AIAnalyzer] Could not find JSON in response" << std::endl;
            return std::nullopt;
        }

        std::string json_str = content.substr(json_start, json_end - json_start + 1);
        auto analysis_json = json::parse(json_str);

        // Build ThreatAnalysis object
        ThreatAnalysis analysis;
        analysis.event_id = event.event_id;
        analysis.severity = getSeverityLevel(event.threat_score);
        analysis.attack_type = analysis_json.value("attack_type", "Unknown");
        analysis.description = analysis_json.value("description", "No description provided");
        analysis.confidence = analysis_json.value("confidence", 0.5);

        // Extract recommendations
        if (analysis_json.contains("recommendations") && analysis_json["recommendations"].is_array()) {
            for (const auto& rec : analysis_json["recommendations"]) {
                if (rec.is_string()) {
                    analysis.recommendations.push_back(rec);
                }
            }
        }

        // Set timestamp
        auto now = std::chrono::system_clock::now();
        analysis.timestamp = std::chrono::duration_cast<std::chrono::seconds>(
            now.time_since_epoch()).count();

        return analysis;

    } catch (const std::exception& e) {
        std::cerr << "[AIAnalyzer] Failed to parse response: " << e.what() << std::endl;
        std::cerr << "[AIAnalyzer] Response was: " << response << std::endl;
        return std::nullopt;
    }
}

std::string AIAnalyzer::getSeverityLevel(double threat_score) const {
    if (threat_score >= 0.9) {
        return "CRITICAL";
    } else if (threat_score >= 0.8) {
        return "HIGH";
    } else if (threat_score >= 0.7) {
        return "MEDIUM";
    } else {
        return "LOW";
    }
}

std::optional<std::vector<float>> AIAnalyzer::generateEmbedding(const Event& event) {
    if (!enabled_) {
        return std::nullopt;
    }

    // Build text representation of the event for embedding
    std::stringstream ss;
    ss << "Security Event: " << eventTypeToString(event.event_type) << ". ";
    ss << "User: " << event.user << ". ";
    ss << "Source IP: " << event.source_ip << ". ";

    if (!event.destination_ip.empty()) {
        ss << "Destination IP: " << event.destination_ip << ". ";
    }
    if (!event.metadata.file_path.empty()) {
        ss << "File: " << event.metadata.file_path << ". ";
    }
    if (!event.metadata.domain.empty()) {
        ss << "Domain: " << event.metadata.domain << ". ";
    }
    if (!event.metadata.process_name.empty()) {
        ss << "Process: " << event.metadata.process_name << ". ";
    }

    ss << "Threat Score: " << event.threat_score;

    std::string text = ss.str();

    // Make API request to embeddings endpoint
    CURL* curl = curl_easy_init();
    if (!curl) {
        std::cerr << "[AIAnalyzer] Failed to initialize CURL for embedding" << std::endl;
        return std::nullopt;
    }

    std::string response_string;
    struct curl_slist* headers = nullptr;

    try {
        // Build request body for embeddings API
        json request_body;
        request_body["model"] = "text-embedding-3-small";
        request_body["input"] = text;

        std::string request_str = request_body.dump();

        // Set headers
        headers = curl_slist_append(headers, "Content-Type: application/json");
        std::string auth_header = "Authorization: Bearer " + api_key_;
        headers = curl_slist_append(headers, auth_header.c_str());

        // Configure CURL for embeddings endpoint
        curl_easy_setopt(curl, CURLOPT_URL, "https://api.openai.com/v1/embeddings");
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, request_str.c_str());
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response_string);
        curl_easy_setopt(curl, CURLOPT_TIMEOUT, 30L);

        // Perform request
        CURLcode res = curl_easy_perform(curl);

        if (res != CURLE_OK) {
            std::cerr << "[AIAnalyzer] Embedding CURL request failed: " << curl_easy_strerror(res) << std::endl;
            curl_slist_free_all(headers);
            curl_easy_cleanup(curl);
            return std::nullopt;
        }

        long http_code = 0;
        curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &http_code);

        if (http_code != 200) {
            std::cerr << "[AIAnalyzer] Embedding API failed with HTTP " << http_code << std::endl;
            curl_slist_free_all(headers);
            curl_easy_cleanup(curl);
            return std::nullopt;
        }

        curl_slist_free_all(headers);
        curl_easy_cleanup(curl);

        // Parse response and extract embedding vector
        auto response_json = json::parse(response_string);

        if (!response_json.contains("data") || response_json["data"].empty()) {
            std::cerr << "[AIAnalyzer] Invalid embedding response: no data" << std::endl;
            return std::nullopt;
        }

        auto embedding_json = response_json["data"][0]["embedding"];
        std::vector<float> embedding;
        embedding.reserve(embedding_json.size());

        for (const auto& val : embedding_json) {
            embedding.push_back(val.get<float>());
        }

        return embedding;

    } catch (const std::exception& e) {
        std::cerr << "[AIAnalyzer] Exception in generateEmbedding: " << e.what() << std::endl;
        if (headers) curl_slist_free_all(headers);
        curl_easy_cleanup(curl);
        return std::nullopt;
    }
}

double AIAnalyzer::cosineSimilarity(const std::vector<float>& vec1,
                                    const std::vector<float>& vec2) {
    if (vec1.size() != vec2.size() || vec1.empty()) {
        return 0.0;
    }

    double dot_product = 0.0;
    double norm1 = 0.0;
    double norm2 = 0.0;

    for (size_t i = 0; i < vec1.size(); ++i) {
        dot_product += vec1[i] * vec2[i];
        norm1 += vec1[i] * vec1[i];
        norm2 += vec2[i] * vec2[i];
    }

    if (norm1 == 0.0 || norm2 == 0.0) {
        return 0.0;
    }

    return dot_product / (std::sqrt(norm1) * std::sqrt(norm2));
}

} // namespace streamguard
