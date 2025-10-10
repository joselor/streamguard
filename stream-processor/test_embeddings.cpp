#include <iostream>
#include <cstdlib>
#include "event_store.h"
#include "ai_analyzer.h"
#include "event.h"

/**
 * Simple test program for embedding generation and similarity search
 *
 * This demonstrates the embedding functionality for US-211:
 * 1. Generate embeddings for test events
 * 2. Store embeddings in RocksDB
 * 3. Test similarity search
 */

int main() {
    std::cout << "=== StreamGuard Embedding Test ===" << std::endl;
    std::cout << std::endl;

    // Get OpenAI API key from environment
    const char* api_key_env = std::getenv("OPENAI_API_KEY");
    if (!api_key_env) {
        std::cerr << "Error: OPENAI_API_KEY environment variable not set" << std::endl;
        std::cerr << "Usage: export OPENAI_API_KEY=your-key-here && ./test_embeddings" << std::endl;
        return 1;
    }
    std::string api_key(api_key_env);

    try {
        // Create EventStore
        std::cout << "[Test] Creating EventStore..." << std::endl;
        streamguard::EventStore store("./test-embeddings.db");

        // Create AIAnalyzer
        std::cout << "[Test] Creating AIAnalyzer..." << std::endl;
        streamguard::AIAnalyzer analyzer(api_key);

        if (!analyzer.isEnabled()) {
            std::cerr << "Error: AIAnalyzer not enabled" << std::endl;
            return 1;
        }

        // Create test events
        std::cout << std::endl;
        std::cout << "[Test] Creating test events..." << std::endl;

        streamguard::Event event1;
        event1.event_id = "evt_test_001";
        event1.event_type = streamguard::EventType::AUTH_ATTEMPT;
        event1.timestamp = 1234567890000;
        event1.user = "admin";
        event1.source_ip = "192.168.1.100";
        event1.threat_score = 0.95;
        event1.metadata.domain = "malicious.com";

        streamguard::Event event2;
        event2.event_id = "evt_test_002";
        event2.event_type = streamguard::EventType::AUTH_ATTEMPT;
        event2.timestamp = 1234567891000;
        event2.user = "root";
        event2.source_ip = "192.168.1.101";
        event2.threat_score = 0.92;
        event2.metadata.domain = "suspicious.com";

        streamguard::Event event3;
        event3.event_id = "evt_test_003";
        event3.event_type = streamguard::EventType::FILE_ACCESS;
        event3.timestamp = 1234567892000;
        event3.user = "user1";
        event3.source_ip = "10.0.0.5";
        event3.threat_score = 0.3;
        event3.metadata.file_path = "/etc/passwd";

        // Store events
        std::cout << "[Test] Storing events in database..." << std::endl;
        store.put(event1);
        store.put(event2);
        store.put(event3);

        // Generate and store embeddings
        std::cout << std::endl;
        std::cout << "[Test] Generating embeddings..." << std::endl;
        std::cout << "This will make 3 API calls to OpenAI..." << std::endl;

        auto emb1 = analyzer.generateEmbedding(event1);
        if (emb1) {
            std::cout << "  Event 1: " << emb1->size() << " dimensions" << std::endl;
            store.putEmbedding(event1.event_id, *emb1);
        }

        auto emb2 = analyzer.generateEmbedding(event2);
        if (emb2) {
            std::cout << "  Event 2: " << emb2->size() << " dimensions" << std::endl;
            store.putEmbedding(event2.event_id, *emb2);
        }

        auto emb3 = analyzer.generateEmbedding(event3);
        if (emb3) {
            std::cout << "  Event 3: " << emb3->size() << " dimensions" << std::endl;
            store.putEmbedding(event3.event_id, *emb3);
        }

        // Test embedding retrieval
        std::cout << std::endl;
        std::cout << "[Test] Testing embedding retrieval..." << std::endl;
        auto retrieved = store.getEmbedding("evt_test_001");
        if (retrieved) {
            std::cout << "  ✓ Successfully retrieved embedding for evt_test_001" << std::endl;
            std::cout << "    Dimensions: " << retrieved->size() << std::endl;
        } else {
            std::cout << "  ✗ Failed to retrieve embedding" << std::endl;
        }

        // Test similarity search
        std::cout << std::endl;
        std::cout << "[Test] Testing similarity search..." << std::endl;
        std::cout << "Query: Find events similar to event 1 (brute force auth attempt)" << std::endl;

        if (emb1) {
            auto similar = store.findSimilar(*emb1, 3);
            std::cout << "  Found " << similar.size() << " similar events:" << std::endl;
            for (const auto& [event_id, similarity] : similar) {
                std::cout << "    - " << event_id << " (similarity: " << similarity << ")" << std::endl;
            }
        }

        // Test cosine similarity calculation
        std::cout << std::endl;
        std::cout << "[Test] Testing cosine similarity..." << std::endl;
        if (emb1 && emb2 && emb3) {
            double sim12 = streamguard::AIAnalyzer::cosineSimilarity(*emb1, *emb2);
            double sim13 = streamguard::AIAnalyzer::cosineSimilarity(*emb1, *emb3);

            std::cout << "  Auth event 1 vs Auth event 2: " << sim12 << std::endl;
            std::cout << "  Auth event 1 vs File access event 3: " << sim13 << std::endl;
            std::cout << "  (Auth events should be more similar to each other)" << std::endl;

            if (sim12 > sim13) {
                std::cout << "  ✓ Similarity scores are as expected!" << std::endl;
            }
        }

        // Stats
        std::cout << std::endl;
        std::cout << "[Test] Database stats:" << std::endl;
        std::cout << "  Total embeddings: " << store.getEmbeddingCount() << std::endl;

        std::cout << std::endl;
        std::cout << "=== Test Complete ===" << std::endl;
        std::cout << "Database saved to: ./test-embeddings.db" << std::endl;

    } catch (const std::exception& e) {
        std::cerr << "Error: " << e.what() << std::endl;
        return 1;
    }

    return 0;
}
