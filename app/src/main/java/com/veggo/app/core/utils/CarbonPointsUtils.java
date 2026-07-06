package com.veggo.app.core.utils;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cùng công thức với CheckoutActivity và backend certificateService:
 * - Có EmissionFactor: kg * emissionFactor * quantity → điểm = emission * 10
 * - Không có: (carbonSavingPoint * quantity) / 10 → điểm = carbonSavingPoint * quantity
 */
public final class CarbonPointsUtils {

    private static final Pattern WEIGHT_PATTERN = Pattern.compile(
            "(\\d+(?:[\\.,]\\d+)?)\\s*(kg|kilogram|g|gr|gram)",
            Pattern.CASE_INSENSITIVE
    );

    private CarbonPointsUtils() {
    }

    public static double calculateEmission(
            double carbonSavingPoint,
            double emissionFactor,
            int quantity,
            double selectedWeight,
            boolean hasWeightOptions,
            @Nullable String weightLabel
    ) {
        if (quantity <= 0) {
            return 0;
        }
        if (emissionFactor > 0) {
            double kilograms = resolveKilograms(selectedWeight, hasWeightOptions, weightLabel);
            if (kilograms <= 0) {
                return 0;
            }
            return kilograms * emissionFactor * quantity;
        }
        if (carbonSavingPoint > 0) {
            return (carbonSavingPoint * quantity) / 10.0;
        }
        return 0;
    }

    public static double calculatePoints(
            double carbonSavingPoint,
            double emissionFactor,
            int quantity,
            double selectedWeight,
            boolean hasWeightOptions,
            @Nullable String weightLabel
    ) {
        return calculateEmission(
                carbonSavingPoint,
                emissionFactor,
                quantity,
                selectedWeight,
                hasWeightOptions,
                weightLabel
        ) * 10.0;
    }

    public static double resolveKilograms(
            double selectedWeight,
            boolean hasWeightOptions,
            @Nullable String weightLabel
    ) {
        if (hasWeightOptions && selectedWeight > 0) {
            return selectedWeight;
        }
        double parsed = parseWeightInKilograms(weightLabel);
        return parsed > 0 ? parsed : 1.0;
    }

    public static double parseWeightInKilograms(@Nullable String value) {
        String text = value == null ? "" : value.trim().toLowerCase(Locale.US);
        if (text.isEmpty()) {
            return 0;
        }
        Matcher matcher = WEIGHT_PATTERN.matcher(text);
        if (!matcher.find()) {
            return 0;
        }
        try {
            double amount = Double.parseDouble(matcher.group(1).replace(',', '.'));
            String unit = matcher.group(2).toLowerCase(Locale.US);
            return unit.startsWith("g") ? amount / 1000.0 : amount;
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
