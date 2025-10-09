# StreamGuard Event Schema Documentation

## Overview

The StreamGuard platform uses a unified event schema to represent various types of security events flowing through the system. All events share a common structure with type-specific metadata.

## Event Types

| Type | Description | Use Case |
|------|-------------|----------|
| `auth_attempt` | Authentication attempts (login, logout) | Brute force detection, credential monitoring |
| `network_connection` | Network connections and communications | C2 detection, data exfiltration |
| `file_access` | File system access events | Sensitive data access, ransomware detection |
| `process_execution` | Process/application execution | Malware execution, unauthorized processes |
| `dns_query` | DNS resolution requests | C2 communication, DNS tunneling |

## Core Schema

### Required Fields

```json
{
  "event_id": "evt_XXXXXXXXXXXX",    // Unique identifier (12 alphanumeric chars)
  "timestamp": 1704067200000,         // Unix timestamp in milliseconds
  "event_type": "auth_attempt",       // One of the event types above
  "source_ip": "192.168.1.100",       // Source IP address (IPv4)
  "user": "alice",                    // Username/principal
  "status": "success",                // Event status (success/failed/blocked/pending)
  "threat_score": 0.85               // AI-calculated threat score (0.0-1.0)
}
```

### Optional Fields

```json
{
  "destination_ip": "10.0.0.5",      // Destination IP (for network events)
  "metadata": {                       // Event-specific details
    // See metadata section below
  }
}
```

## Event Status Values

- `success`: Event completed successfully
- `failed`: Event failed (e.g., authentication failure)
- `blocked`: Event was blocked by security policy
- `pending`: Event is being processed

## Threat Score

The `threat_score` field (0.0 to 1.0) represents the AI-calculated likelihood of malicious intent:

- **0.0 - 0.3**: Low threat (normal behavior)
- **0.3 - 0.6**: Medium threat (suspicious, warrants monitoring)
- **0.6 - 0.9**: High threat (likely malicious, requires investigation)
- **0.9 - 1.0**: Critical threat (immediate action required)

## Metadata Fields

Metadata contains event-specific information:

### Common Metadata

- `user_agent` (string): Browser/client user agent
- `geo_location` (string): Geographic location (format: `CC-ST-City` e.g., `US-MN-Minneapolis`)

### Network Event Metadata

- `port` (integer): Network port (1-65535)
- `protocol` (string): Network protocol (TCP, UDP, ICMP, etc.)
- `bytes_transferred` (integer): Number of bytes transmitted

### File Event Metadata

- `file_path` (string): Full path to the file
- `process_name` (string): Name of the process accessing the file

### Process Event Metadata

- `process_name` (string): Name of the executed process
- `file_path` (string): Path to the executable

### DNS Event Metadata

- `domain` (string): Requested domain name
- `protocol` (string): DNS protocol (UDP, TCP, DoH)
- `port` (integer): DNS port (typically 53)

## Example Events

### Authentication Event (Success)

```json
{
  "event_id": "evt_a1b2c3d4e5f6",
  "timestamp": 1704067200000,
  "event_type": "auth_attempt",
  "source_ip": "192.168.1.100",
  "destination_ip": "10.0.0.5",
  "user": "alice",
  "status": "success",
  "threat_score": 0.05,
  "metadata": {
    "user_agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    "geo_location": "US-MN-Minneapolis"
  }
}
```

### Authentication Event (Failed - High Threat)

```json
{
  "event_id": "evt_x9y8z7w6v5u4",
  "timestamp": 1704067260000,
  "event_type": "auth_attempt",
  "source_ip": "203.45.67.89",
  "destination_ip": "10.0.0.5",
  "user": "admin",
  "status": "failed",
  "threat_score": 0.85,
  "metadata": {
    "user_agent": "curl/7.68.0",
    "geo_location": "RU-MOW-Moscow"
  }
}
```

### Network Connection (Suspicious)

