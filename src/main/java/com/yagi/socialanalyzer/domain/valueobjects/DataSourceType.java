package com.yagi.socialanalyzer.domain.valueobjects;

/**
 * Data collection method for social media platforms.
 */
public enum DataSourceType {
    /** Official platform API */
    API,
    
    /** Web scraping via Selenium */
    SELENIUM
}
