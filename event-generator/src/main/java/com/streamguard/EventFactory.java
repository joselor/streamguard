package com.streamguard;

import com.streamguard.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Factory class for generating realistic security events.
 *
 * <p>This class creates synthetic security events with realistic data distributions
 * for testing the StreamGuard platform. Events are generated with:
 * <ul>
 *   <li>Realistic IP addresses (internal and external)</li>
 *   <li>Plausible usernames and hostnames</li>
 *   <li>Appropriate threat scores based on event characteristics</li>
 *   <li>Varied metadata and details per event type</li>
 * </ul>
 *
 * <p><b>Event Distribution:</b>
 * The factory generates events with the following distribution:
 * <ul>
 *   <li>40% auth_attempt (most common in real systems)</li>
 *   <li>25% network_connection</li>
 *   <li>20% file_access</li>
 *   <li>10% process_execution</li>
 *   <li>5% dns_query</li>
 * </ul>
 *
 * <p><b>Threat Score Distribution:</b>
 * <ul>
 *   <li>70% low threat (0.0 - 0.3) - normal activity</li>
 *   <li>20% medium threat (0.3 - 0.7) - suspicious but not confirmed</li>
 *   <li>10% high threat (0.7 - 1.0) - likely malicious</li>
 * </ul>
 *
 * @author Jose Ortuno
 * @version 1.0
 */
public class EventFactory {

    private final Random random;

    // User names for event generation
    private static final String[] USERS = {
        "alice", "bob", "charlie", "david", "eve", "frank", "grace", "henry",
        "admin", "contractor", "developer", "analyst", "manager", "guest"
    };

    // Internal IP ranges (192.168.1.0/24)
    private static final String[] INTERNAL_IPS = generateIpRange("192.168.1.", 1, 200);

    // External IPs (mix of legitimate and suspicious)
    private static final String[] EXTERNAL_IPS = {
        "8.8.8.8", "1.1.1.1", "52.1.2.3", "104.16.0.1",  // Legitimate (Google, Cloudflare, AWS)
        "185.220.101.5", "89.163.145.23", "103.45.67.89", "203.45.67.89"  // Suspicious
    };

    // DNS server IPs
    private static final String[] DNS_SERVERS = {"8.8.8.8", "1.1.1.1", "192.168.1.1"};

    // Hostnames
    private static final String[] HOSTNAMES = {
        "workstation01", "workstation02", "server01", "server02", "laptop-alice",
        "laptop-bob", "build-server", "database-01", "web-server-01"
    };

    // Geographic locations
    private static final String[] GEO_LOCATIONS = {
        "US-CA-San Francisco", "US-NY-New York", "US-TX-Austin", "US-WA-Seattle",
        "UK-LON-London", "DE-BER-Berlin", "JP-TYO-Tokyo", "AU-SYD-Sydney",
        "RU-MOW-Moscow", "CN-BJ-Beijing", "BR-SAO-Sao Paulo"  // Some suspicious
    };

    // Domain names for DNS queries
    private static final String[] DOMAINS = {
        "google.com", "microsoft.com", "amazon.com", "github.com", "stackoverflow.com",
        "c2server.suspicious.com", "malware-download.tk", "phishing-site.ml",
        "xjk29fks.com", "qpwoeiruty.net"  // DGA-like domains
    };

    // File paths
    private static final String[] FILE_PATHS = {
        "/home/user/documents/report.pdf", "/var/log/syslog", "/etc/passwd",
        "/etc/shadow", "/tmp/malware.exe", "C:\\Users\\Public\\temp.exe",
        "C:\\Windows\\System32\\config\\SAM", "/var/data/confidential/payroll.xlsx"
    };

    // Process names
    private static final String[] PROCESS_NAMES = {
        "chrome.exe", "firefox.exe", "code.exe", "bash", "python3", "java",
        "powershell.exe", "cmd.exe", "svchost.exe", "mimikatz.exe", "cryptominer.exe"
    };