```json
{
  "event_id": "evt_m5n6o7p8q9r0",
  "timestamp": 1704067320000,
  "event_type": "network_connection",
  "source_ip": "192.168.1.50",
  "destination_ip": "185.220.101.5",
  "user": "bob",
  "status": "success",
  "threat_score": 0.62,
  "metadata": {
    "port": 9050,
    "protocol": "TCP",
    "bytes_transferred": 1048576,
    "geo_location": "US-CA-San Francisco"
  }
}
```

### Process Execution (Blocked Malware)

```json
{
  "event_id": "evt_d7e8f9g0h1i2",
  "timestamp": 1704067440000,
  "event_type": "process_execution",
  "source_ip": "192.168.1.120",
  "user": "david",
  "status": "blocked",
  "threat_score": 0.95,
  "metadata": {
    "process_name": "cryptominer.exe",
    "file_path": "/tmp/malware/cryptominer.exe",
    "geo_location": "US-TX-Austin"
  }
}
```

## Validation Rules

### Event ID
- Format: `evt_` followed by exactly 12 alphanumeric characters
- Example: `evt_a1b2c3d4e5f6`
- Regex: `^evt_[a-zA-Z0-9]{12}$`

### Timestamp
- Unix timestamp in milliseconds
- Must be within reasonable range (2020-2033)
- Range: 1600000000000 to 2000000000000

### IP Addresses
- IPv4 format: `xxx.xxx.xxx.xxx`
- Each octet: 0-255

### Threat Score
- Float between 0.0 and 1.0 (inclusive)
- Precision: typically 2 decimal places

### Geographic Location
- Format: `COUNTRY_CODE-STATE_CODE-City`
- Example: `US-MN-Minneapolis`
- Pattern: `^[A-Z]{2}-[A-Z]{2}-.*$`

## Usage in Code

### Java

```java
import com.streamguard.model.Event;
import com.streamguard.model.EventType;
import com.streamguard.model.EventStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

// Create event
Event event = new Event.Builder()
    .eventId("evt_test12345678")
    .timestamp(System.currentTimeMillis())
    .eventType(EventType.AUTH_ATTEMPT)
    .sourceIp("192.168.1.100")
    .user("alice")
    .status(EventStatus.SUCCESS)
    .threatScore(0.05)
    .build();

// Serialize to JSON
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(event);

// Deserialize from JSON
Event parsed = mapper.readValue(json, Event.class);

// Validate
if (event.isValid()) {
    // Process event
}
```

### C++

```cpp
#include "event.h"
#include <nlohmann/json.hpp>

using namespace streamguard;

// Create event
Event event;
event.event_id = "evt_test12345678";
event.timestamp = std::time(nullptr) * 1000;
event.event_type = EventType::AUTH_ATTEMPT;
event.source_ip = "192.168.1.100";
event.user = "alice";
event.status = EventStatus::SUCCESS;
event.threat_score = 0.05;

// Serialize to JSON
std::string json = event.toJson();

// Deserialize from JSON
Event parsed = Event::fromJson(json);

// Validate
if (event.isValid()) {
    // Process event
}
```

## Extensibility

The schema supports extensibility through:

1. **Custom Metadata Fields**: Add application-specific fields to `metadata.custom_fields`
2. **New Event Types**: Additional event types can be added to the enum
3. **Forward Compatibility**: Unknown fields are ignored during deserialization

## Performance Considerations

- **Compact Representation**: Use minimal field names for reduced payload size
- **Efficient Parsing**: nlohmann/json (C++) and Jackson (Java) provide high-performance parsing
- **Validation**: Perform validation only when necessary (e.g., at ingestion boundaries)
- **Caching**: Event schemas are statically defined; parsers can be reused

## Related Documentation

- [Architecture Overview](architecture.md)
- [Event Generator](../event-generator/README.md)
- [Stream Processor](../stream-processor/README.md)
- [Query API](../query-api/README.md)