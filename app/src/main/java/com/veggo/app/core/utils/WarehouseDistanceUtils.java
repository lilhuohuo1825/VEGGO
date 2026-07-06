package com.veggo.app.core.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veggo.app.data.remote.dto.WarehouseDto;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class WarehouseDistanceUtils {
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double AVERAGE_SPEED_KMH = 35.0;

    private WarehouseDistanceUtils() {
    }

    public static class RankedWarehouse {
        public final WarehouseDto warehouse;
        public final double distanceKm;
        public final int etaMinutes;
        public final boolean nearest;

        public RankedWarehouse(
                WarehouseDto warehouse,
                double distanceKm,
                int etaMinutes,
                boolean nearest
        ) {
            this.warehouse = warehouse;
            this.distanceKm = distanceKm;
            this.etaMinutes = etaMinutes;
            this.nearest = nearest;
        }
    }

    @NonNull
    public static List<RankedWarehouse> rankByDeliveryAddress(
            @Nullable List<WarehouseDto> warehouses,
            @Nullable String deliveryAddress
    ) {
        if (warehouses == null || warehouses.isEmpty()) {
            return Collections.emptyList();
        }
        double[] destination = resolveCoordinates(deliveryAddress);
        List<RankedWarehouse> ranked = new ArrayList<>();
        for (WarehouseDto warehouse : warehouses) {
            if (warehouse == null || !warehouse.isActive()) {
                continue;
            }
            WarehouseDto.Location location = warehouse.getLocation();
            if (location == null) {
                continue;
            }
            double distanceKm = distanceKm(
                    destination[0], destination[1],
                    location.lat, location.lng
            );
            ranked.add(new RankedWarehouse(
                    warehouse,
                    distanceKm,
                    estimateMinutes(distanceKm),
                    false
            ));
        }
        ranked.sort(Comparator.comparingDouble(left -> left.distanceKm));
        if (!ranked.isEmpty()) {
            RankedWarehouse first = ranked.get(0);
            ranked.set(0, new RankedWarehouse(
                    first.warehouse,
                    first.distanceKm,
                    first.etaMinutes,
                    true
            ));
        }
        return ranked;
    }

    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public static int estimateMinutes(double distanceKm) {
        int minutes = (int) Math.round((distanceKm / AVERAGE_SPEED_KMH) * 60.0);
        return Math.max(15, minutes);
    }

    @NonNull
    public static String formatDistanceEta(double distanceKm, int etaMinutes) {
        int km = (int) Math.round(distanceKm);
        if (km <= 0) {
            km = 1;
        }
        return km + " km ~ " + etaMinutes + " phút";
    }

    @NonNull
    public static double[] resolveCoordinates(@Nullable String deliveryAddress) {
        String normalized = normalize(deliveryAddress == null ? "" : deliveryAddress);
        if (containsAny(normalized, "binh duong", "binhduong", "di an", "dian", "thu dau mot")) {
            return new double[]{11.0062, 106.6530};
        }
        if (containsAny(normalized, "ho chi minh", "hcm", "tp hcm", "sai gon", "saigon", "quan 1", "quan 7", "tan phong")) {
            return new double[]{10.7769, 106.7009};
        }
        if (containsAny(normalized, "ha noi", "hanoi", "cau giay", "long bien", "hoan kiem")) {
            return new double[]{21.0285, 105.8542};
        }
        if (containsAny(normalized, "da nang", "danang", "hai chau")) {
            return new double[]{16.0544, 108.2022};
        }
        if (containsAny(normalized, "can tho", "cantho", "ninh kieu")) {
            return new double[]{10.0289, 105.7725};
        }
        if (containsAny(normalized, "hai phong", "haiphong", "ngo quyen")) {
            return new double[]{20.8468, 106.6811};
        }
        if (containsAny(normalized, "nghe an", "nghean", "vinh")) {
            return new double[]{18.6734, 105.6816};
        }
        if (containsAny(normalized, "nha trang", "khanh hoa", "loc tho")) {
            return new double[]{12.2388, 109.1964};
        }
        if (containsAny(normalized, "buon ma thuot", "dak lak", "daklak")) {
            return new double[]{12.6869, 108.0544};
        }
        return new double[]{10.7769, 106.7009};
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        String lower = value.toLowerCase(Locale.US).trim();
        String withoutAccent = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return withoutAccent.replace('đ', 'd');
    }
}
