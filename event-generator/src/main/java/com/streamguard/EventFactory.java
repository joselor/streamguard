package com.streamguard;

import com.streamguard.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Factory class for generating realistic security events with user behavior patterns.
 *
 * <p>This enhanced version creates events with:
 * <ul>
 *   <li>Consistent user profiles (each user has typical IPs, locations, patterns)</li>
 *   <li>Realistic behavioral patterns (users active at certain hours)</li>
 *   <li>Occasional anomalous events to trigger detection</li>
 *   <li>Varied metadata and details per event type</li>
 * </ul>
 *
 * @author Jose Ortuno
 * @version 2.0 - Enhanced for anomaly detection demo
 */
public class EventFactory {

    private final Random random;
    private final Map<String, UserProfile> userProfiles = new HashMap<>();
    private long eventCounter = 0;

    // User names for event generation
    private static final String[] USERS = {
        "alice", "bob", "charlie", "david", "eve", "frank", "grace", "henry",
        "admin", "contractor", "developer", "analyst", "manager", "guest"
    };

    // Base IP pools for users
    private static final String[] BASE_IPS = {
        "192.168.1.10", "192.168.1.20", "192.168.1.30", "192.168.1.40",
        "192.168.1.50", "192.168.1.60", "192.168.1.70", "192.168.1.80",
        "192.168.1.90", "192.168.1.100", "192.168.1.110", "192.168.1.120",
        "192.168.1.130", "192.168.1.140"
    };

    // External IPs
    private static final String[] EXTERNAL_IPS = {
        "8.8.8.8", "1.1.1.1", "52.1.2.3", "104.16.0.1",
        "185.220.101.5", "89.163.145.23", "103.45.67.89", "203.45.67.89"
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
        "UK-LON-London", "DE-BER-Berlin", "JP-TYO-Tokyo", "AU-SYD-Sydney"
    };

    private static final String[] SUSPICIOUS_GEO_LOCATIONS = {
        "RU-MOW-Moscow", "CN-BJ-Beijing", "KP-PYO-Pyongyang"
    };

    // Domain names for DNS queries
    private static final String[] DOMAINS = {
        "google.com", "microsoft.com", "amazon.com", "github.com", "stackoverflow.com"
    };

    private static final String[] SUSPICIOUS_DOMAINS = {
        "c2server.suspicious.com", "malware-download.tk", "phishing-site.ml",
        "xjk29fks.com", "qpwoeiruty.net"
    };

    // File paths
    private static final String[] FILE_PATHS = {
        "/home/user/documents/report.pdf", "/var/log/syslog", "/home/user/data.csv",
        "C:\\Users\\Public\\Documents\\notes.txt", "/tmp/script.sh"
    };

    private static final String[] SENSITIVE_FILE_PATHS = {
        "/etc/passwd", "/etc/shadow", "/tmp/malware.exe",
        "C:\\Windows\\System32\\config\\SAM", "/var/data/confidential/payroll.xlsx"
    };

    // Process names
    private static final String[] PROCESS_NAMES = {
        "chrome.exe", "firefox.exe", "code.exe", "bash", "python3", "java"
    };

    private static final String[] SUSPICIOUS_PROCESS_NAMES = {
        "powershell.exe", "cmd.exe", "mimikatz.exe", "cryptominer.exe"
    };

    /**
     * User profile to maintain consistent behavior
     */
    private static class UserProfile {
        String primaryIp;
        String[] alternateIps;  // Secondary IPs (10-20% usage)
        String primaryLocation;
        int[] activeHours;  // Hours when user is typically active

        UserProfile(String primaryIp, String[] alternateIps, String primaryLocation, int[] activeHours) {
            this.primaryIp = primaryIp;
            this.alternateIps = alternateIps;
            this.primaryLocation = primaryLocation;
            this.activeHours = activeHours;
        }
    }

    /**
     * Creates a new EventFactory with a random seed.
     */
    public EventFactory() {
        this.random = new Random();
        initializeUserProfiles();
    }

    /**
     * Creates a new EventFactory with a specific seed for reproducible testing.
     */
    public EventFactory(long seed) {
        this.random = new Random(seed);
        initializeUserProfiles();
    }