    /**
     * Creates a new EventFactory with a random seed.
     */
    public EventFactory() {
        this.random = new Random();
    }

    /**
     * Creates a new EventFactory with a specific seed for reproducible testing.
     *
     * @param seed Random seed for reproducible event generation
     */
    public EventFactory(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Generates a random security event of any type.
     * Event type is selected based on realistic distribution.
     *
     * @return A randomly generated security event
     */
    public Event generateEvent() {
        // Event type distribution: 40% auth, 25% network, 20% file, 10% process, 5% dns
        int eventTypeChoice = random.nextInt(100);

        if (eventTypeChoice < 40) {
            return generateAuthEvent();
        } else if (eventTypeChoice < 65) {
            return generateNetworkEvent();
        } else if (eventTypeChoice < 85) {
            return generateFileEvent();
        } else if (eventTypeChoice < 95) {
            return generateProcessEvent();
        } else {
            return generateDnsEvent();
        }
    }

    /**
     * Generates a random authentication event.
     *
     * @return A new AuthEvent with realistic data
     */
    public AuthEvent generateAuthEvent() {
        String sourceIp = randomChoice(INTERNAL_IPS);
        String destIp = randomChoice(INTERNAL_IPS);
        String user = randomChoice(USERS);

        // 70% success, 25% failed, 5% blocked
        String status = randomWeightedChoice(
            new String[]{"success", "failed", "blocked"},
            new int[]{70, 25, 5}
        );

        // Threat score depends on status
        double threatScore;
        if ("success".equals(status)) {
            threatScore = randomThreatScore(0.0, 0.2);  // Low threat for successful auth
        } else if ("failed".equals(status)) {
            threatScore = randomThreatScore(0.3, 0.7);  // Medium threat for failures
        } else {
            threatScore = randomThreatScore(0.8, 1.0);  // High threat for blocked
        }

        String authType = randomChoice(new String[]{"password", "mfa", "sso", "api_key"});

        AuthEvent event = new AuthEvent(sourceIp, destIp, user, threatScore, authType, status);

        // Set auth details
        if ("failed".equals(status) || "blocked".equals(status)) {
            String failureReason = randomChoice(new String[]{
                "invalid_password", "account_locked", "mfa_failed", "expired_credentials"
            });
            event.getAuthDetails().setFailureReason(failureReason);
            event.getAuthDetails().setAttemptsCount(random.nextInt(10) + 1);
        }

        // Set metadata
        event.getMetadata().setHostname(randomChoice(HOSTNAMES));
        event.getMetadata().setGeoLocation(randomChoice(GEO_LOCATIONS));
        event.getMetadata().setUserAgent(generateUserAgent());

        return event;
    }

    /**
     * Generates a random network connection event.
     *
     * @return A new NetworkEvent with realistic data
     */
    public NetworkEvent generateNetworkEvent() {
        String sourceIp = randomChoice(INTERNAL_IPS);
        String destIp = randomChoice(EXTERNAL_IPS);
        String user = randomChoice(USERS);
        String protocol = randomChoice(new String[]{"tcp", "udp", "icmp"});

        // Common ports
        int destPort = randomChoice(new int[]{80, 443, 22, 3389, 445, 9050, 8080});
        int sourcePort = 49152 + random.nextInt(16384);  // Ephemeral port range

        // Suspicious ports get higher threat scores
        double threatScore;
        if (destPort == 9050 || destPort == 22) {  // Tor, SSH
            threatScore = randomThreatScore(0.5, 0.8);
        } else if (destPort == 443 || destPort == 80) {
            threatScore = randomThreatScore(0.0, 0.2);
        } else {
            threatScore = randomThreatScore(0.2, 0.5);
        }

        NetworkEvent event = new NetworkEvent(sourceIp, destIp, user, threatScore,
                                              protocol, sourcePort, destPort);

        // Set network details
        event.getNetworkDetails().setBytesSent(random.nextLong() & Long.MAX_VALUE % 10_000_000);
        event.getNetworkDetails().setBytesReceived(random.nextLong() & Long.MAX_VALUE % 10_000_000);
        event.getNetworkDetails().setDurationMs(random.nextInt(300000));  // Up to 5 minutes
        event.getNetworkDetails().setConnectionState(
            randomChoice(new String[]{"established", "closed", "timeout"})
        );

        // Set metadata
        event.getMetadata().setHostname(randomChoice(HOSTNAMES));
        event.getMetadata().setGeoLocation(randomChoice(GEO_LOCATIONS));

        return event;
    }

    /**
     * Generates a random file access event.
     *
     * @return A new FileEvent with realistic data
     */
    public FileEvent generateFileEvent() {
        String sourceIp = randomChoice(INTERNAL_IPS);
        String destIp = randomChoice(INTERNAL_IPS);
        String user = randomChoice(USERS);
        String filePath = randomChoice(FILE_PATHS);
        String operation = randomChoice(new String[]{"read", "write", "delete", "create", "modify"});

        // Sensitive files get higher threat scores
        double threatScore;
        if (filePath.contains("shadow") || filePath.contains("SAM") || filePath.contains("passwd")) {
            threatScore = randomThreatScore(0.7, 0.9);
        } else if (filePath.contains("confidential") || filePath.contains("payroll")) {
            threatScore = randomThreatScore(0.4, 0.7);
        } else if (filePath.contains("malware") || filePath.contains(".exe")) {
            threatScore = randomThreatScore(0.6, 0.95);
        } else {
            threatScore = randomThreatScore(0.0, 0.3);
        }

        FileEvent event = new FileEvent(sourceIp, destIp, user, threatScore, filePath, operation);

        // Set file details
        event.getFileDetails().setFileSizeBytes(random.nextLong() & Long.MAX_VALUE % 10_000_000);
        event.getFileDetails().setEncrypted(random.nextBoolean());
        event.getFileDetails().setPermissionsChanged(random.nextDouble() < 0.1);  // 10% chance
        event.getFileDetails().setAccessDenied(random.nextDouble() < 0.05);  // 5% chance

        // Set metadata
        event.getMetadata().setHostname(randomChoice(HOSTNAMES));
        event.getMetadata().setGeoLocation(randomChoice(GEO_LOCATIONS));

        return event;
    }

    /**
     * Generates a random process execution event.
     *
     * @return A new ProcessEvent with realistic data
     */
    public ProcessEvent generateProcessEvent() {
        String sourceIp = randomChoice(INTERNAL_IPS);
        String user = randomChoice(USERS);
        String processName = randomChoice(PROCESS_NAMES);
        String commandLine = generateCommandLine(processName);

        // Malicious processes get higher threat scores
        double threatScore;
        if (processName.contains("mimikatz") || processName.contains("cryptominer")) {
            threatScore = randomThreatScore(0.9, 1.0);
        } else if (processName.equals("powershell.exe") && commandLine.contains("-enc")) {
            threatScore = randomThreatScore(0.7, 0.9);
        } else {
            threatScore = randomThreatScore(0.0, 0.3);
        }

        ProcessEvent event = new ProcessEvent(sourceIp, sourceIp, user, threatScore,
                                             processName, commandLine);

        // Set process details
        event.getProcessDetails().setProcessId(random.nextInt(30000) + 1000);
        event.getProcessDetails().setParentProcessId(random.nextInt(1000) + 1);
        event.getProcessDetails().setUserPrivileges(
            randomChoice(new String[]{"system", "admin", "standard"})
        );
        event.getProcessDetails().setSigned(!processName.contains("mimikatz") &&
                                            !processName.contains("cryptominer"));
        if (event.getProcessDetails().isSigned()) {
            event.getProcessDetails().setSigner("Microsoft Corporation");
        }

        // Set metadata
        event.getMetadata().setHostname(randomChoice(HOSTNAMES));
        event.getMetadata().setGeoLocation(randomChoice(GEO_LOCATIONS));

        return event;
    }

    /**
     * Generates a random DNS query event.
     *
     * @return A new DnsEvent with realistic data
     */
    public DnsEvent generateDnsEvent() {
        String sourceIp = randomChoice(INTERNAL_IPS);
        String dnsServer = randomChoice(DNS_SERVERS);
        String user = randomChoice(USERS);
        String domain = randomChoice(DOMAINS);
        String queryType = randomWeightedChoice(
            new String[]{"A", "AAAA", "MX", "TXT", "CNAME"},
            new int[]{60, 20, 5, 10, 5}
        );

        // Suspicious domains get higher threat scores
        double threatScore;
        if (domain.contains("suspicious") || domain.contains("malware") ||
            domain.contains("phishing") || domain.length() > 20) {
            threatScore = randomThreatScore(0.7, 0.95);
        } else {
            threatScore = randomThreatScore(0.0, 0.2);
        }

        DnsEvent event = new DnsEvent(sourceIp, dnsServer, user, threatScore, domain, queryType);

        // Set DNS details
        event.getDnsDetails().setResponseCode(
            random.nextDouble() < 0.9 ? "NOERROR" : "NXDOMAIN"
        );

        if ("NOERROR".equals(event.getDnsDetails().getResponseCode())) {
            // Add resolved IPs
            List<String> resolvedIps = new ArrayList<>();
            resolvedIps.add(randomChoice(EXTERNAL_IPS));
            event.getDnsDetails().setResolvedIps(resolvedIps);
        }

        event.getDnsDetails().setTtl(random.nextInt(3600) + 60);  // 60s to 1 hour
        event.getDnsDetails().setCached(random.nextBoolean());
        event.getDnsDetails().setDnsServer(dnsServer);

        // Set metadata
        event.getMetadata().setHostname(randomChoice(HOSTNAMES));
        event.getMetadata().setGeoLocation(randomChoice(GEO_LOCATIONS));

        return event;
    }

    // Helper methods

    /**
     * Generates a realistic command line for a given process name.
     */
    private String generateCommandLine(String processName) {
        switch (processName) {
            case "powershell.exe":
                if (random.nextDouble() < 0.2) {  // 20% suspicious
                    return "powershell.exe -ExecutionPolicy Bypass -EncodedCommand JABzAD0ATgBlAHcA...";
                }
                return "powershell.exe -File script.ps1";

            case "chrome.exe":
                return "chrome.exe --type=renderer";

            case "mimikatz.exe":
                return "mimikatz.exe sekurlsa::logonpasswords";

            case "cryptominer.exe":
                return "cryptominer.exe --pool mining.pool.com";

            default:
                return processName;
        }
    }

    /**
     * Generates a realistic user agent string.
     */
    private String generateUserAgent() {
        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
            "curl/7.68.0",
            "Python-urllib/3.8"
        };
        return randomChoice(userAgents);
    }

    /**
     * Generates a threat score within the given range.
     */
    private double randomThreatScore(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    /**
     * Selects a random element from an array.
     */
    private <T> T randomChoice(T[] array) {
        return array[random.nextInt(array.length)];
    }

    /**
     * Selects a random element from an int array.
     */
    private int randomChoice(int[] array) {
        return array[random.nextInt(array.length)];
    }

    /**
     * Selects a random choice based on weights.
     */
    private String randomWeightedChoice(String[] choices, int[] weights) {
        int totalWeight = 0;
        for (int weight : weights) {
            totalWeight += weight;
        }

        int randomValue = random.nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (int i = 0; i < choices.length; i++) {
            cumulativeWeight += weights[i];
            if (randomValue < cumulativeWeight) {
                return choices[i];
            }
        }

        return choices[0];  // Fallback
    }

    /**
     * Generates an IP range as an array.
     */
    private static String[] generateIpRange(String prefix, int start, int end) {
        String[] ips = new String[end - start + 1];
        for (int i = 0; i < ips.length; i++) {
            ips[i] = prefix + (start + i);
        }
        return ips;
    }
}
