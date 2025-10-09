package com.streamguard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamguard.model.Event;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main class for the StreamGuard Event Generator.
 *
 * <p>This application generates realistic security events and produces them to Kafka
 * for consumption by the StreamGuard stream processor. It supports:
 * <ul>
 *   <li>Configurable event generation rate (events per second)</li>
 *   <li>All 5 event types with realistic data distributions</li>
 *   <li>Graceful shutdown handling (SIGINT, SIGTERM)</li>
 *   <li>Production metrics logging</li>
 *   <li>Kafka producer error handling</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>
 * java -jar event-generator.jar --rate 1000 --broker localhost:9092
 * </pre>
 *
 * <p><b>Command-line Arguments:</b>
 * <ul>
 *   <li><b>--rate</b>: Events per second (default: 100, max: 50000)</li>
 *   <li><b>--broker</b>: Kafka bootstrap servers (default: localhost:9092)</li>
 *   <li><b>--topic</b>: Kafka topic name (default: security-events)</li>
 *   <li><b>--duration</b>: Run duration in seconds (default: unlimited)</li>
 * </ul>
 *
 * <p><b>Kafka Configuration:</b>
 * The producer is configured for high throughput with reasonable durability:
 * <ul>
 *   <li>acks=1 (leader acknowledgment for balance of speed and durability)</li>
 *   <li>Compression enabled (gzip) to reduce network usage</li>
 *   <li>Batching enabled for throughput</li>
 *   <li>Idempotence enabled to prevent duplicates</li>
 * </ul>
 *
 * @author Jose Ortuno
 * @version 1.0
 */
public class EventGenerator {

    private static final Logger logger = LoggerFactory.getLogger(EventGenerator.class);

    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String DEFAULT_TOPIC = "security-events";
    private static final int DEFAULT_RATE = 100;
    private static final int MAX_RATE = 50000;

    private final KafkaProducer<String, String> producer;
    private final EventFactory eventFactory;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final int eventsPerSecond;
    private final long durationSeconds;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong eventsSent = new AtomicLong(0);
    private final AtomicLong errorsCount = new AtomicLong(0);

    /**
     * Creates a new EventGenerator with the specified configuration.
     *
     * @param bootstrapServers Kafka bootstrap servers
     * @param topic Kafka topic to produce to
     * @param eventsPerSecond Target event generation rate
     * @param durationSeconds Duration to run (0 for unlimited)
     */
    public EventGenerator(String bootstrapServers, String topic, int eventsPerSecond, long durationSeconds) {
        this.topic = topic;
        this.eventsPerSecond = Math.min(eventsPerSecond, MAX_RATE);
        this.durationSeconds = durationSeconds;
        this.eventFactory = new EventFactory();
        this.objectMapper = new ObjectMapper();
        this.producer = createKafkaProducer(bootstrapServers);

        setupShutdownHook();
    }

