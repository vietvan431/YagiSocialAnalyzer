package com.yagi.socialanalyzer.domain.valueobjects;

/**
 * Authentication methods for platform access.
 */
public enum AuthenticationType {
    /** No authentication required */
    NONE,
    
    /** Single API key */
    API_KEY,
    
    /** OAuth 2.0 flow */
    OAUTH2,
    
    /** Username/password via Selenium */
    SESSION_LOGIN
}
