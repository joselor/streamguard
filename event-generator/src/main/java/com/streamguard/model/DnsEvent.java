package com.streamguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a DNS query event in the security event stream.
 *
 * <p>This event type captures all DNS resolution activities including:
 * <ul>
 *   <li>Domain name lookups (A, AAAA records)</li>
 *   <li>Mail server queries (MX records)</li>
 *   <li>Text record queries (TXT records, often for verification)</li>
 *   <li>Canonical name queries (CNAME records)</li>
 *   <li>Query results and response codes</li>
 * </ul>
 *
 * <p>DNS events are critical for detecting:
 * <ul>
 *   <li>Command and Control (C2) communication (domains associated with malware)</li>
 *   <li>DNS tunneling (data exfiltration through DNS queries)</li>
 *   <li>Domain Generation Algorithms (DGA) - algorithmically generated domains</li>
 *   <li>Phishing domains (typosquatting, homograph attacks)</li>
 *   <li>Newly registered domains (NRDs) - often used by attackers</li>
 *   <li>Fast-flux networks (rapidly changing DNS responses)</li>
 *   <li>Cryptomining domains</li>
 * </ul>
 *
 * <p><b>Threat Score Calculation:</b>
 * Base threat scores are typically:
 * <ul>
 *   <li>Common business domains (microsoft.com, google.com): 0.0 - 0.1</li>
 *   <li>Legitimate but unusual domains: 0.1 - 0.3</li>
 *   <li>Newly registered domains (< 30 days old): 0.3 - 0.5</li>
 *   <li>Suspicious TLDs (.tk, .ml, .ga free domains): 0.5 - 0.7</li>
 *   <li>DGA-like domains (random characters): 0.7 - 0.9</li>
 *   <li>Known malicious domains (threat intel match): 0.9 - 1.0</li>
 *   <li>Excessive TXT queries (potential tunneling): 0.6 - 0.8</li>
 * </ul>
 *
 * <p><b>Suspicious Domain Patterns:</b>
 * <ul>
 *   <li><b>DGA domains:</b> xjk29fks.com, qpwoeiruty.net (random characters)</li>
 *   <li><b>Typosquatting:</b> gooogle.com, micros0ft.com (mimicking legitimate)</li>
 *   <li><b>Long subdomains:</b> aaabbbcccdddeeefffggghhhiii.example.com (data in subdomain)</li>
 *   <li><b>Excessive queries:</b> 1000+ queries/minute (scanning or tunneling)</li>
 *   <li><b>NXDOMAIN responses:</b> Many failed lookups (reconnaissance)</li>
 *   <li><b>Unusual ports:</b> DNS queries to non-53 ports (DNS over HTTPS abuse)</li>
 * </ul>
 *
 * <p><b>DNS Tunneling Detection:</b>
 * DNS tunneling encodes data in DNS queries to bypass firewalls:
 * <ul>
 *   <li>Excessive TXT record queries (TXT records can hold more data)</li>
 *   <li>Long subdomain labels (data encoded in subdomain)</li>
 *   <li>High query frequency to same domain</li>
 *   <li>Unusual query types (NULL, private use types)</li>
 *   <li>Large response sizes</li>
 * </ul>
 *
 * <p><b>Example Usage - Malicious Domain:</b>
 * <pre>{@code
 * DnsEvent event = new DnsEvent(
 *     "192.168.1.115",                              // workstation IP
 *     "8.8.8.8",                                    // DNS server
 *     "compromised_user",                           // user
 *     0.95,                                         // very high threat
 *     "c2-command-server.suspicious-domain.tk",    // known C2 domain
 *     "A"                                           // A record query
 * );
 *
 * // Set query details
 * event.getDnsDetails().setResponseCode("NOERROR");
 * event.getDnsDetails().getResolvedIps().add("185.220.101.45");  // Known bad IP
 * event.getDnsDetails().setTtl(60);  // Short TTL (fast-flux indicator)
 * event.getDnsDetails().setCached(false);
 * }</pre>
 *
 * <p><b>Example Usage - DNS Tunneling:</b>
 * <pre>{@code
 * DnsEvent event = new DnsEvent(
 *     "192.168.1.120",
 *     "8.8.8.8",
 *     "insider_threat",
 *     0.88,
 *     "aabbccddee112233445566.data-exfil.com",  // Data in subdomain
 *     "TXT"  // TXT queries can exfiltrate more data
 * );
 *
 * // Many of these in rapid succession = tunneling
 * }</pre>
 *
 * @see Event Base event class for common fields
 * @see DnsDetails DNS query-specific details
 * @author Jose Ortuno
 * @version 1.0
 */