    /**
     * Creates and configures the Kafka producer.
     */
    private KafkaProducer<String, String> createKafkaProducer(String bootstrapServers) {
        Properties props = new Properties();

        // Basic Kafka configuration
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Performance and reliability configuration
        props.put(ProducerConfig.ACKS_CONFIG, "all");  // All replicas acknowledgment (required for idempotence)
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");  // Compress messages
        props.put(ProducerConfig.LINGER_MS_CONFIG, "10");  // Small batching delay
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, "32768");  // 32KB batch size
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, "67108864");  // 64MB buffer
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");  // Prevent duplicates

        // Retry configuration
        props.put(ProducerConfig.RETRIES_CONFIG, "3");
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, "100");

        // Client ID for identification
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "streamguard-event-generator");

        logger.info("Kafka producer configured with bootstrap servers: {}", bootstrapServers);

        return new KafkaProducer<>(props);
    }

    /**
     * Sets up graceful shutdown handling for SIGINT and SIGTERM.
     */
    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received, stopping event generation...");
            running.set(false);
        }));
    }

    /**
     * Starts generating events at the configured rate.
     */
    public void start() {
        logger.info("=== StreamGuard Event Generator Starting ===");
        logger.info("Target rate: {} events/second", eventsPerSecond);
        logger.info("Kafka topic: {}", topic);
        logger.info("Duration: {} seconds (0 = unlimited)", durationSeconds);
        logger.info("===========================================");

        long startTime = System.currentTimeMillis();
        long endTime = durationSeconds > 0 ? startTime + (durationSeconds * 1000) : Long.MAX_VALUE;

        // Calculate sleep interval between events (in nanoseconds)
        long nanosPerEvent = 1_000_000_000L / eventsPerSecond;
        long nextEventTime = System.nanoTime();

        long lastLogTime = System.currentTimeMillis();
        long lastEventCount = 0;

        try {
            while (running.get() && System.currentTimeMillis() < endTime) {
                // Generate and send event
                Event event = eventFactory.generateEvent();
                sendEvent(event);

                // Rate limiting using precise timing
                nextEventTime += nanosPerEvent;
                long sleepTime = nextEventTime - System.nanoTime();

                if (sleepTime > 0) {
                    // Sleep for remaining time
                    long millis = sleepTime / 1_000_000;
                    int nanos = (int) (sleepTime % 1_000_000);

                    if (millis > 0 || nanos > 0) {
                        Thread.sleep(millis, nanos);
                    }
                } else {
                    // We're falling behind, reset timing
                    nextEventTime = System.nanoTime();
                }

                // Log metrics every 10 seconds
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastLogTime >= 10000) {
                    long currentCount = eventsSent.get();
                    long eventsInPeriod = currentCount - lastEventCount;
                    double actualRate = eventsInPeriod / 10.0;

                    logger.info("Events sent: {} | Rate: {:.1f} events/sec | Errors: {}",
                               currentCount, actualRate, errorsCount.get());

                    lastLogTime = currentTime;
                    lastEventCount = currentCount;
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Event generation interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            shutdown();
        }
    }

    /**
     * Sends an event to Kafka.
     */
    private void sendEvent(Event event) {
        try {
            // Serialize event to JSON
            String eventJson = objectMapper.writeValueAsString(event);

            // Create Kafka record (key = event_id for partitioning)
            ProducerRecord<String, String> record = new ProducerRecord<>(
                topic,
                event.getEventId(),
                eventJson
            );

            // Send asynchronously with callback
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Failed to send event {}: {}", event.getEventId(), exception.getMessage());
                    errorsCount.incrementAndGet();
                } else {
                    eventsSent.incrementAndGet();
                }
            });

        } catch (Exception e) {
            logger.error("Error serializing event: {}", e.getMessage());
            errorsCount.incrementAndGet();
        }
    }

    /**
     * Gracefully shuts down the event generator.
     */
    private void shutdown() {
        logger.info("Shutting down event generator...");

        // Flush any pending messages
        producer.flush();

        // Close producer
        producer.close();

        long totalEvents = eventsSent.get();
        long totalErrors = errorsCount.get();

        logger.info("=== Event Generator Stopped ===");
        logger.info("Total events sent: {}", totalEvents);
        logger.info("Total errors: {}", totalErrors);
        logger.info("Success rate: {:.2f}%",
                   totalEvents > 0 ? (100.0 * totalEvents / (totalEvents + totalErrors)) : 0.0);
        logger.info("==============================");
    }

    /**
     * Parses command-line arguments.
     */
    private static Config parseArgs(String[] args) {
        Config config = new Config();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--rate":
                    if (i + 1 < args.length) {
                        config.rate = Integer.parseInt(args[++i]);
                    }
                    break;

                case "--broker":
                    if (i + 1 < args.length) {
                        config.bootstrapServers = args[++i];
                    }
                    break;

                case "--topic":
                    if (i + 1 < args.length) {
                        config.topic = args[++i];
                    }
                    break;

                case "--duration":
                    if (i + 1 < args.length) {
                        config.durationSeconds = Long.parseLong(args[++i]);
                    }
                    break;

                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                    break;

                default:
                    logger.warn("Unknown argument: {}", args[i]);
            }
        }

        return config;
    }

    /**
     * Prints usage information.
     */
    private static void printUsage() {
        System.out.println("StreamGuard Event Generator");
        System.out.println();
        System.out.println("Usage: java -jar event-generator.jar [OPTIONS]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --rate <num>       Events per second (default: 100, max: 50000)");
        System.out.println("  --broker <addr>    Kafka bootstrap servers (default: localhost:9092)");
        System.out.println("  --topic <name>     Kafka topic (default: security-events)");
        System.out.println("  --duration <sec>   Run duration in seconds (default: unlimited)");
        System.out.println("  --help, -h         Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Generate 1000 events/sec for 60 seconds");
        System.out.println("  java -jar event-generator.jar --rate 1000 --duration 60");
        System.out.println();
        System.out.println("  # Generate events to remote Kafka cluster");
        System.out.println("  java -jar event-generator.jar --broker kafka1:9092,kafka2:9092,kafka3:9092");
    }

    /**
     * Configuration holder.
     */
    private static class Config {
        String bootstrapServers = DEFAULT_BOOTSTRAP_SERVERS;
        String topic = DEFAULT_TOPIC;
        int rate = DEFAULT_RATE;
        long durationSeconds = 0;  // 0 = unlimited
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        // Parse command-line arguments
        Config config = parseArgs(args);

        // Validate rate
        if (config.rate <= 0) {
            logger.error("Invalid rate: {}. Must be positive.", config.rate);
            System.exit(1);
        }

        if (config.rate > MAX_RATE) {
            logger.warn("Rate {} exceeds maximum {}. Capping to maximum.", config.rate, MAX_RATE);
            config.rate = MAX_RATE;
        }

        // Create and start generator
        EventGenerator generator = new EventGenerator(
            config.bootstrapServers,
            config.topic,
            config.rate,
            config.durationSeconds
        );

        generator.start();
    }
}