    /**
     * Initialize consistent user profiles with realistic patterns
     */
    private void initializeUserProfiles() {
        for (int i = 0; i < USERS.length && i < BASE_IPS.length; i++) {
            String user = USERS[i];
            String primaryIp = BASE_IPS[i];

            // Give each user 1-2 alternate IPs
            String[] alternateIps = new String[]{
                generateRandomIp("192.168.1."),
                generateRandomIp("192.168.1.")
            };

            // Assign primary location
            String primaryLocation = GEO_LOCATIONS[i % GEO_LOCATIONS.length];

            // Define active hours (8am-6pm workday for most users)
            int[] activeHours;
            if (user.equals("admin") || user.equals("contractor")) {
                // Admin/contractor: odd hours sometimes
                activeHours = new int[]{0, 1, 2, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 22, 23};
            } else {
                // Regular users: business hours
                activeHours = new int[]{8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18};
            }

            userProfiles.put(user, new UserProfile(primaryIp, alternateIps, primaryLocation, activeHours));
        }
    }

    /**
     * Generates a random IP in the given subnet
     */
    private String generateRandomIp(String prefix) {
        return prefix + (random.nextInt(200) + 1);
    }

    /**
     * Generates a random security event with realistic user behavior patterns.
     * Every ~50th event is intentionally anomalous for detection testing.
     */
    public Event generateEvent() {
        eventCounter++;

        // 2% of events are intentionally anomalous
        boolean shouldBeAnomalous = (eventCounter % 50 == 0);

        // Event type distribution: 40% auth, 25% network, 20% file, 10% process, 5% dns
        int eventTypeChoice = random.nextInt(100);

        // For anomalous events, bias towards unusual event types to trigger type_anomaly
        if (shouldBeAnomalous) {
            // 50% chance of doing unusual event types (process exec or DNS for regular users)
            eventTypeChoice = random.nextBoolean() ? 90 : eventTypeChoice;  // Force to process or DNS range
        }

        if (eventTypeChoice < 40) {
            return generateAuthEvent(shouldBeAnomalous);
        } else if (eventTypeChoice < 65) {
            return generateNetworkEvent(shouldBeAnomalous);
        } else if (eventTypeChoice < 85) {
            return generateFileEvent(shouldBeAnomalous);
        } else if (eventTypeChoice < 95) {
            return generateProcessEvent(shouldBeAnomalous);
        } else {
            return generateDnsEvent(shouldBeAnomalous);
        }
    }

    /**
     * Get source IP for a user (80% primary, 20% alternate)
     */
    private String getSourceIpForUser(String user, boolean forceAnomalous) {
        UserProfile profile = userProfiles.get(user);
        if (profile == null) {
            return generateRandomIp("192.168.1.");
        }

        if (forceAnomalous) {
            // Anomalous: completely new IP
            return generateRandomIp("192.168.2.");  // Different subnet
        }

        // 80% primary IP, 20% alternate IPs
        if (random.nextDouble() < 0.8) {
            return profile.primaryIp;
        } else {
            return randomChoice(profile.alternateIps);
        }
    }

    /**
     * Get location for a user (90% primary, 10% other)
     */
    private String getLocationForUser(String user, boolean forceAnomalous) {
        UserProfile profile = userProfiles.get(user);
        if (profile == null) {
            return randomChoice(GEO_LOCATIONS);
        }

        if (forceAnomalous) {
            // Anomalous: suspicious location
            return randomChoice(SUSPICIOUS_GEO_LOCATIONS);
        }

        // 90% primary location
        if (random.nextDouble() < 0.9) {
            return profile.primaryLocation;
        } else {
            return randomChoice(GEO_LOCATIONS);
        }
    }

