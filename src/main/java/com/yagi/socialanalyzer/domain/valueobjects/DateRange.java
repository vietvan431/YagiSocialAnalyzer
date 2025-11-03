package com.yagi.socialanalyzer.domain.valueobjects;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Value object encapsulating a date range with validation.
 * Ensures start date is before or equal to end date and range doesn't exceed 365 days.
 */
public record DateRange(LocalDate start, LocalDate end) {
    
    public DateRange {
        Objects.requireNonNull(start, "Start date cannot be null");
        Objects.requireNonNull(end, "End date cannot be null");
        
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                "Start date must be before or equal to end date");
        }
        
        long daysBetween = ChronoUnit.DAYS.between(start, end);
        if (daysBetween > 365) {
            throw new IllegalArgumentException(
                "Date range cannot exceed 365 days (current: " + daysBetween + " days)");
        }
    }
    
    /**
     * Check if a date falls within this range (inclusive).
     *
     * @param date the date to check
     * @return true if date is within range
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
    
    /**
     * Get the number of days in this range.
     *
     * @return number of days (inclusive)
     */
    public long getDays() {
        return ChronoUnit.DAYS.between(start, end) + 1;
    }
}
