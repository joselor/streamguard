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
)

# Link directories
link_directories(/opt/homebrew/lib)

# Find required packages
# Note: Only nlohmann_json is needed for event.cpp
# RocksDB and RdKafka will be added when needed in later user stories
find_package(nlohmann_json 3.2.0 REQUIRED)

# RocksDB and Kafka (optional for now - will be required later)
find_library(ROCKSDB_LIB rocksdb HINTS /opt/homebrew/lib)
find_library(RDKAFKA_LIB rdkafka++ HINTS /opt/homebrew/lib)

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

# For now, just create a library from event.cpp
# We'll add the main executable later
add_library(streamguard_event STATIC ${EVENT_SOURCES})

target_link_libraries(streamguard_event
    nlohmann_json::nlohmann_json
    pthread
)

# Add RocksDB if found
if(ROCKSDB_LIB)
    target_link_libraries(streamguard_event ${ROCKSDB_LIB})
endif()

# Main executable (will be added in future user stories)
# For now, commented out since we don't have main.cpp yet
# set(MAIN_SOURCES
#     src/main.cpp
# )
# add_executable(stream_processor ${MAIN_SOURCES} ${EVENT_SOURCES})
# target_link_libraries(stream_processor
#     RocksDB::rocksdb
#     RdKafka::rdkafka++
#     nlohmann_json::nlohmann_json
#     pthread
# )

# Unit tests (if Google Test is found)
if(GTest_FOUND)
    enable_testing()
    
    add_executable(event_tests
        test/event_test.cpp
    )
    
    target_link_libraries(event_tests
        streamguard_event
        GTest::gtest
        GTest::gtest_main
    )
    
    add_test(NAME EventTests COMMAND event_tests)
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
message(STATUS "  Install Prefix: ${CMAKE_INSTALL_PREFIX}")
message(STATUS "")
message(STATUS "Dependencies:")
if(ROCKSDB_LIB)
    message(STATUS "  RocksDB: Found (${ROCKSDB_LIB})")
else()
    message(STATUS "  RocksDB: Not Required Yet")
endif()
if(RDKAFKA_LIB)
    message(STATUS "  RdKafka: Found (${RDKAFKA_LIB})")
else()
    message(STATUS "  RdKafka: Not Required Yet")
endif()
message(STATUS "  nlohmann_json: ${nlohmann_json_FOUND}")
if(GTest_FOUND)
    message(STATUS "  Google Test: Found")
else()
    message(STATUS "  Google Test: Not Found (tests disabled)")
endif()
message(STATUS "")