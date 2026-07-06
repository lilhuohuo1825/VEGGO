package com.veggo.app.presentation.product;

import com.veggo.app.domain.model.Consultation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ConsultationUiHelper {
    public static final int PREVIEW_LIMIT = 2;

    private ConsultationUiHelper() {}

    public static List<Consultation> filterForUser(List<Consultation> source, String customerId) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }
        List<Consultation> filtered = new ArrayList<>();
        for (Consultation item : source) {
            if (isAnswered(item)) {
                filtered.add(item);
                continue;
            }
            if (isPending(item) && customerId != null && !customerId.isEmpty()
                    && customerId.equals(item.getCustomerId())) {
                filtered.add(item);
            }
        }
        return sortForDisplay(filtered);
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

    private static boolean isPending(Consultation item) {
        return item != null && "pending".equalsIgnoreCase(item.getStatus());
    }
}