@JsonTypeName("dns_query")
public class DnsEvent extends Event {

    @JsonProperty("dns_details")
    private DnsDetails dnsDetails;

    /**
     * Default constructor for Jackson deserialization.
     * Initializes event_type to "dns_query" and creates empty DnsDetails.
     */
    public DnsEvent() {
        super();
        this.setEventType("dns_query");
        this.dnsDetails = new DnsDetails();
    }

    /**
     * Creates a DNS query event with essential fields.
     *
     * @param sourceIp Source IP address (machine making the DNS query)
     * @param destinationIp IP address of the DNS server
     * @param user Username associated with the query
     * @param threatScore Calculated threat score (0.0 - 1.0)
     * @param queryName Domain name being queried
     * @param queryType Type of DNS record (A, AAAA, MX, TXT, CNAME)
     */
    public DnsEvent(String sourceIp, String destinationIp, String user,
                   double threatScore, String queryName, String queryType) {
        super("dns_query", sourceIp, destinationIp, user, threatScore);
        this.dnsDetails = new DnsDetails();
        this.dnsDetails.setQueryName(queryName);
        this.dnsDetails.setQueryType(queryType);
    }

    public DnsDetails getDnsDetails() {
        return dnsDetails;
    }

    public void setDnsDetails(DnsDetails dnsDetails) {
        this.dnsDetails = dnsDetails;
    }

    @Override
    public String toString() {
        return "DnsEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", queryName='" + dnsDetails.getQueryName() + '\'' +
                ", queryType='" + dnsDetails.getQueryType() + '\'' +
                '}';
    }

    /**
     * Contains DNS query-specific details that extend the base event information.
     *
     * <p>This class encapsulates all DNS-related metadata including:
     * <ul>
     *   <li>Query information (domain name, record type)</li>
     *   <li>Response details (response code, resolved IPs)</li>
     *   <li>Caching information (cache hits reduce DNS traffic)</li>
     *   <li>Time-to-live (TTL) values</li>
     *   <li>DNS server used</li>
     * </ul>
     *
     * <p><b>DNS Record Types:</b>
     * <ul>
     *   <li><b>A:</b> IPv4 address (most common, e.g., example.com → 93.184.216.34)</li>
     *   <li><b>AAAA:</b> IPv6 address (e.g., example.com → 2606:2800:220:1:...)</li>
     *   <li><b>MX:</b> Mail exchange servers (email routing)</li>
     *   <li><b>TXT:</b> Text records (SPF, DKIM, domain verification, also abused for tunneling)</li>
     *   <li><b>CNAME:</b> Canonical name (alias for another domain)</li>
     *   <li><b>NS:</b> Name server (authoritative DNS servers for domain)</li>
     *   <li><b>PTR:</b> Reverse DNS lookup (IP to hostname)</li>
     * </ul>
     *
     * <p><b>Response Codes:</b>
     * <ul>
     *   <li><b>NOERROR:</b> Successful query, domain exists</li>
     *   <li><b>NXDOMAIN:</b> Domain doesn't exist (non-existent domain)</li>
     *   <li><b>SERVFAIL:</b> Server failure (DNS server error)</li>
     *   <li><b>REFUSED:</b> Query refused (policy or security)</li>
     * </ul>
     *
     * <p><b>Security Indicators:</b>
     * <ul>
     *   <li>Short TTL (< 300s) may indicate fast-flux networks</li>
     *   <li>Many NXDOMAIN responses may indicate DGA activity</li>
     *   <li>TXT queries with long responses may be tunneling</li>
     *   <li>Queries to unusual DNS servers may bypass security</li>
     * </ul>
     */
    public static class DnsDetails {
        @JsonProperty("query_name")
        private String queryName;

