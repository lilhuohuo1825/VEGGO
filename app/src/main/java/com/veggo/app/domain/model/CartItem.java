package com.veggo.app.domain.model;

public class CartItem {
    private final Product product;
    private int quantity;
    private final String selectedWeight;

    public CartItem(Product product, int quantity, double selectedWeight) {
        this.product = product;
        this.quantity = quantity;
        this.selectedWeight = formatWeight(selectedWeight);
    }

    private String formatWeight(double weight) {
        if (weight >= 1.0) {
            java.util.Locale locale = java.util.Locale.US;
            if (Math.abs(weight - Math.round(weight)) < 0.0001) {
                return String.format(locale, "%.0fkg", weight);
            }
            return String.format(locale, "%skg", trimTrailingZeros(weight));
        }
        int grams = (int) Math.round(weight * 1000d);
        return grams + "g";
    }

    private String trimTrailingZeros(double value) {
        String text = String.format(java.util.Locale.US, "%.3f", value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getSelectedWeight() { return selectedWeight; }

    // Tính carbon point giả định (ví dụ 33.1 như trong yêu cầu)
    public double getCarbonSavingPoint() {
        return product.getCarbonSavingPoint();
    }
}
