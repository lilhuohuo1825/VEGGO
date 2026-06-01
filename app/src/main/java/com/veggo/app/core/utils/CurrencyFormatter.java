package com.veggo.app.core.utils;

import java.text.NumberFormat;
import java.util.Locale;

public final class CurrencyFormatter {
    private CurrencyFormatter() {
    }

    public static String formatVnd(long amount) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(amount);
    }
}
