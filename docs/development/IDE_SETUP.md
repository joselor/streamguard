# IntelliJ IDEA Setup for StreamGuard

This guide helps you run, debug, test, and profile StreamGuard from IntelliJ IDEA. All essential Run/Debug configurations are shared under the project’s .run/ folder and will appear automatically in the Run/Debug configurations menu when you open the project with IntelliJ IDEA (2022.3+).

If you work on the C++ stream-processor, CLion is recommended for a first-class CMake experience. IntelliJ IDEA can still be used for the Java services and for Docker orchestration.

---

## Prerequisites

- IntelliJ IDEA (Ultimate recommended for Docker & profiler integrations)
- JDK 17 installed (matching Maven settings)
- Maven (3.9+) installed or use IntelliJ’s bundled Maven
- Docker Desktop (for running full stack via docker-compose)
- For C++ stream-processor (macOS ARM64 example):
  - Homebrew packages: librdkafka, rocksdb, prometheus-cpp, curl, nlohmann-json, cmake, pkg-config
  - Example install (macOS/Apple Silicon):
    - brew install librdkafka rocksdb prometheus-cpp curl nlohmann-json cmake pkg-config

---

## Importing the Project

1. Open IntelliJ IDEA.
2. File > Open… and select the repository root folder.
3. IntelliJ will detect Maven modules (query-api, event-generator). Confirm when prompted.
4. Configure SDK:
   - File > Project Structure > Project SDK: set to JDK 17
   - Project language level: 17
5. Wait for Maven indexing to finish.

Optional: For C++ stream-processor, open stream-processor as a separate CMake project in CLion for best results, or open it in IDEA with the CMake plugin.

---

## Shared Run/Debug Configurations

These are preconfigured under .run/ and should appear automatically:

- Query API (Spring Boot)
  - Runs com.streamguard.queryapi.QueryApiApplication with -Dspring.profiles.active=dev
  - Working directory: query-api

- Query API (JFR Profiling)
  - Same as above but starts Java Flight Recorder and stores JFR at out/jfr/query-api.jfr

- Event Generator
  - Runs com.streamguard.EventGenerator
  - Default args: --rate 1000 --duration 60 --broker localhost:9092 --topic security-events
  - Adjust program parameters per your test needs

- Maven Test (query-api)
  - Executes mvn test in query-api

- Maven Test (event-generator)
  - Executes mvn test in event-generator

- Docker Compose Up
  - Brings up the full stack using docker-compose.yml in detached mode
  - Requires the Docker plugin in IntelliJ and Docker Desktop running

- Remote Debug (5005)
  - Attach to a JVM on localhost:5005 (useful when launching services with JDWP enabled)

If you don’t see these, enable “Store as project file” when creating any new run configuration and ensure your IDE is 2022.3+.

---

## Running the System

Common flows:

- Full stack (Docker):
  - Run “Docker Compose Up” to start dependent services (Kafka, Prometheus, Grafana, etc.).
  - Then run “Query API (Spring Boot)” to start the API.
  - Optionally run “Event Generator” to push synthetic events into Kafka.

- Local only (Java):
  - Run “Query API (Spring Boot)” directly.
  - Event generator can be run separately and pointed to your Kafka.

---

## Debugging

- Java service (Query API):
  - Use the “Query API (Spring Boot)” config to run in debug mode (the IDE’s Debug button will attach).

- Remote debug any JVM:
  - Add this VM option to the JVM you want to debug:
    - -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
  - Then use the shared “Remote Debug (5005)” configuration to attach.

---

## Profiling (JFR)

- Use the “Query API (JFR Profiling)” run configuration to start Flight Recorder automatically.
- The resulting file will be at out/jfr/query-api.jfr (created on first run).
- Open the .jfr file in IntelliJ IDEA’s profiler tool (or Java Mission Control) for analysis.

Notes:
- Ensure you’re using a JDK build with JFR (most modern JDK 11+ do).

---

## Testing

- Java modules:
  - Use the “Maven Test (query-api)” and “Maven Test (event-generator)” run configurations.
  - Alternatively, right-click any test class/method and choose “Run Tests”.

- C++ (stream-processor):
  - In CLion or terminal:
    - mkdir -p build && cd build
    - cmake ..
    - cmake --build . -j
    - ctest
  - Note: GoogleTest is optional; if not found, C++ tests are skipped (see stream-processor/CMakeLists.txt).

---

## Monitoring and Dashboards

- Docker Compose brings up Prometheus and Grafana.
- Grafana dashboards are provided in monitoring/grafana/dashboards.
- Default Grafana URL: http://localhost:3000/ (credentials depend on your local setup).

---

## Useful Paths and Docs

- API docs: docs/final/api
- Diagrams: docs/final/diagrams (Component, Class, Data Flow)
- Project Handoffs: docs/PROJECT_HANDOFF_SPRINT2.md, docs/PROJECT_HANDOFF_SPRINT3.md
- Final README: docs/final/README.md

---

## Troubleshooting

- Docker not starting:
  - Ensure Docker Desktop is running.
  - If the Docker run configuration is missing, install/enable the Docker plugin in IntelliJ.

- JDK mismatch:
  - Verify Project SDK is JDK 17 and Maven respects it (Maven settings in IntelliJ or set JAVA_HOME).

- C++ dependencies missing:
  - Install Homebrew packages listed above; ensure /opt/homebrew is in your environment (Apple Silicon).

- Kafka connectivity for Event Generator:
  - Check the broker address (default localhost:9092) or use Docker compose services.

If anything is unclear or you want additional run configurations (e.g., per-profile, different ports, compound runs), let me know and I’ll add them.