package com.veggo.app.presentation.promotion;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.veggo.app.adapter.VoucherOptionAdapter;
import com.veggo.app.data.remote.dto.PromotionDto;
import com.veggo.app.data.remote.dto.PromotionTargetDto;
import com.veggo.app.data.remote.dto.PromotionUsageDto;
import com.veggo.app.data.remote.dto.ProductDto;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class PromotionVoucherHelper {

    public enum VoucherFilter {
        PRODUCT,
        SHIPPING
    }

    private static final int VOUCHER_LABEL_HIDE_LENGTH = 24;

    private PromotionVoucherHelper() {
    }

    public static void bindVoucherSelectionRow(
            @Nullable TextView labelView,
            @Nullable TextView valueView,
            @Nullable String summary,
            @ColorInt int selectedColor,
            @ColorInt int defaultColor
    ) {
        if (valueView == null) {
            return;
        }
        boolean hasSelection = summary != null
                && !summary.trim().isEmpty()
                && !"Chọn hoặc nhập mã".equals(summary.trim());
        if (!hasSelection) {
            if (labelView != null) {
                labelView.setVisibility(View.VISIBLE);
            }
            valueView.setText("Chọn hoặc nhập mã");
            valueView.setTextColor(defaultColor);
            valueView.setMaxLines(1);
            return;
        }

        valueView.setText(summary);
        valueView.setTextColor(selectedColor);
        boolean hideLabel = summary.length() > VOUCHER_LABEL_HIDE_LENGTH || summary.contains(" + ");
        if (labelView != null) {
            labelView.setVisibility(hideLabel ? View.GONE : View.VISIBLE);
        }
        valueView.setMaxLines(hideLabel ? 2 : 1);
    }

    @Nullable
    public static Long parseDateMillis(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim();
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(pattern, Locale.US);
                if (pattern.endsWith("'Z'")) {
                    formatter.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                }
                Date date = formatter.parse(normalized);
                if (date != null) {
                    return date.getTime();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static boolean isPromotionExpired(@Nullable PromotionDto promotion) {
        if (promotion == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long end = parseDateMillis(promotion.getEndDate());
        return end != null && now > end;
    }

    public static boolean isPromotionNotStarted(@Nullable PromotionDto promotion) {
        if (promotion == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long start = parseDateMillis(promotion.getStartDate());
        return start != null && now < start;
    }

    @Nullable
    public static String evaluateDisabledReason(
            @Nullable PromotionDto promotion,
            @Nullable PromotionTargetDto target,
            @Nullable PromotionUsageDto usage,
            long subtotal,
            @Nullable String customerId,
            boolean matchesTarget
    ) {
        if (promotion == null) {
            return null;
        }

        if (!Boolean.FALSE.equals(promotion.getActive())) {
            if (isPromotionNotStarted(promotion)) {
                return "Chưa đến thời gian bắt đầu";
            }
            if (isPromotionExpired(promotion)) {
                return "Mã đã hết hạn";
            }
        } else {
            return "Mã đã bị vô hiệu hóa";
        }

        boolean skipTargetCheck = VoucherOptionAdapter.isShippingPromotion(promotion);

        double minOrder = promotion.getMinOrderValue() != null ? promotion.getMinOrderValue() : 0;
        if (subtotal < minOrder) {
            return "Đơn tối thiểu " + formatCurrency((long) minOrder);
        }
        if (!skipTargetCheck && !matchesTarget) {
            return "Không áp dụng cho sản phẩm trong giỏ";
        }
        if (promotion.getUsageLimit() != null && promotion.getUsageLimit() > 0 && usage != null
                && usage.getOrderIds() != null
                && usage.getOrderIds().size() >= promotion.getUsageLimit()) {
            return "Mã đã hết lượt sử dụng";
        }
        if (promotion.getUserLimit() != null && promotion.getUserLimit() > 0
                && usage != null && usage.getUserIds() != null && customerId != null) {
            int userUseCount = 0;
            for (String userId : usage.getUserIds()) {
                if (customerId.equals(userId)) {
                    userUseCount++;
                }
            }
            if (userUseCount >= promotion.getUserLimit()) {
                return "Bạn đã sử dụng mã này";
            }
        }
        return null;
    }

    public static String voucherTitle(@Nullable PromotionDto promotion) {
        if (promotion == null) {
            return "Khuyến mãi";
        }
        if (promotion.getName() != null && !promotion.getName().trim().isEmpty()) {
            return promotion.getName();
        }
        if (promotion.getCode() != null && !promotion.getCode().trim().isEmpty()) {
            return promotion.getCode();
        }
        return promotion.getPromotionId() != null ? promotion.getPromotionId() : "Khuyến mãi";
    }

    public static String voucherCondition(
            @Nullable PromotionDto promotion,
            @Nullable PromotionTargetDto target,
            @Nullable String disabledReason
    ) {
        if (promotion == null) {
            return "Khuyến mãi";
        }
        StringBuilder text = new StringBuilder();
        Double discountValue = promotion.getDiscountValue();
        if (discountValue != null && discountValue > 0) {
            boolean fixed = "fixed".equalsIgnoreCase(promotion.getDiscountType());
            text.append(fixed
                    ? "Giảm " + formatCurrency(discountValue.longValue())
                    : "Giảm " + trimTrailingZeros(discountValue) + "%");
        } else if (VoucherOptionAdapter.isShippingPromotion(promotion)) {
            text.append("Miễn phí vận chuyển");
        } else {
            text.append(promotion.getDescription() != null ? promotion.getDescription() : "Khuyến mãi");
        }
        if (promotion.getMinOrderValue() != null && promotion.getMinOrderValue() > 0) {
            text.append(" | Đơn từ ").append(formatCurrency(promotion.getMinOrderValue().longValue()));
        }
        String targetLabel = formatTargetTypeLabel(target);
        if (targetLabel != null && !targetLabel.isEmpty()) {
            text.append(" | Áp dụng: ").append(targetLabel);
        }
        if (disabledReason != null) {
            text.append(" | ").append(disabledReason);
        }
        return text.toString();
    }

    public static String voucherExpiry(@Nullable PromotionDto promotion) {
        if (promotion == null || promotion.getEndDate() == null || promotion.getEndDate().trim().isEmpty()) {
            return "Không giới hạn thời gian";
        }
        return "HSD: " + promotion.getEndDate().substring(0, Math.min(10, promotion.getEndDate().length()));
    }

    @Nullable
    public static String formatTargetTypeLabel(@Nullable PromotionTargetDto target) {
        if (target == null) {
            return null;
        }
        if (target.getTargetGroups() != null && !target.getTargetGroups().isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (PromotionTargetDto.TargetGroupDto group : target.getTargetGroups()) {
                String label = formatTargetType(group.getTargetType());
                if (label == null || label.isEmpty()) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(label);
            }
            return builder.length() > 0 ? builder.toString() : null;
        }
        return formatTargetType(target.getTargetType());
    }

    @Nullable
    public static String formatTargetType(@Nullable String targetType) {
        if (targetType == null || targetType.trim().isEmpty()) {
            return null;
        }
        switch (targetType.trim().toLowerCase(Locale.US)) {
            case "product":
                return "Sản phẩm";
            case "category":
                return "Danh mục";
            case "subcategory":
                return "Danh mục con";
            case "brand":
                return "Thương hiệu";
            case "user":
                return "Người dùng";
            case "shipping":
                return "Vận chuyển";
            case "order":
                return "Đơn hàng";
            default:
                return targetType;
        }
    }

    public static String formatCurrency(long amount) {
        return String.format(Locale.US, "%,d", amount).replace(',', '.') + "đ";
    }

    private static String trimTrailingZeros(double value) {
        String text = String.format(Locale.US, "%.3f", value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    public static boolean matchesTargetRef(@Nullable String targetType, @Nullable List<String> targetRefs,
                                         @Nullable String sku, @Nullable ProductDto product) {
        if (targetType == null || targetRefs == null || targetRefs.isEmpty()) {
            return true;
        }
        if ("User".equalsIgnoreCase(targetType)
                || "Shipping".equalsIgnoreCase(targetType)
                || "Order".equalsIgnoreCase(targetType)) {
            return true;
        }
        for (String ref : targetRefs) {
            if (ref == null || ref.trim().isEmpty()) {
                continue;
            }
            String normalizedRef = ref.trim();
            if ("Product".equalsIgnoreCase(targetType)) {
                if (sku != null && normalizedRef.equalsIgnoreCase(sku.trim())) {
                    return true;
                }
                if (product != null && product.getSku() != null
                        && normalizedRef.equalsIgnoreCase(product.getSku().trim())) {
                    return true;
                }
            } else if (product != null) {
                String value = null;
                if ("Category".equalsIgnoreCase(targetType)) {
                    value = product.getCategoryId();
                } else if ("Subcategory".equalsIgnoreCase(targetType)) {
                    value = product.getSubcategoryId();
                } else if ("Brand".equalsIgnoreCase(targetType)) {
                    value = product.getBrand();
                }
                if (value != null && normalizedRef.equalsIgnoreCase(value.trim())) {
                    return true;
                }
            }
        }
        return false;
    }
}