        @JsonProperty("query_type")
        private String queryType; // A, AAAA, MX, TXT, CNAME

        @JsonProperty("response_code")
        private String responseCode; // NOERROR, NXDOMAIN, SERVFAIL

        @JsonProperty("resolved_ips")
        private List<String> resolvedIps;

        @JsonProperty("ttl")
        private int ttl;

        @JsonProperty("is_cached")
        private boolean isCached;

        @JsonProperty("dns_server")
        private String dnsServer;

        /**
         * Default constructor.
         * Initializes resolvedIps as empty list and numeric fields to 0.
         */
        public DnsDetails() {
            this.resolvedIps = new ArrayList<>();
        }

        // Getters and Setters

        /**
         * Gets the domain name that was queried.
         *
         * <p>Domain analysis can reveal:
         * <ul>
         *   <li><b>Legitimate:</b> microsoft.com, google.com, aws.amazon.com</li>
         *   <li><b>Suspicious length:</b> Very long domains (> 50 chars) may be DGA</li>
         *   <li><b>Character entropy:</b> High randomness suggests DGA</li>
         *   <li><b>Subdomain depth:</b> Many levels may indicate tunneling</li>
         *   <li><b>TLD reputation:</b> .tk, .ml, .ga are often abused</li>
         * </ul>
         *
         * <p><b>DGA Detection Heuristics:</b>
         * <ul>
         *   <li>High consonant-to-vowel ratio</li>
         *   <li>Unusual character sequences</li>
         *   <li>No recognizable words</li>
         *   <li>Numeric sequences</li>
         * </ul>
         *
         * @return Domain name queried
         */
        public String getQueryName() {
            return queryName;
        }

        public void setQueryName(String queryName) {
            this.queryName = queryName;
        }

        /**
         * Gets the DNS record type queried.
         *
         * <p>Record type can indicate intent:
         * <ul>
         *   <li><b>A/AAAA:</b> Normal web browsing, application connectivity</li>
         *   <li><b>MX:</b> Email server lookup (or reconnaissance)</li>
         *   <li><b>TXT:</b> Legitimate (SPF check) or tunneling (data exfiltration)</li>
         *   <li><b>PTR:</b> Reverse lookup (sometimes reconnaissance)</li>
         *   <li><b>ANY:</b> Get all records (often used in reconnaissance)</li>
         * </ul>
         *
         * <p>Excessive TXT queries to the same domain may indicate DNS tunneling.
         *
         * @return Query type (A, AAAA, MX, TXT, CNAME, etc.)
         */
        public String getQueryType() {
            return queryType;
        }

        public void setQueryType(String queryType) {
            this.queryType = queryType;
        }

        /**
         * Gets the DNS response code.
         *
         * <p><b>Response code patterns:</b>
         * <ul>
         *   <li><b>Many NOERROR:</b> Normal traffic</li>
         *   <li><b>Many NXDOMAIN:</b> May indicate:
         *     <ul>
         *       <li>DGA malware trying multiple domains</li>
         *       <li>Misconfigured application</li>
         *       <li>Reconnaissance/scanning</li>
         *     </ul>
         *   </li>
         *   <li><b>SERVFAIL:</b> May indicate:
         *     <ul>
         *       <li>DNS server issues</li>
         *       <li>Sinkholed domain (security blocking)</li>
         *       <li>DNS poisoning attempt</li>
         *     </ul>
         *   </li>
         * </ul>
         *
         * @return Response code (NOERROR, NXDOMAIN, SERVFAIL, REFUSED)
         */
        public String getResponseCode() {
            return responseCode;
        }