    /**
     * Check if current hour is typical for user
     */
    private boolean isTypicalHour(String user) {
        UserProfile profile = userProfiles.get(user);
        if (profile == null) return true;

        int currentHour = getCurrentHour();
        for (int hour : profile.activeHours) {
            if (hour == currentHour) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get current hour (0-23)
     */
    private int getCurrentHour() {
        return (int) ((System.currentTimeMillis() / 1000 / 3600) % 24);
    }

    /**
     * Generates an authentication event with realistic patterns
     */
    public AuthEvent generateAuthEvent(boolean forceAnomalous) {
        String user = randomChoice(USERS);
        String sourceIp = getSourceIpForUser(user, forceAnomalous);
        String destIp = randomChoice(BASE_IPS);

        // Anomalous events: ALWAYS failed to trigger failure anomaly
        String status;
        double threatScore;

        if (forceAnomalous) {
            status = "failed";  // Always failed for anomalous auth events
            threatScore = randomThreatScore(0.7, 0.95);
        } else {
            status = randomWeightedChoice(
                new String[]{"success", "failed", "blocked"},
                new int[]{92, 6, 2}  // Normal users: 92% success, very few failures
            );

            if ("success".equals(status)) {
                threatScore = randomThreatScore(0.0, 0.2);
            } else if ("failed".equals(status)) {
                threatScore = randomThreatScore(0.3, 0.6);
            } else {
                threatScore = randomThreatScore(0.7, 0.9);
            }
        }

        String authType = randomChoice(new String[]{"password", "mfa", "sso", "api_key"});
        AuthEvent event = new AuthEvent(sourceIp, destIp, user, threatScore, authType, status);

        if ("failed".equals(status) || "blocked".equals(status)) {
            String failureReason = randomChoice(new String[]{
                "invalid_password", "account_locked", "mfa_failed", "expired_credentials"
            });
            event.getAuthDetails().setFailureReason(failureReason);
            event.getAuthDetails().setAttemptsCount(forceAnomalous ? random.nextInt(10) + 5 : random.nextInt(3) + 1);
        }

        event.getMetadata().setHostname(randomChoice(HOSTNAMES));
        event.getMetadata().setGeoLocation(getLocationForUser(user, forceAnomalous));
        event.getMetadata().setUserAgent(generateUserAgent());

        return event;
    }

    /**
     * Generates a network connection event with realistic patterns
     */
    public NetworkEvent generateNetworkEvent(boolean forceAnomalous) {
        String user = randomChoice(USERS);
        String sourceIp = getSourceIpForUser(user, forceAnomalous);
        String destIp = randomChoice(EXTERNAL_IPS);
        String protocol = randomChoice(new String[]{"tcp", "udp", "icmp"});

        int destPort;
        double threatScore;

        if (forceAnomalous) {
            // Suspicious ports
            destPort = randomChoice(new int[]{9050, 4444, 31337, 6667});
            threatScore = randomThreatScore(0.7, 0.95);
        } else {
            destPort = randomChoice(new int[]{80, 443, 22, 3389, 8080});
            threatScore = destPort == 443 || destPort == 80 ?
                randomThreatScore(0.0, 0.2) : randomThreatScore(0.2, 0.5);
        }

        int sourcePort = 49152 + random.nextInt(16384);

        NetworkEvent event = new NetworkEvent(sourceIp, destIp, user, threatScore,
                                              protocol, sourcePort, destPort);

        event.getNetworkDetails().setBytesSent(random.nextLong() & Long.MAX_VALUE % 10_000_000);
        event.getNetworkDetails().setBytesReceived(random.nextLong() & Long.MAX_VALUE % 10_000_000);
        event.getNetworkDetails().setDurationMs(random.nextInt(300000));
        event.getNetworkDetails().setConnectionState(
            randomChoice(new String[]{"established", "closed", "timeout"})
        );

        event.getMetadata().setHostname(randomChoice(HOSTNAMES));
        event.getMetadata().setGeoLocation(getLocationForUser(user, forceAnomalous));

        return event;
    }

    /**
     * Generates a file access event with realistic patterns
     */
    public FileEvent generateFileEvent(boolean forceAnomalous) {
        String user = randomChoice(USERS);
        String sourceIp = getSourceIpForUser(user, forceAnomalous);
        String destIp = randomChoice(BASE_IPS);

        String filePath;
        String operation;
        double threatScore;

        if (forceAnomalous) {
            filePath = randomChoice(SENSITIVE_FILE_PATHS);
            operation = randomChoice(new String[]{"read", "delete", "modify"});
            threatScore = randomThreatScore(0.7, 0.95);
        } else {
            filePath = randomChoice(FILE_PATHS);
            operation = randomChoice(new String[]{"read", "write", "create"});
            threatScore = randomThreatScore(0.0, 0.3);
        }

        FileEvent event = new FileEvent(sourceIp, destIp, user, threatScore, filePath, operation);

        event.getFileDetails().setFileSizeBytes(random.nextLong() & Long.MAX_VALUE % 10_000_000);
        event.getFileDetails().setEncrypted(random.nextBoolean());
        event.getFileDetails().setPermissionsChanged(forceAnomalous || random.nextDouble() < 0.1);
        event.getFileDetails().setAccessDenied(forceAnomalous && random.nextDouble() < 0.3);

        event.getMetadata().setHostname(randomChoice(HOSTNAMES));
        event.getMetadata().setGeoLocation(getLocationForUser(user, forceAnomalous));

        return event;
    }

    /**
     * Generates a process execution event with realistic patterns
     */
    public ProcessEvent generateProcessEvent(boolean forceAnomalous) {
        String user = randomChoice(USERS);
        String sourceIp = getSourceIpForUser(user, forceAnomalous);

        String processName;
        String commandLine;
        double threatScore;

        if (forceAnomalous) {
            processName = randomChoice(SUSPICIOUS_PROCESS_NAMES);
            commandLine = generateCommandLine(processName);
            threatScore = randomThreatScore(0.8, 1.0);
        } else {
            processName = randomChoice(PROCESS_NAMES);
            commandLine = generateCommandLine(processName);
            threatScore = randomThreatScore(0.0, 0.3);
        }

        ProcessEvent event = new ProcessEvent(sourceIp, sourceIp, user, threatScore,
                                             processName, commandLine);

        event.getProcessDetails().setProcessId(random.nextInt(30000) + 1000);
        event.getProcessDetails().setParentProcessId(random.nextInt(1000) + 1);
        event.getProcessDetails().setUserPrivileges(
            randomChoice(new String[]{"system", "admin", "standard"})
        );
        event.getProcessDetails().setSigned(!forceAnomalous);
        if (event.getProcessDetails().isSigned()) {
            event.getProcessDetails().setSigner("Microsoft Corporation");
        }

        event.getMetadata().setHostname(randomChoice(HOSTNAMES));
        event.getMetadata().setGeoLocation(getLocationForUser(user, forceAnomalous));

        return event;
    }

    /**
     * Generates a DNS query event with realistic patterns
     */
    public DnsEvent generateDnsEvent(boolean forceAnomalous) {
        String user = randomChoice(USERS);
        String sourceIp = getSourceIpForUser(user, forceAnomalous);
        String dnsServer = randomChoice(DNS_SERVERS);

        String domain;
        double threatScore;

        if (forceAnomalous) {
            domain = randomChoice(SUSPICIOUS_DOMAINS);
            threatScore = randomThreatScore(0.7, 0.95);
        } else {
            domain = randomChoice(DOMAINS);
            threatScore = randomThreatScore(0.0, 0.2);
        }

        String queryType = randomWeightedChoice(
            new String[]{"A", "AAAA", "MX", "TXT", "CNAME"},
            new int[]{60, 20, 5, 10, 5}
        );

        DnsEvent event = new DnsEvent(sourceIp, dnsServer, user, threatScore, domain, queryType);

        event.getDnsDetails().setResponseCode(
            random.nextDouble() < 0.9 ? "NOERROR" : "NXDOMAIN"
        );

        if ("NOERROR".equals(event.getDnsDetails().getResponseCode())) {
            List<String> resolvedIps = new ArrayList<>();
            resolvedIps.add(randomChoice(EXTERNAL_IPS));
            event.getDnsDetails().setResolvedIps(resolvedIps);
        }

        event.getDnsDetails().setTtl(random.nextInt(3600) + 60);
        event.getDnsDetails().setCached(random.nextBoolean());
        event.getDnsDetails().setDnsServer(dnsServer);

        event.getMetadata().setHostname(randomChoice(HOSTNAMES));
        event.getMetadata().setGeoLocation(getLocationForUser(user, forceAnomalous));

        return event;
    }

    // Helper methods

    private String generateCommandLine(String processName) {
        switch (processName) {
            case "powershell.exe":
                if (random.nextDouble() < 0.3) {
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

    private String generateUserAgent() {
        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
            "curl/7.68.0",
            "Python-urllib/3.8"
        };
        return randomChoice(userAgents);
    }

    private double randomThreatScore(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    private <T> T randomChoice(T[] array) {
        return array[random.nextInt(array.length)];
    }

    private int randomChoice(int[] array) {
        return array[random.nextInt(array.length)];
    }

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

        return choices[0];
    }
}
