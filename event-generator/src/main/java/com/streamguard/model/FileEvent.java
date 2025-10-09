package com.streamguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Represents a file system access event in the security event stream.
 *
 * <p>This event type captures all file system operations including:
 * <ul>
 *   <li>File reads (opening, viewing)</li>
 *   <li>File writes (creating, modifying, appending)</li>
 *   <li>File deletions (removing files)</li>
 *   <li>Permission changes (chmod, ACL modifications)</li>
 *   <li>File movements and copies</li>
 * </ul>
 *
 * <p>File access events are critical for detecting:
 * <ul>
 *   <li>Data theft (unauthorized access to sensitive files)</li>
 *   <li>Ransomware activity (mass file encryption/modification)</li>
 *   <li>Insider threats (employee accessing confidential data)</li>
 *   <li>Privilege escalation (accessing system files)</li>
 *   <li>Data destruction (mass deletions)</li>
 *   <li>Compliance violations (PII/PCI data access)</li>
 * </ul>
 *
 * <p><b>Threat Score Calculation:</b>
 * Base threat scores are typically:
 * <ul>
 *   <li>Normal file access (user's own documents): 0.0 - 0.1</li>
 *   <li>Access to shared files: 0.1 - 0.3</li>
 *   <li>Access to sensitive directories: 0.4 - 0.6</li>
 *   <li>Access by contractor/temp user: 0.5 - 0.7</li>
 *   <li>Access to highly confidential data: 0.7 - 0.9</li>
 *   <li>Mass file operations (ransomware pattern): 0.9 - 1.0</li>
 *   <li>Access denied (attempted unauthorized access): 0.8 - 1.0</li>
 * </ul>
 *
 * <p><b>High-Risk File Patterns:</b>
 * <ul>
 *   <li>System files: /etc/passwd, /etc/shadow, SAM registry</li>
 *   <li>Credentials: .ssh/id_rsa, .aws/credentials, browser password stores</li>
 *   <li>Financial data: payroll*.xlsx, invoice*.pdf, accounting*</li>
 *   <li>Personal data: SSN, credit cards, health records</li>
 *   <li>Source code: proprietary algorithms, API keys in code</li>
 * </ul>
 *
 * <p><b>Suspicious Operations:</b>
 * <ul>
 *   <li>Reading large numbers of files in short time (data harvesting)</li>
 *   <li>Creating encrypted archives (preparing for exfiltration)</li>
 *   <li>Modifying file extensions (ransomware behavior)</li>
 *   <li>Accessing files outside normal working hours</li>
 *   <li>Contractor accessing HR/financial data</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * FileEvent event = new FileEvent(
 *     "192.168.1.105",                              // source IP
 *     "10.0.0.20",                                  // file server IP
 *     "contractor_temp",                            // suspicious user
 *     0.75,                                         // high threat score
 *     "/var/data/confidential/payroll_2024.xlsx",  // sensitive file
 *     "read"                                        // operation
 * );
 *
 * // Set file details
 * event.getFileDetails().setFileSizeBytes(524288);  // 512 KB
 * event.getFileDetails().setFileHash("sha256:a3b2c1...");
 * event.getFileDetails().setEncrypted(false);  // Unencrypted sensitive data!
 * event.getMetadata().setHostname("contractor-laptop");
 * }</pre>
 *
 * @see Event Base event class for common fields
 * @see FileDetails File operation-specific details
 * @author Jose Ortuno
 * @version 1.0
 */
@JsonTypeName("file_access")
public class FileEvent extends Event {

    @JsonProperty("file_details")
    private FileDetails fileDetails;

    /**
     * Default constructor for Jackson deserialization.
     * Initializes event_type to "file_access" and creates empty FileDetails.
     */
    public FileEvent() {
        super();
        this.setEventType("file_access");
        this.fileDetails = new FileDetails();
    }

    /**
     * Creates a file access event with essential fields.
     *
     * @param sourceIp Source IP address (where the access originated)
     * @param destinationIp IP address of the file server/storage
     * @param user Username performing the file operation
     * @param threatScore Calculated threat score (0.0 - 1.0)
     * @param filePath Full path to the file being accessed
     * @param operation Type of operation (read, write, delete, create, modify)
     */
    public FileEvent(String sourceIp, String destinationIp, String user,
                    double threatScore, String filePath, String operation) {
        super("file_access", sourceIp, destinationIp, user, threatScore);
        this.fileDetails = new FileDetails();
        this.fileDetails.setFilePath(filePath);
        this.fileDetails.setOperation(operation);
    }

    public FileDetails getFileDetails() {
        return fileDetails;
    }

    public void setFileDetails(FileDetails fileDetails) {
        this.fileDetails = fileDetails;
    }

    @Override
    public String toString() {
        return "FileEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", user='" + getUser() + '\'' +
                ", operation='" + fileDetails.getOperation() + '\'' +
                ", filePath='" + fileDetails.getFilePath() + '\'' +
                '}';
    }