        public void setResponseCode(String responseCode) {
            this.responseCode = responseCode;
        }

        /**
         * Gets the list of IP addresses the domain resolved to.
         *
         * <p>Multiple IPs may indicate:
         * <ul>
         *   <li>Load balancing (normal for large sites)</li>
         *   <li>CDN usage (content delivery networks)</li>
         *   <li>Fast-flux networks (malware infrastructure)</li>
         * </ul>
         *
         * <p><b>IP reputation checks:</b>
         * Compare resolved IPs against threat intelligence:
         * <ul>
         *   <li>Known malicious IPs</li>
         *   <li>Tor exit nodes</li>
         *   <li>Known hosting providers used by malware</li>
         *   <li>Geographic locations (unexpected countries)</li>
         * </ul>
         *
         * <p>Empty list indicates NXDOMAIN or query failure.
         *
         * @return List of resolved IP addresses (may be empty)
         */
        public List<String> getResolvedIps() {
            return resolvedIps;
        }

        public void setResolvedIps(List<String> resolvedIps) {
            this.resolvedIps = resolvedIps;
        }

        /**
         * Gets the Time-To-Live (TTL) value in seconds.
         *
         * <p>TTL indicates how long the DNS response can be cached:
         * <ul>
         *   <li><b>High TTL (> 3600s / 1 hour):</b> Static infrastructure</li>
         *   <li><b>Medium TTL (300-3600s):</b> Normal for most websites</li>
         *   <li><b>Low TTL (< 300s):</b> May indicate:
         *     <ul>
         *       <li>Load balancing/failover infrastructure</li>
         *       <li>Fast-flux networks (rapidly changing IPs to evade blocking)</li>
         *       <li>Active C2 infrastructure</li>
         *     </ul>
         *   </li>
         *   <li><b>Very low TTL (< 60s):</b> High suspicion for fast-flux</li>
         * </ul>
         *
         * <p>Malware often uses low TTLs to quickly change infrastructure.
         *
         * @return TTL in seconds (0 if not applicable)
         */
        public int getTtl() {
            return ttl;
        }

        public void setTtl(int ttl) {
            this.ttl = ttl;
        }

        /**
         * Indicates whether this response came from cache.
         *
         * <p>Cached responses:
         * <ul>
         *   <li>Reduce DNS query load</li>
         *   <li>Improve performance</li>
         *   <li>May mask recent changes (attacker registered new domain)</li>
         * </ul>
         *
         * <p>Non-cached queries may indicate:
         * <ul>
         *   <li>First access to domain (potentially newly registered)</li>
         *   <li>TTL expired</li>
         *   <li>Cache flush/invalidation</li>
         * </ul>
         *
         * @return true if response was from cache
         */
        public boolean isCached() {
            return isCached;
        }

        public void setCached(boolean cached) {
            isCached = cached;
        }

        /**
         * Gets the IP address of the DNS server that answered the query.
         *
         * <p>DNS server analysis:
         * <ul>
         *   <li><b>Corporate DNS (10.x.x.x, 172.x.x.x, 192.168.x.x):</b> Normal</li>
         *   <li><b>Public DNS (8.8.8.8, 1.1.1.1):</b> May bypass corporate controls</li>
         *   <li><b>Unusual DNS servers:</b> May indicate:
         *     <ul>
         *       <li>DNS hijacking</li>
         *       <li>Malware-controlled DNS</li>
         *       <li>DNS over HTTPS (DoH) bypass</li>
         *     </ul>
         *   </li>
         * </ul>
         *
         * <p>Workstations querying external DNS directly may bypass security controls.
         *
         * @return DNS server IP address
         */
        public String getDnsServer() {
            return dnsServer;
        }

        public void setDnsServer(String dnsServer) {
            this.dnsServer = dnsServer;
        }
    }
}
