package com.veggo.app.core.utils;

import com.veggo.app.domain.model.Product;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class TextSearchUtils {
    private static final Map<Character, Character> VIETNAMESE_DIACRITICS = new HashMap<>();

    static {
        putDiacritics('a', 'à', 'á', 'ả', 'ã', 'ạ', 'ă', 'ằ', 'ắ', 'ẳ', 'ẵ', 'ặ', 'â', 'ầ', 'ấ', 'ẩ', 'ẫ', 'ậ');
        putDiacritics('e', 'è', 'é', 'ẻ', 'ẽ', 'ẹ', 'ê', 'ề', 'ế', 'ể', 'ễ', 'ệ');
        putDiacritics('i', 'ì', 'í', 'ỉ', 'ĩ', 'ị');
        putDiacritics('o', 'ò', 'ó', 'ỏ', 'õ', 'ọ', 'ô', 'ồ', 'ố', 'ổ', 'ỗ', 'ộ', 'ơ', 'ờ', 'ớ', 'ở', 'ỡ', 'ợ');
        putDiacritics('u', 'ù', 'ú', 'ủ', 'ũ', 'ụ', 'ư', 'ừ', 'ứ', 'ử', 'ữ', 'ự');
        putDiacritics('y', 'ỳ', 'ý', 'ỷ', 'ỹ', 'ỵ');
        VIETNAMESE_DIACRITICS.put('đ', 'd');
        VIETNAMESE_DIACRITICS.put('Đ', 'd');
    }

    private TextSearchUtils() {
    }

    private static void putDiacritics(char base, char... variants) {
        for (char variant : variants) {
            VIETNAMESE_DIACRITICS.put(variant, base);
            VIETNAMESE_DIACRITICS.put(Character.toUpperCase(variant), base);
        }
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(text.length());
        String lower = text.toLowerCase(Locale.ROOT);
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            Character mapped = VIETNAMESE_DIACRITICS.get(ch);
            builder.append(mapped != null ? mapped : ch);
        }
        return builder.toString().trim();
    }

    public static boolean matches(String haystack, String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return true;
        }

        String normalizedHaystack = normalize(haystack);
        String[] tokens = normalizedQuery.split("\\s+");
        boolean matchedToken = false;
        for (String token : tokens) {
            if (token.length() < 2) {
                continue;
            }
            matchedToken = true;
            if (!normalizedHaystack.contains(token)) {
                return false;
            }
        }
        return matchedToken || normalizedHaystack.contains(normalizedQuery);
    }

    public static boolean matchesProduct(Product product, String query) {
        if (product == null) {
            return false;
        }
        StringBuilder searchable = new StringBuilder();
        append(searchable, product.getName());
        append(searchable, product.getSku());
        append(searchable, product.getBrand());
        append(searchable, product.getDescription());
        append(searchable, product.getIngredients());
        return matches(searchable.toString(), query);
    }

    private static void append(StringBuilder builder, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(value.trim());
    }
}
