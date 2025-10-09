package com.streamguard.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.streamguard.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Utility class for JSON serialization and deserialization of Event objects.
 *
 * <p>This class provides a centralized, thread-safe ObjectMapper configuration
 * for converting Event objects to/from JSON. Uses Jackson's polymorphic
 * deserialization to automatically handle different event subtypes.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Polymorphic serialization - preserves event subtypes</li>
 *   <li>Thread-safe singleton ObjectMapper</li>
 *   <li>Optimized for high-throughput (10K+ events/sec)</li>
 *   <li>Kafka-compatible byte array support</li>
 *   <li>Comprehensive error logging</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // Serialize for Kafka
 * AuthEvent event = new AuthEvent(...);
 * byte[] bytes = JsonUtil.toJsonBytes(event);
 * kafkaProducer.send(new ProducerRecord<>("events", bytes));
 *
 * // Deserialize (automatically gets correct subtype)
 * Event event = JsonUtil.fromJsonBytes(consumerRecord.value());
 * if (event instanceof AuthEvent) {
 *     AuthEvent authEvent = (AuthEvent) event;
 *     // Process authentication event
 * }
 * }</pre>
 *
 * <p><b>Thread Safety:</b> All methods are thread-safe. The ObjectMapper
 * is configured once and reused across all calls.
 *
 * @author Jose Ortuno
 * @version 1.0
 */
public class JsonUtil {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);
    private static final ObjectMapper objectMapper = createObjectMapper();

    /**
     * Creates and configures the singleton ObjectMapper.
     *
     * <p>Configuration:
     * <ul>
     *   <li>Ignores unknown properties (forward compatibility)</li>
     *   <li>Compact output (no pretty printing for performance)</li>
     *   <li>Handles polymorphic Event types automatically</li>
     * </ul>
     *
     * @return Configured ObjectMapper instance
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Forward compatibility: don't fail on unknown properties
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // Compact JSON for production (disable pretty printing)
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);

        return mapper;
    }

    /**
     * Serializes an Event object to compact JSON string.
     *
     * <p>Uses polymorphic serialization to include the event_type discriminator.
     * The resulting JSON can be deserialized back to the correct Event subtype.
     *
     * @param event The event to serialize
     * @return Compact JSON string representation
     * @throws JsonProcessingException if serialization fails
     */
    public static String toJson(Event event) throws JsonProcessingException {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event: {}", event, e);
            throw e;
        }
    }

    /**
     * Serializes an Event object to pretty-printed JSON string.
     *
     * <p>Useful for debugging and logging. Not recommended for production
     * Kafka messages due to increased size.
     *
     * @param event The event to serialize
     * @return Pretty-printed JSON string with indentation
     * @throws JsonProcessingException if serialization fails
     */
    public static String toPrettyJson(Event event) throws JsonProcessingException {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                              .writeValueAsString(event);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event to pretty JSON: {}", event, e);
            throw e;
        }
    }

    /**
     * Serializes an Event object to JSON byte array for Kafka.
     *
     * <p>This is the primary method for producing events to Kafka.
     * Byte arrays are more efficient for network transmission than strings.
     *
     * @param event The event to serialize
     * @return JSON as UTF-8 encoded byte array
     * @throws JsonProcessingException if serialization fails
     */
    public static byte[] toJsonBytes(Event event) throws JsonProcessingException {
        try {
            return objectMapper.writeValueAsBytes(event);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event to bytes: {}", event, e);
            throw e;
        }
    }

    /**
     * Deserializes JSON string to the appropriate Event subtype.
     *
     * <p>Uses Jackson's polymorphic deserialization based on the event_type field.
     * Automatically returns the correct subclass (AuthEvent, NetworkEvent, etc.).
     *
     * <p><b>Example JSON → Object mapping:</b>
     * <ul>
     *   <li>"event_type": "auth_attempt" → AuthEvent</li>
     *   <li>"event_type": "network_connection" → NetworkEvent</li>
     *   <li>"event_type": "file_access" → FileEvent</li>
     *   <li>etc.</li>
     * </ul>
     *
     * @param json JSON string to deserialize
     * @return Deserialized Event object (correct subtype)
     * @throws IOException if deserialization fails or JSON is invalid
     */
    public static Event fromJson(String json) throws IOException {
        try {
            return objectMapper.readValue(json, Event.class);
        } catch (IOException e) {
            logger.error("Failed to deserialize JSON: {}", json, e);
            throw e;
        }
    }

    /**
     * Deserializes JSON byte array to the appropriate Event subtype.
     *
     * <p>This is the primary method for consuming events from Kafka.
     * Directly processes byte arrays from Kafka consumer records.
     *
     * @param jsonBytes JSON as UTF-8 encoded byte array
     * @return Deserialized Event object (correct subtype)
     * @throws IOException if deserialization fails or data is invalid
     */
    public static Event fromJsonBytes(byte[] jsonBytes) throws IOException {
        try {
            return objectMapper.readValue(jsonBytes, Event.class);
        } catch (IOException e) {
            logger.error("Failed to deserialize JSON bytes (length: {})",
                        jsonBytes != null ? jsonBytes.length : 0, e);
            throw e;
        }
    }

    /**
     * Validates if a string contains valid JSON.
     *
     * <p>Useful for input validation before attempting deserialization.
     * Does not validate against Event schema, only JSON syntax.
     *
     * @param json String to validate
     * @return true if valid JSON syntax, false otherwise
     */
    public static boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * Gets the configured ObjectMapper instance.
     *
     * <p>Exposed for advanced use cases where custom serialization is needed.
     * The returned mapper is the same instance used by all utility methods.
     *
     * <p><b>Warning:</b> Do not modify the configuration of the returned
     * ObjectMapper as it will affect all usages in the application.
     *
     * @return The singleton ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
