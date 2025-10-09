package com.streamguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Represents a network connection event in the security event stream.
 *
 * <p>This event type captures all network-level activities including:
 * <ul>
 *   <li>Outbound connections (user to internet/external services)</li>
 *   <li>Inbound connections (external to internal systems)</li>
 *   <li>Lateral movement (internal system to internal system)</li>
 *   <li>Data transfer metrics (bytes sent/received)</li>
 * </ul>
 *
 * <p>Network events are critical for detecting:
 * <ul>
 *   <li>Data exfiltration (large outbound transfers to unusual destinations)</li>
 *   <li>Command and control (C2) communications</li>
 *   <li>Port scanning and reconnaissance</li>
 *   <li>Lateral movement during breach</li>
 *   <li>DNS tunneling and covert channels</li>
 *   <li>Connections to known malicious IPs</li>
 * </ul>
 *
 * <p><b>Threat Score Calculation:</b>
 * Base threat scores are typically:
 * <ul>
 *   <li>Normal business traffic (HTTPS to known sites): 0.0 - 0.1</li>
 *   <li>Unusual port (non-standard): 0.2 - 0.4</li>
 *   <li>High data volume: 0.3 - 0.5</li>
 *   <li>Connection to unusual geographic location: 0.5 - 0.7</li>
 *   <li>Connection to known malicious IP: 0.8 - 1.0</li>
 *   <li>Suspicious protocol/port combination: 0.7 - 0.9</li>
 * </ul>
 *
 * <p><b>Common Suspicious Patterns:</b>
 * <ul>
 *   <li>SSH (port 22) from workstation to external IP</li>
 *   <li>Large data transfers during off-hours</li>
 *   <li>Connections to Tor exit nodes</li>
 *   <li>Connections to countries not in normal business operations</li>
 *   <li>Reverse shells (unusual source ports)</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * NetworkEvent event = new NetworkEvent(
 *     "192.168.1.100",       // source (internal workstation)
 *     "185.220.101.45",      // destination (suspicious external IP)
 *     "jane.smith",          // user
 *     0.88,                  // high threat score
 *     "tcp",                 // protocol
 *     49152,                 // source port (ephemeral)
 *     22                     // destination port (SSH - unusual from workstation)
 * );
 *
 * // Set transfer details
 * event.getNetworkDetails().setBytesSent(524288000);  // 500MB uploaded
 * event.getNetworkDetails().setDurationMs(180000);     // 3 minutes
 * event.getNetworkDetails().setConnectionState("established");
 * }</pre>
 *
 * @see Event Base event class for common fields
 * @see NetworkDetails Network-specific connection details
 * @author StreamGuard Team
 * @version 1.0
 */
@JsonTypeName("network_connection")
public class NetworkEvent extends Event {

    @JsonProperty("network_details")
    private NetworkDetails networkDetails;

    /**
     * Default constructor for Jackson deserialization.
     * Initializes event_type to "network_connection" and creates empty NetworkDetails.
     */
    public NetworkEvent() {
        super();
        this.setEventType("network_connection");
        this.networkDetails = new NetworkDetails();
    }

    /**
     * Creates a network connection event with essential fields.
     *
     * @param sourceIp Source IP address of the connection
     * @param destinationIp Destination IP address
     * @param user Username associated with the connection
     * @param threatScore Calculated threat score (0.0 - 1.0)
     * @param protocol Network protocol (tcp, udp, icmp)
     * @param sourcePort Source port number (0-65535)
     * @param destPort Destination port number (0-65535)
     */
    public NetworkEvent(String sourceIp, String destinationIp, String user,
                       double threatScore, String protocol, int sourcePort, int destPort) {
        super("network_connection", sourceIp, destinationIp, user, threatScore);
        this.networkDetails = new NetworkDetails();
        this.networkDetails.setProtocol(protocol);
        this.networkDetails.setSourcePort(sourcePort);
        this.networkDetails.setDestinationPort(destPort);
    }

    public NetworkDetails getNetworkDetails() {
        return networkDetails;
    }

    public void setNetworkDetails(NetworkDetails networkDetails) {
        this.networkDetails = networkDetails;
    }

    @Override
    public String toString() {
        return "NetworkEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", sourceIp='" + getSourceIp() + '\'' +
                ", destinationIp='" + getDestinationIp() + '\'' +
                ", protocol='" + networkDetails.getProtocol() + '\'' +
                ", port=" + networkDetails.getDestinationPort() +
                '}';
    }

