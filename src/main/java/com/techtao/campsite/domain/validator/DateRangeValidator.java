package com.techtao.campsite.domain.validator;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDate;

/**
 * This class used to validate if date rages are valid.
 *
 * @author rantao
 */
@Component
public class DateRangeValidator {
    private static int MAX_RESERVE_DAYS = 3;
    private static int VALIDATE_OFFSET_DAYS = 1;

    /**
     * Validate if the given date range is valid.
     * The maximum reservation days is 3 days.
     *
     * @param startFrom the date to start
     * @param endTo the date to be end
     * @return if the total days are less or equal 3 days
     */
    public boolean validateDateRange(LocalDate startFrom, LocalDate endTo) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Assert.notNull(startFrom, "Start date is mandatory");
        Assert.notNull(endTo, "End date is mandatory");

        if (startFrom.isBefore(tomorrow) || endTo.isBefore(tomorrow)) {
            throw new IllegalArgumentException("You need to reserve from tomorrow");
        }

        if (startFrom.isAfter(endTo)) {
            throw new IllegalArgumentException("Start date should before or equal the end date");
        }

        if (startFrom.isAfter(tomorrow.plusMonths(1)) || endTo.isAfter(tomorrow.plusMonths(1))) {
            throw new IllegalArgumentException("You can only reserve dates in one month from tomorrow");
        }

        return startFrom.plusDays(MAX_RESERVE_DAYS).isAfter(endTo.plusDays(VALIDATE_OFFSET_DAYS))
                || startFrom.plusDays(MAX_RESERVE_DAYS).isEqual(endTo.plusDays(VALIDATE_OFFSET_DAYS));
    }

}
