package com.veggo.app.presentation.product;

import com.veggo.app.domain.model.Consultation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ConsultationUiHelper {
    public static final int PREVIEW_LIMIT = 2;

    private ConsultationUiHelper() {}

    /**
     * Shows all consultations so users can reply to each other's questions
     * (including pending ones waiting for admin).
     */
    public static List<Consultation> filterForUser(List<Consultation> source, String customerId) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }
        return sortForDisplay(new ArrayList<>(source));
    }

    public static List<Consultation> limit(List<Consultation> source, int maxItems) {
        if (source == null || source.isEmpty() || maxItems <= 0) {
            return new ArrayList<>();
        }
        if (source.size() <= maxItems) {
            return new ArrayList<>(source);
        }
        return new ArrayList<>(source.subList(0, maxItems));
    }

    private static List<Consultation> sortForDisplay(List<Consultation> source) {
        List<Consultation> sorted = new ArrayList<>(source);
        sorted.sort(Comparator
                .comparing((Consultation item) -> !isAnswered(item))
                .thenComparing(Consultation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return sorted;
    }

    private static boolean isAnswered(Consultation item) {
        return item != null
                && "answered".equalsIgnoreCase(item.getStatus())
                && item.getAnswer() != null
                && !item.getAnswer().trim().isEmpty();
    }
}