    /**
     * Contains network connection-specific details that extend the base event information.
     *
     * <p>This class encapsulates all network-related metadata including:
     * <ul>
     *   <li>Layer 4 protocol information (TCP/UDP/ICMP)</li>
     *   <li>Port numbers (source and destination)</li>
     *   <li>Data transfer metrics (bytes sent/received)</li>
     *   <li>Connection timing and state</li>
     *   <li>DNS resolution information (if available)</li>
     * </ul>
     *
     * <p><b>Protocol Types:</b>
     * <ul>
     *   <li><b>tcp:</b> Transmission Control Protocol (connection-oriented, most common)</li>
     *   <li><b>udp:</b> User Datagram Protocol (connectionless, DNS, VoIP)</li>
     *   <li><b>icmp:</b> Internet Control Message Protocol (ping, traceroute)</li>
     * </ul>
     *
     * <p><b>Connection States:</b>
     * <ul>
     *   <li><b>established:</b> Active, data flowing</li>
     *   <li><b>closed:</b> Connection terminated normally</li>
     *   <li><b>timeout:</b> Connection timed out (may indicate filtering/blocking)</li>
     * </ul>
     *
     * <p><b>Common Well-Known Ports:</b>
     * <ul>
     *   <li>22: SSH</li>
     *   <li>80: HTTP</li>
     *   <li>443: HTTPS</li>
     *   <li>3389: RDP</li>
     *   <li>445: SMB</li>
     * </ul>
     */
    public static class NetworkDetails {
        @JsonProperty("protocol")
        private String protocol; // tcp, udp, icmp

        @JsonProperty("source_port")
        private int sourcePort;

        @JsonProperty("destination_port")
        private int destinationPort;

        @JsonProperty("bytes_sent")
        private long bytesSent;

        @JsonProperty("bytes_received")
        private long bytesReceived;

        @JsonProperty("duration_ms")
        private long durationMs;

        @JsonProperty("destination_hostname")
        private String destinationHostname;

        @JsonProperty("connection_state")
        private String connectionState; // established, closed, timeout

        /**
         * Default constructor.
         * Initializes numeric fields to 0.
         */
        public NetworkDetails() {}

        // Getters and Setters

        /**
         * Gets the network protocol.
         *
         * @return Protocol name (tcp, udp, icmp)
         */
        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        /**
         * Gets the source port number.
         *
         * <p>For client connections, this is typically an ephemeral port (49152-65535).
         * For server responses, this is usually a well-known port (0-1023).
         *
         * @return Source port number (0-65535)
         */
        public int getSourcePort() {
            return sourcePort;
        }

        public void setSourcePort(int sourcePort) {
            this.sourcePort = sourcePort;
        }

        /**
         * Gets the destination port number.
         *
         * <p>This is typically the service port that the client is connecting to.
         * Well-known ports (0-1023) indicate standard services.
         * High ports may indicate custom applications or suspicious activity.
         *
         * @return Destination port number (0-65535)
         */
        public int getDestinationPort() {
            return destinationPort;
        }

        public void setDestinationPort(int destinationPort) {
            this.destinationPort = destinationPort;
        }

        /**
         * Gets the number of bytes sent (uploaded).
         *
         * <p>Large outbound transfers may indicate:
         * <ul>
         *   <li>Data exfiltration</li>
         *   <li>Backup operations (legitimate)</li>
         *   <li>File uploads (legitimate)</li>
         * </ul>
         *
         * <p>Threshold for suspicious activity: typically > 100MB in short duration.
         *
         * @return Bytes sent (0 if not measured)
         */
        public long getBytesSent() {
            return bytesSent;
        }

        public void setBytesSent(long bytesSent) {
            this.bytesSent = bytesSent;
        }

        /**
         * Gets the number of bytes received (downloaded).
         *
         * @return Bytes received (0 if not measured)
         */
        public long getBytesReceived() {
            return bytesReceived;
        }

        public void setBytesReceived(long bytesReceived) {
            this.bytesReceived = bytesReceived;
        }

        /**
         * Gets the connection duration in milliseconds.
         *
         * <p>Very short connections (< 100ms) may indicate:
         * <ul>
         *   <li>Port scanning</li>
         *   <li>Failed connection attempts</li>
         * </ul>
         *
         * <p>Very long connections may indicate:
         * <ul>
         *   <li>Persistent shells/backdoors</li>
         *   <li>Large file transfers</li>
         *   <li>Remote desktop sessions (legitimate)</li>
         * </ul>
         *
         * @return Duration in milliseconds
         */
        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }

        /**
         * Gets the resolved hostname of the destination IP.
         * May be null if DNS resolution was not performed or failed.
         *
         * <p>Useful for identifying legitimate services vs. suspicious domains.
         *
         * @return Hostname, or null if not resolved
         */
        public String getDestinationHostname() {
            return destinationHostname;
        }

        public void setDestinationHostname(String destinationHostname) {
            this.destinationHostname = destinationHostname;
        }

        /**
         * Gets the connection state.
         *
         * @return Connection state (established, closed, timeout)
         */
        public String getConnectionState() {
            return connectionState;
        }

        public void setConnectionState(String connectionState) {
            this.connectionState = connectionState;
        }

        @Override
        public String toString() {
            return "NetworkDetails{" +
                    "protocol='" + protocol + '\'' +
                    ", sourcePort=" + sourcePort +
                    ", destinationPort=" + destinationPort +
                    ", connectionState='" + connectionState + '\'' +
                    '}';
        }
    }
}
