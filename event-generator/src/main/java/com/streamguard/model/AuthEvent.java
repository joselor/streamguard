package com.streamguard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Represents an authentication attempt event in the security event stream.
 *
 * <p>This event type captures all authentication-related activities including:
 * <ul>
 *   <li>Successful logins (password, MFA, SSO, API keys)</li>
 *   <li>Failed authentication attempts</li>
 *   <li>Blocked authentication attempts (rate limiting, account lockout)</li>
 * </ul>
 *
 * <p>Authentication events are critical for detecting:
 * <ul>
 *   <li>Brute force attacks (multiple failed attempts)</li>
 *   <li>Credential stuffing (failed attempts from unusual locations)</li>
 *   <li>Account takeover (successful login from suspicious IP/location)</li>
 *   <li>Unusual authentication patterns (off-hours, new devices)</li>
 * </ul>
 *
 * <p><b>Threat Score Calculation:</b>
 * Base threat scores are typically:
 * <ul>
 *   <li>Successful auth from known location: 0.0 - 0.1</li>
 *   <li>Failed auth (normal): 0.2 - 0.4</li>
 *   <li>Failed auth (unusual location): 0.5 - 0.7</li>
 *   <li>Multiple failures + success: 0.7 - 0.9</li>
 *   <li>Blocked attempts: 0.8 - 1.0</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * AuthEvent event = new AuthEvent(
 *     "192.168.1.100",     // source IP
 *     "10.0.0.5",          // auth server IP
 *     "john.doe",          // username
 *     0.85,                // threat score (high - suspicious)
 *     "password",          // auth type
 *     "failed"             // status
 * );
 *
 * // Set additional details
 * event.getAuthDetails().setFailureReason("invalid_password");
 * event.getAuthDetails().setAttemptsCount(5);
 * event.getMetadata().setGeoLocation("RU-Moscow");  // Unusual location
 * }</pre>
 *
 * @see Event Base event class for common fields
 * @see AuthDetails Authentication-specific details
 * @author StreamGuard Team
 * @version 1.0
 */
@JsonTypeName("auth_attempt")
public class AuthEvent extends Event {

    @JsonProperty("auth_details")
    private AuthDetails authDetails;

    /**
     * Default constructor for Jackson deserialization.
     * Initializes event_type to "auth_attempt" and creates empty AuthDetails.
     */
    public AuthEvent() {
        super();
        this.setEventType("auth_attempt");
        this.authDetails = new AuthDetails();
    }

    /**
     * Creates an authentication event with essential fields.
     *
     * @param sourceIp Source IP address of the authentication attempt
     * @param destinationIp IP address of the authentication server
     * @param user Username attempting authentication
     * @param threatScore Calculated threat score (0.0 - 1.0)
     * @param authType Type of authentication (password, mfa, sso, api_key)
     * @param status Result of authentication attempt (success, failed, blocked)
     */
    public AuthEvent(String sourceIp, String destinationIp, String user,
                     double threatScore, String authType, String status) {
        super("auth_attempt", sourceIp, destinationIp, user, threatScore);
        this.authDetails = new AuthDetails();
        this.authDetails.setAuthType(authType);
        this.authDetails.setStatus(status);
        this.setStatus(status);  // Set root-level status for C++ compatibility
    }

    public AuthDetails getAuthDetails() {
        return authDetails;
    }

    public void setAuthDetails(AuthDetails authDetails) {
        this.authDetails = authDetails;
    }

    @Override
    public String toString() {
        return "AuthEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", user='" + getUser() + '\'' +
                ", status='" + authDetails.getStatus() + '\'' +
                ", threatScore=" + getThreatScore() +
                '}';
    }

    /**
     * Contains authentication-specific details that extend the base event information.
     *
     * <p>This class encapsulates all authentication-related metadata including:
     * <ul>
     *   <li>Authentication method used (password, MFA, SSO, API key)</li>
     *   <li>Result of the attempt (success, failed, blocked)</li>
     *   <li>Failure diagnostics (if applicable)</li>
     *   <li>Historical context (previous attempts, last success)</li>
     * </ul>
     *
     * <p><b>Authentication Types:</b>
     * <ul>
     *   <li><b>password:</b> Traditional username/password authentication</li>
     *   <li><b>mfa:</b> Multi-factor authentication (2FA, TOTP, SMS)</li>
     *   <li><b>sso:</b> Single sign-on (SAML, OAuth, OIDC)</li>
     *   <li><b>api_key:</b> API key or token-based authentication</li>
     * </ul>
     *
     * <p><b>Failure Reasons:</b>
     * <ul>
     *   <li><b>invalid_password:</b> Incorrect password provided</li>
     *   <li><b>account_locked:</b> Account locked due to security policy</li>
     *   <li><b>mfa_failed:</b> MFA challenge failed</li>
     *   <li><b>expired_credentials:</b> Password or token expired</li>
     *   <li><b>user_not_found:</b> Username doesn't exist</li>
     * </ul>
     */
    public static class AuthDetails {
        @JsonProperty("auth_type")
        private String authType; // password, mfa, sso, api_key

        @JsonProperty("status")
        private String status; // success, failed, blocked

        @JsonProperty("failure_reason")
        private String failureReason; // invalid_password, account_locked, mfa_failed

        @JsonProperty("attempts_count")
        private int attemptsCount;

        @JsonProperty("previous_success_timestamp")
        private Long previousSuccessTimestamp;

        /**
         * Default constructor.
         * Initializes attempts_count to 1 (minimum for any authentication attempt).
         */
        public AuthDetails() {
            this.attemptsCount = 1;
        }

        // Getters and Setters

        /**
         * Gets the authentication type.
         *
         * @return Authentication type (password, mfa, sso, api_key)
         */
        public String getAuthType() {
            return authType;
        }

        public void setAuthType(String authType) {
            this.authType = authType;
        }

        /**
         * Gets the authentication status.
         *
         * @return Status (success, failed, blocked)
         */
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        /**
         * Gets the reason for authentication failure.
         * Only populated when status is "failed" or "blocked".
         *
         * @return Failure reason, or null if authentication succeeded
         */
        public String getFailureReason() {
            return failureReason;
        }

        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }

        /**
         * Gets the number of authentication attempts.
         * This represents attempts in the current session/window.
         *
         * <p>Multiple failed attempts (e.g., 5+) may indicate:
         * <ul>
         *   <li>Brute force attack</li>
         *   <li>Credential stuffing</li>
         *   <li>User forgot password (legitimate)</li>
         * </ul>
         *
         * @return Number of attempts (minimum 1)
         */
        public int getAttemptsCount() {
            return attemptsCount;
        }

        public void setAttemptsCount(int attemptsCount) {
            this.attemptsCount = attemptsCount;
        }

        /**
         * Gets the timestamp of the previous successful authentication.
         * Used to detect unusual authentication patterns.
         *
         * <p>If this is significantly different from expected patterns,
         * it may indicate account compromise.
         *
         * @return Timestamp in milliseconds since epoch, or null if no previous success
         */
        public Long getPreviousSuccessTimestamp() {
            return previousSuccessTimestamp;
        }

        public void setPreviousSuccessTimestamp(Long previousSuccessTimestamp) {
            this.previousSuccessTimestamp = previousSuccessTimestamp;
        }

        @Override
        public String toString() {
            return "AuthDetails{" +
                    "authType='" + authType + '\'' +
                    ", status='" + status + '\'' +
                    ", attemptsCount=" + attemptsCount +
                    '}';
        }
    }
}