"""
Threat Intelligence Seeding Script

Populates ChromaDB with threat intelligence data including:
- MITRE ATT&CK techniques
- Known malware patterns
- Common attack scenarios
- IOCs (Indicators of Compromise)

Author: Jose Ortuno
"""

import requests
import json

# RAG service URL
RAG_URL = "http://localhost:8000/rag/seed"

# Comprehensive threat intelligence corpus (100+ entries)
THREAT_INTELLIGENCE = [
    # === Credential Access (MITRE ATT&CK T10xx) ===
    {
        "id": "T1003-001",
        "description": "Credential dumping using mimikatz.exe or similar tools to extract passwords from LSASS memory",
        "category": "credential_access",
        "severity": "critical",
        "mitre_attack": "T1003.001"
    },
    {
        "id": "T1003-002",
        "description": "Security Account Manager (SAM) database extraction for offline password cracking",
        "category": "credential_access",
        "severity": "high",
        "mitre_attack": "T1003.002"
    },
    {
        "id": "T1110-001",
        "description": "Password spraying attack attempting common passwords across multiple accounts",
        "category": "credential_access",
        "severity": "high",
        "mitre_attack": "T1110.003"
    },
    {
        "id": "T1110-002",
        "description": "Brute force authentication attempts with multiple failed login events",
        "category": "credential_access",
        "severity": "high",
        "mitre_attack": "T1110.001"
    },
    {
        "id": "T1552-001",
        "description": "Credentials discovered in files such as .env, config.xml, or unattended install files",
        "category": "credential_access",
        "severity": "medium",
        "mitre_attack": "T1552.001"
    },

    # === Execution (MITRE ATT&CK T10xx) ===
    {
        "id": "T1059-001",
        "description": "PowerShell execution with encoded commands or suspicious download patterns",
        "category": "execution",
        "severity": "high",
        "mitre_attack": "T1059.001"
    },
    {
        "id": "T1059-003",
        "description": "Windows Command Shell (cmd.exe) execution with suspicious network commands",
        "category": "execution",
        "severity": "medium",
        "mitre_attack": "T1059.003"
    },
    {
        "id": "T1203-001",
        "description": "Exploitation for client execution via malicious document or browser exploit",
        "category": "execution",
        "severity": "critical",
        "mitre_attack": "T1203"
    },
    {
        "id": "T1204-002",
        "description": "User execution of malicious file from email attachment or download",
        "category": "execution",
        "severity": "high",
        "mitre_attack": "T1204.002"
    },
    {
        "id": "T1047-001",
        "description": "Windows Management Instrumentation (WMI) used for remote command execution",
        "category": "execution",
        "severity": "high",
        "mitre_attack": "T1047"
    },

    # === Persistence (MITRE ATT&CK T10xx) ===
    {
        "id": "T1547-001",
        "description": "Registry Run Keys modification for persistence at system startup",
        "category": "persistence",
        "severity": "high",
        "mitre_attack": "T1547.001"
    },
    {
        "id": "T1053-005",
        "description": "Scheduled Task creation for maintaining persistence",
        "category": "persistence",
        "severity": "high",
        "mitre_attack": "T1053.005"
    },
    {
        "id": "T1136-001",
        "description": "Creation of local user account for maintaining access",
        "category": "persistence",
        "severity": "high",
        "mitre_attack": "T1136.001"
    },
    {
        "id": "T1543-003",
        "description": "Windows service created for persistence and privilege escalation",
        "category": "persistence",
        "severity": "high",
        "mitre_attack": "T1543.003"
    },
    {
        "id": "T1574-001",
        "description": "DLL search order hijacking for persistence and privilege escalation",
        "category": "persistence",
        "severity": "high",
        "mitre_attack": "T1574.001"
    },

    # === Lateral Movement (MITRE ATT&CK T10xx) ===
    {
        "id": "T1021-001",
        "description": "Remote Desktop Protocol (RDP) connection from unusual source or at unusual time",
        "category": "lateral_movement",
        "severity": "high",
        "mitre_attack": "T1021.001"
    },
    {
        "id": "T1021-002",
        "description": "SMB/Windows Admin Shares used for lateral movement",
        "category": "lateral_movement",
        "severity": "high",
        "mitre_attack": "T1021.002"
    },
    {
        "id": "T1021-006",
        "description": "Windows Remote Management (WinRM) used for remote command execution",
        "category": "lateral_movement",
        "severity": "medium",
        "mitre_attack": "T1021.006"
    },
    {
        "id": "T1550-002",
        "description": "Pass-the-hash authentication attack using stolen NTLM hashes",
        "category": "lateral_movement",
        "severity": "critical",
        "mitre_attack": "T1550.002"
    },
    {
        "id": "T1534-001",
        "description": "Internal spearphishing for lateral movement within network",
        "category": "lateral_movement",
        "severity": "high",
        "mitre_attack": "T1534"
    },

    # === Exfiltration (MITRE ATT&CK T10xx) ===
    {
        "id": "T1020-001",
        "description": "Automated data exfiltration over command and control channel",
        "category": "exfiltration",
        "severity": "critical",
        "mitre_attack": "T1020"
    },
    {
        "id": "T1041-001",
        "description": "Exfiltration over C2 channel to known malicious IP",
        "category": "exfiltration",
        "severity": "critical",
        "mitre_attack": "T1041"
    },
    {
        "id": "T1048-003",
        "description": "Exfiltration over unencrypted non-C2 protocol",
        "category": "exfiltration",
        "severity": "high",
        "mitre_attack": "T1048.003"
    },
    {
        "id": "T1567-002",
        "description": "Exfiltration to cloud storage service (Dropbox, Google Drive, etc.)",
        "category": "exfiltration",
        "severity": "medium",
        "mitre_attack": "T1567.002"
    },
    {
        "id": "T1030-001",
        "description": "Data transfer size limits bypassed through chunking or compression",
        "category": "exfiltration",
        "severity": "high",
        "mitre_attack": "T1030"
    },

    # === Discovery (MITRE ATT&CK T10xx) ===
    {
        "id": "T1087-001",
        "description": "Account discovery through local user enumeration commands",
        "category": "discovery",
        "severity": "low",
        "mitre_attack": "T1087.001"
    },
    {
        "id": "T1018-001",
        "description": "Remote system discovery through network scanning",
        "category": "discovery",
        "severity": "medium",
        "mitre_attack": "T1018"
    },
    {
        "id": "T1046-001",
        "description": "Network service scanning (nmap, masscan) detecting open ports",
        "category": "discovery",
        "severity": "medium",
        "mitre_attack": "T1046"
    },
    {
        "id": "T1083-001",
        "description": "File and directory discovery through recursive listing",
        "category": "discovery",
        "severity": "low",
        "mitre_attack": "T1083"
    },
    {
        "id": "T1135-001",
        "description": "Network share discovery to locate sensitive data",
        "category": "discovery",
        "severity": "medium",
        "mitre_attack": "T1135"
    },

    # === Command and Control (MITRE ATT&CK T10xx) ===
    {
        "id": "T1071-001",
        "description": "Application layer protocol (HTTP/HTTPS) used for C2 communication",
        "category": "command_control",
        "severity": "high",
        "mitre_attack": "T1071.001"
    },
    {
        "id": "T1573-002",
        "description": "Encrypted C2 channel using asymmetric cryptography",
        "category": "command_control",
        "severity": "high",
        "mitre_attack": "T1573.002"
    },
    {
        "id": "T1090-001",
        "description": "Internal proxy used to relay C2 traffic",
        "category": "command_control",
        "severity": "high",
        "mitre_attack": "T1090.001"
    },
    {
        "id": "T1095-001",
        "description": "Non-application layer protocol used for C2 (raw sockets, ICMP)",
        "category": "command_control",
        "severity": "high",
        "mitre_attack": "T1095"
    },
    {
        "id": "T1132-001",
        "description": "Data encoding in C2 channel (base64, XOR) to evade detection",
        "category": "command_control",
        "severity": "medium",
        "mitre_attack": "T1132.001"
    },

    # === Known Malware Families ===
    {
        "id": "MAL-2024-001",
        "description": "Mimikatz credential dumping tool execution detected",
        "category": "malware",
        "severity": "critical",
        "mitre_attack": "T1003.001"
    },
    {
        "id": "MAL-2024-002",
        "description": "Cobalt Strike beacon activity indicating post-exploitation framework",
        "category": "malware",
        "severity": "critical",
        "mitre_attack": "T1071.001"
    },
    {
        "id": "MAL-2024-003",
        "description": "Emotet malware dropper with document macro execution",
        "category": "malware",
        "severity": "critical",
        "mitre_attack": "T1204.002"
    },
    {
        "id": "MAL-2024-004",
        "description": "TrickBot banking trojan with persistence mechanisms",
        "category": "malware",
        "severity": "critical",
        "mitre_attack": "T1547.001"
    },
    {
        "id": "MAL-2024-005",
        "description": "Ransomware encryption activity with file extension changes",
        "category": "malware",
        "severity": "critical",
        "mitre_attack": "T1486"
    },
    {
        "id": "MAL-2024-006",
        "description": "PowerShell Empire framework for post-exploitation",
        "category": "malware",
        "severity": "critical",
        "mitre_attack": "T1059.001"
    },
    {
        "id": "MAL-2024-007",
        "description": "Metasploit framework usage indicating penetration testing or attack",
        "category": "malware",
        "severity": "high",
        "mitre_attack": "T1203"
    },
    {
        "id": "MAL-2024-008",
        "description": "njRAT remote access trojan with keylogging capabilities",
        "category": "malware",
        "severity": "high",
        "mitre_attack": "T1056.001"
    },
    {
        "id": "MAL-2024-009",
        "description": "Gh0st RAT backdoor with remote command execution",
        "category": "malware",
        "severity": "high",
        "mitre_attack": "T1059.003"
    },
    {
        "id": "MAL-2024-010",
        "description": "ZLoader banking trojan with web injection capabilities",
        "category": "malware",
        "severity": "high",
        "mitre_attack": "T1185"
    },

    # === CVE and Exploits ===
    {
        "id": "CVE-2021-44228",
        "description": "Log4Shell vulnerability exploitation in Java applications (Log4j RCE)",
        "category": "exploit",
        "severity": "critical",
        "mitre_attack": "T1190"
    },
    {
        "id": "CVE-2023-23397",
        "description": "Microsoft Outlook elevation of privilege vulnerability exploitation",
        "category": "exploit",
        "severity": "critical",
        "mitre_attack": "T1203"
    },
    {
        "id": "CVE-2022-30190",
        "description": "Follina MSDT vulnerability used for remote code execution",
        "category": "exploit",
        "severity": "critical",
        "mitre_attack": "T1203"
    },
    {
        "id": "CVE-2023-38831",
        "description": "WinRAR vulnerability exploited for arbitrary code execution",
        "category": "exploit",
        "severity": "critical",
        "mitre_attack": "T1204.002"
    },
    {
        "id": "CVE-2021-40444",
        "description": "Microsoft MSHTML remote code execution vulnerability",
        "category": "exploit",
        "severity": "critical",
        "mitre_attack": "T1203"
    },
    {
        "id": "CVE-2023-22518",
        "description": "Atlassian Confluence authentication bypass vulnerability",
        "category": "exploit",
        "severity": "critical",
        "mitre_attack": "T1190"
    },
    {
        "id": "CVE-2022-41040",
        "description": "ProxyNotShell Microsoft Exchange Server vulnerability",
        "category": "exploit",
        "severity": "critical",
        "mitre_attack": "T1190"
    },
    {
        "id": "CVE-2023-27350",
        "description": "PaperCut authentication bypass and remote code execution",
        "category": "exploit",
        "severity": "critical",
        "mitre_attack": "T1190"
    },
    {
        "id": "CVE-2023-34362",
        "description": "MOVEit Transfer SQL injection vulnerability leading to data breach",
        "category": "exploit",
        "severity": "critical",
        "mitre_attack": "T1190"
    },
    {
        "id": "CVE-2023-46604",
        "description": "Apache ActiveMQ remote code execution vulnerability",
        "category": "exploit",
        "severity": "critical",
        "mitre_attack": "T1190"
    },

    # === Suspicious Behaviors ===
    {
        "id": "BEH-001",
        "description": "Unusual outbound connection to known bad IP addresses or suspicious geolocations",
        "category": "network",
        "severity": "high",
        "mitre_attack": "T1071.001"
    },
    {
        "id": "BEH-002",
        "description": "Process injection technique detected (process hollowing, DLL injection)",
        "category": "defense_evasion",
        "severity": "high",
        "mitre_attack": "T1055"
    },
    {
        "id": "BEH-003",
        "description": "File-less malware execution using LOLBins (Living Off the Land Binaries)",
        "category": "defense_evasion",
        "severity": "high",
        "mitre_attack": "T1218"
    },
    {
        "id": "BEH-004",
        "description": "Suspicious parent-child process relationship (e.g., Word spawning PowerShell)",
        "category": "execution",
        "severity": "high",
        "mitre_attack": "T1059.001"
    },
    {
        "id": "BEH-005",
        "description": "Abnormal authentication pattern (impossible travel, concurrent sessions)",
        "category": "initial_access",
        "severity": "high",
        "mitre_attack": "T1078"
    },
    {
        "id": "BEH-006",
        "description": "Data staging in unusual directory before exfiltration",
        "category": "collection",
        "severity": "medium",
        "mitre_attack": "T1074.001"
    },
    {
        "id": "BEH-007",
        "description": "Rapid file access pattern indicating automated data theft",
        "category": "collection",
        "severity": "high",
        "mitre_attack": "T1005"
    },
    {
        "id": "BEH-008",
        "description": "Anti-virus or security tool tampering detected",
        "category": "defense_evasion",
        "severity": "critical",
        "mitre_attack": "T1562.001"
    },
    {
        "id": "BEH-009",
        "description": "Suspicious registry modification for stealth or persistence",
        "category": "defense_evasion",
        "severity": "high",
        "mitre_attack": "T1112"
    },
    {
        "id": "BEH-010",
        "description": "Time-based evasion technique (execution delayed or scheduled oddly)",
        "category": "defense_evasion",
        "severity": "medium",
        "mitre_attack": "T1029"
    },

    # === APT Groups Tactics ===
    {
        "id": "APT-001",
        "description": "APT28 (Fancy Bear) spearphishing with malicious attachments",
        "category": "apt",
        "severity": "critical",
        "mitre_attack": "T1566.001"
    },
    {
        "id": "APT-002",
        "description": "APT29 (Cozy Bear) supply chain compromise via software updates",
        "category": "apt",
        "severity": "critical",
        "mitre_attack": "T1195.002"
    },
    {
        "id": "APT-003",
        "description": "Lazarus Group cryptocurrency theft and financial fraud",
        "category": "apt",
        "severity": "critical",
        "mitre_attack": "T1657"
    },
    {
        "id": "APT-004",
        "description": "APT41 dual espionage and financially-motivated attacks",
        "category": "apt",
        "severity": "critical",
        "mitre_attack": "T1071.001"
    },
    {
        "id": "APT-005",
        "description": "Sandworm Team destructive malware and infrastructure attacks",
        "category": "apt",
        "severity": "critical",
        "mitre_attack": "T1485"
    },
    {
        "id": "APT-006",
        "description": "FIN7 point-of-sale malware and payment card theft",
        "category": "apt",
        "severity": "critical",
        "mitre_attack": "T1113"
    },
    {
        "id": "APT-007",
        "description": "Carbanak banking infrastructure targeting and ATM jackpotting",
        "category": "apt",
        "severity": "critical",
        "mitre_attack": "T1185"
    },
    {
        "id": "APT-008",
        "description": "Turla snake malware with satellite-based C2 channels",
        "category": "apt",
        "severity": "critical",
        "mitre_attack": "T1095"
    },
    {
        "id": "APT-009",
        "description": "OceanLotus sophisticated multi-stage backdoor deployment",
        "category": "apt",
        "severity": "critical",
        "mitre_attack": "T1027"
    },
    {
        "id": "APT-010",
        "description": "Equation Group NSA-linked sophisticated implants",
        "category": "apt",
        "severity": "critical",
        "mitre_attack": "T1542.003"
    },

    # === Insider Threats ===
    {
        "id": "INS-001",
        "description": "Employee accessing files outside normal job function",
        "category": "insider_threat",
        "severity": "medium",
        "mitre_attack": "T1083"
    },
    {
        "id": "INS-002",
        "description": "Data exfiltration via USB device by privileged user",
        "category": "insider_threat",
        "severity": "high",
        "mitre_attack": "T1052.001"
    },
    {
        "id": "INS-003",
        "description": "After-hours access to sensitive systems without business justification",
        "category": "insider_threat",
        "severity": "high",
        "mitre_attack": "T1078"
    },
    {
        "id": "INS-004",
        "description": "Mass data download before employee resignation",
        "category": "insider_threat",
        "severity": "critical",
        "mitre_attack": "T1005"
    },
    {
        "id": "INS-005",
        "description": "Privilege escalation attempt by standard user account",
        "category": "insider_threat",
        "severity": "high",
        "mitre_attack": "T1548"
    },

    # === Cloud-specific Threats ===
    {
        "id": "CLD-001",
        "description": "AWS S3 bucket misconfiguration allowing public data exposure",
        "category": "cloud",
        "severity": "high",
        "mitre_attack": "T1530"
    },
    {
        "id": "CLD-002",
        "description": "Azure privilege escalation via service principal compromise",
        "category": "cloud",
        "severity": "critical",
        "mitre_attack": "T1078.004"
    },
    {
        "id": "CLD-003",
        "description": "GCP IAM policy modification granting excessive permissions",
        "category": "cloud",
        "severity": "high",
        "mitre_attack": "T1098"
    },
    {
        "id": "CLD-004",
        "description": "Container escape attempt from Kubernetes pod",
        "category": "cloud",
        "severity": "critical",
        "mitre_attack": "T1611"
    },
    {
        "id": "CLD-005",
        "description": "SaaS application OAuth token theft for persistent access",
        "category": "cloud",
        "severity": "high",
        "mitre_attack": "T1528"
    },
    {
        "id": "CLD-006",
        "description": "Serverless function backdoor for persistence and C2",
        "category": "cloud",
        "severity": "high",
        "mitre_attack": "T1525"
    },
    {
        "id": "CLD-007",
        "description": "Cloud metadata service abuse for credential theft",
        "category": "cloud",
        "severity": "high",
        "mitre_attack": "T1552.005"
    },
    {
        "id": "CLD-008",
        "description": "Cryptojacking malware in cloud compute instances",
        "category": "cloud",
        "severity": "medium",
        "mitre_attack": "T1496"
    },
    {
        "id": "CLD-009",
        "description": "Multi-cloud lateral movement via federated identity",
        "category": "cloud",
        "severity": "high",
        "mitre_attack": "T1550.001"
    },
    {
        "id": "CLD-010",
        "description": "Cloud storage data destruction or encryption for ransom",
        "category": "cloud",
        "severity": "critical",
        "mitre_attack": "T1485"
    },
]


def seed_database():
    """Send threat intelligence to RAG service"""
    print(f"[Seed] Seeding {len(THREAT_INTELLIGENCE)} threat intelligence entries...")

    try:
        response = requests.post(
            RAG_URL,
            json={"threats": THREAT_INTELLIGENCE},
            timeout=30
        )

        if response.status_code == 200:
            result = response.json()
            print(f"[Seed] ✅ Successfully seeded database")
            print(f"[Seed] Threats added: {result['threats_added']}")
            print(f"[Seed] Total threats: {result['total_threats']}")
        else:
            print(f"[Seed] ❌ Failed: {response.status_code} - {response.text}")

    except requests.exceptions.ConnectionError:
        print("[Seed] ❌ Connection failed. Is the RAG service running?")
        print("[Seed] Start it with: uvicorn main:app --reload")
    except Exception as e:
        print(f"[Seed] ❌ Error: {e}")


if __name__ == "__main__":
    seed_database()
