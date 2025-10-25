package com.example.feedback.accounting;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.DateTimeException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Bean validation constraint validator that checks period strings for strict MM-YYYY compliance.
 */
public class ValidPeriodValidator implements ConstraintValidator<ValidPeriod, String> {

    private static final DateTimeFormatter PERIOD_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("MM-uuuu")
            .toFormatter(Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return true;
        }

        try {
            YearMonth.parse(trimmed, PERIOD_FORMATTER);
            return true;
        } catch (DateTimeException ex) {
            return false;
        }
    }
}