    /**
     * Contains file operation-specific details that extend the base event information.
     *
     * <p>This class encapsulates all file-related metadata including:
     * <ul>
     *   <li>File location and path information</li>
     *   <li>Type of operation performed</li>
     *   <li>File characteristics (size, hash, encryption status)</li>
     *   <li>Permission and access control changes</li>
     *   <li>Access denial information</li>
     * </ul>
     *
     * <p><b>Operation Types:</b>
     * <ul>
     *   <li><b>read:</b> File opened for reading (view, copy source)</li>
     *   <li><b>write:</b> File written to (save, append)</li>
     *   <li><b>delete:</b> File removed from filesystem</li>
     *   <li><b>create:</b> New file created</li>
     *   <li><b>modify:</b> Existing file modified (metadata, content, permissions)</li>
     * </ul>
     *
     * <p><b>Ransomware Detection Indicators:</b>
     * <ul>
     *   <li>High volume of modify operations (100+ files in < 1 minute)</li>
     *   <li>File extension changes (*.doc → *.encrypted)</li>
     *   <li>Creation of ransom notes (README_DECRYPT.txt)</li>
     *   <li>Encryption of previously unencrypted files</li>
     * </ul>
     *
     * <p><b>Data Exfiltration Indicators:</b>
     * <ul>
     *   <li>Read operations on many sensitive files</li>
     *   <li>Creation of large archives (.zip, .tar.gz)</li>
     *   <li>Access from unusual accounts or locations</li>
     *   <li>Off-hours access to financial/HR data</li>
     * </ul>
     */
    public static class FileDetails {
        @JsonProperty("file_path")
        private String filePath;

        @JsonProperty("operation")
        private String operation; // read, write, delete, create, modify

        @JsonProperty("file_size_bytes")
        private long fileSizeBytes;

        @JsonProperty("file_hash")
        private String fileHash;

        @JsonProperty("is_encrypted")
        private boolean isEncrypted;

        @JsonProperty("permissions_changed")
        private boolean permissionsChanged;

        @JsonProperty("access_denied")
        private boolean accessDenied;

        /**
         * Default constructor.
         * Initializes boolean fields to false and numeric fields to 0.
         */
        public FileDetails() {}

        // Getters and Setters

        /**
         * Gets the full file path.
         *
         * <p>Path format depends on operating system:
         * <ul>
         *   <li>Windows: C:\Users\john\Documents\file.txt</li>
         *   <li>Linux/Mac: /home/john/documents/file.txt</li>
         *   <li>Network: \\server\share\folder\file.txt</li>
         * </ul>
         *
         * @return Full path to the file
         */
        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        /**
         * Gets the file operation type.
         *
         * @return Operation (read, write, delete, create, modify)
         */
        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        /**
         * Gets the file size in bytes.
         *
         * <p>Large files may indicate:
         * <ul>
         *   <li>Database dumps</li>
         *   <li>Video/media files</li>
         *   <li>Backup archives</li>
         *   <li>Complete directory zips (potential exfiltration)</li>
         * </ul>
         *
         * @return File size in bytes (0 if not measured or file doesn't exist)
         */
        public long getFileSizeBytes() {
            return fileSizeBytes;
        }

        public void setFileSizeBytes(long fileSizeBytes) {
            this.fileSizeBytes = fileSizeBytes;
        }

        /**
         * Gets the cryptographic hash of the file.
         * Typically SHA-256 format: "sha256:abc123..."
         *
         * <p>File hashes are used for:
         * <ul>
         *   <li>Malware identification (compare against known bad hashes)</li>
         *   <li>Data loss prevention (detect sensitive document movement)</li>
         *   <li>Change detection (verify file hasn't been tampered with)</li>
         *   <li>Deduplication (identify duplicate files)</li>
         * </ul>
         *
         * @return File hash, or null if not calculated
         */
        public String getFileHash() {
            return fileHash;
        }

        public void setFileHash(String fileHash) {
            this.fileHash = fileHash;
        }

        /**
         * Indicates whether the file is encrypted.
         *
         * <p>Encryption status helps identify:
         * <ul>
         *   <li>Ransomware activity (sudden encryption of many files)</li>
         *   <li>Proper data protection (sensitive files should be encrypted)</li>
         *   <li>Compliance violations (unencrypted PII/PCI data)</li>
         * </ul>
         *
         * @return true if file is encrypted, false otherwise
         */
        public boolean isEncrypted() {
            return isEncrypted;
        }

        public void setEncrypted(boolean encrypted) {
            isEncrypted = encrypted;
        }

        /**
         * Indicates whether file permissions were modified.
         *
         * <p>Permission changes may indicate:
         * <ul>
         *   <li>Privilege escalation attempts (making file world-writable)</li>
         *   <li>Hiding activity (removing read permissions)</li>
         *   <li>Legitimate admin activity (chmod, ACL updates)</li>
         * </ul>
         *
         * @return true if permissions were changed during this operation
         */
        public boolean isPermissionsChanged() {
            return permissionsChanged;
        }

        public void setPermissionsChanged(boolean permissionsChanged) {
            this.permissionsChanged = permissionsChanged;
        }

        /**
         * Indicates whether access was denied.
         *
         * <p>Access denials are important security indicators:
         * <ul>
         *   <li>Unauthorized access attempts (should investigate user's intent)</li>
         *   <li>Misconfigured permissions (legitimate users blocked)</li>
         *   <li>Lateral movement attempts (attacker probing)</li>
         *   <li>Insider threat (employee trying to access restricted data)</li>
         * </ul>
         *
         * <p>Multiple access denials from same user may indicate:
         * <ul>
         *   <li>Automated attack/scanning</li>
         *   <li>Compromised credentials</li>
         *   <li>Malicious insider</li>
         * </ul>
         *
         * @return true if access was denied, false if operation succeeded
         */
        public boolean isAccessDenied() {
            return accessDenied;
        }

        public void setAccessDenied(boolean accessDenied) {
            this.accessDenied = accessDenied;
        }
    }
}
