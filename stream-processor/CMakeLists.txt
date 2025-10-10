cmake_minimum_required(VERSION 3.20)
project(StreamProcessor VERSION 1.0)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# M1 Mac specific - ARM64 architecture
set(CMAKE_OSX_ARCHITECTURES "arm64")

# Homebrew paths for M1 Mac
set(CMAKE_PREFIX_PATH "/opt/homebrew")

# Include directories
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    /opt/homebrew/include
    ${RDKAFKA_INCLUDE_DIR}
)

# Link directories
link_directories(/opt/homebrew/lib)

# Find nlohmann_json (required for event.cpp)
find_package(nlohmann_json 3.2.0 REQUIRED)

# Find librdkafka (required for Kafka consumer)
find_library(RDKAFKA_LIB rdkafka++ REQUIRED)
find_path(RDKAFKA_INCLUDE_DIR librdkafka/rdkafkacpp.h REQUIRED)

# Find RocksDB (required for event storage)
find_package(RocksDB REQUIRED)

# Find prometheus-cpp (required for metrics)
find_package(prometheus-cpp CONFIG REQUIRED)

# Find CURL (required for AI analyzer)
find_package(CURL REQUIRED)

# Optional: Google Test for unit testing
find_package(GTest QUIET)

# Compiler flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -O0")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3")

# Source files
set(EVENT_SOURCES
    src/event.cpp
)

set(CONSUMER_SOURCES
    src/kafka_consumer.cpp
)

set(STORE_SOURCES
    src/event_store.cpp
)

set(METRICS_SOURCES
    src/metrics.cpp
)

set(AI_SOURCES
    src/ai_analyzer.cpp
)

# Create library from event.cpp
add_library(streamguard_event STATIC ${EVENT_SOURCES})

target_link_libraries(streamguard_event
    nlohmann_json::nlohmann_json
    pthread
)

# Create library from event_store.cpp
add_library(streamguard_store STATIC ${STORE_SOURCES})

target_link_libraries(streamguard_store
    streamguard_event
    RocksDB::rocksdb
    nlohmann_json::nlohmann_json
    pthread
)

# Create library from kafka_consumer.cpp
add_library(streamguard_consumer STATIC ${CONSUMER_SOURCES})

target_link_libraries(streamguard_consumer
    streamguard_event
    ${RDKAFKA_LIB}
    nlohmann_json::nlohmann_json
    pthread
)

# Create library from metrics.cpp
add_library(streamguard_metrics STATIC ${METRICS_SOURCES})

target_link_libraries(streamguard_metrics
    prometheus-cpp::core
    prometheus-cpp::pull
    pthread
)

# Create library from ai_analyzer.cpp
add_library(streamguard_ai STATIC ${AI_SOURCES})

target_link_libraries(streamguard_ai
    streamguard_event
    CURL::libcurl
    nlohmann_json::nlohmann_json
    pthread
)

# Create stream processor executable
add_executable(stream-processor
    src/main.cpp
)

target_link_libraries(stream-processor
    streamguard_consumer
    streamguard_store
    streamguard_metrics
    streamguard_ai
    streamguard_event
    ${RDKAFKA_LIB}
    RocksDB::rocksdb
    prometheus-cpp::core
    prometheus-cpp::pull
    CURL::libcurl
    nlohmann_json::nlohmann_json
    pthread
)

# Embedding test executable
add_executable(test-embeddings
    test_embeddings.cpp
)

target_link_libraries(test-embeddings
    streamguard_consumer
    streamguard_store
    streamguard_ai
    streamguard_event
    ${RDKAFKA_LIB}
    RocksDB::rocksdb
    CURL::libcurl
    nlohmann_json::nlohmann_json
    pthread
)

# Unit tests (if Google Test is found)
if(GTest_FOUND)
    enable_testing()

    add_executable(event-tests
        test/event-test.cpp
    )

    target_link_libraries(event-tests
        streamguard_event
        GTest::gtest
        GTest::gtest_main
    )

    add_test(NAME EventTests COMMAND event-tests)

    message(STATUS "Google Test found - building tests")
else()
    message(STATUS "Google Test not found - skipping tests")
endif()

# Installation
install(TARGETS streamguard_event
    ARCHIVE DESTINATION lib
)

install(FILES include/event.h
    DESTINATION include/streamguard
)

# Print configuration summary
message(STATUS "")
message(STATUS "StreamGuard Stream Processor Configuration:")
message(STATUS "  C++ Standard: ${CMAKE_CXX_STANDARD}")
message(STATUS "  Build Type: ${CMAKE_BUILD_TYPE}")
message(STATUS "  Architecture: ${CMAKE_OSX_ARCHITECTURES}")
message(STATUS "  Compiler: ${CMAKE_CXX_COMPILER_ID} ${CMAKE_CXX_COMPILER_VERSION}")
message(STATUS "")
message(STATUS "Dependencies:")
message(STATUS "  nlohmann_json: ${nlohmann_json_FOUND}")
message(STATUS "  librdkafka: ${RDKAFKA_LIB}")
message(STATUS "  RocksDB: ${RocksDB_FOUND}")
message(STATUS "")