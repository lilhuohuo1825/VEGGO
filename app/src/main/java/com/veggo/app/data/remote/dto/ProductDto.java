package com.veggo.app.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO ánh xạ trực tiếp từ response của /api/products/home
 * Các field đặt theo đúng tên field trong MongoDB collection 'products'.
 */
public class ProductDto {
    @SerializedName(value = "_id", alternate = {"id"})
    private String id;

    /** DB dùng product_name, fallback về "name" cho API cũ */
    @SerializedName(value = "product_name", alternate = {"name"})
    private String name;

    private String brand;
    private String sku;
    private String unit;
    private String weight;
    private String origin;
    private String status;

    /** Giá bán */
    private long price;

    /** Giá gốc trước giảm */
    @SerializedName("base_price")
    private long originalPrice;

    /** Danh sách ảnh – lấy ảnh đầu tiên để hiển thị */
    private List<String> image;

    private float rating;

    @SerializedName("purchase_count")
    private int soldCount;

    private int liked;
    private int stock;

    private Boolean active;

    @SerializedName("CategoryID")
    private String categoryId;

    @SerializedName("SubcategoryID")
    private String subcategoryId;

    // ─── Getters & Setters ───────────────────────────────────────────────

    public String getId()              { return id; }
    public void setId(String id)       { this.id = id; }

    public String getName()            { return name; }
    public void setName(String name)   { this.name = name; }

    public String getBrand()           { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getSku()             { return sku; }
    public void setSku(String sku)     { this.sku = sku; }

    public String getUnit()            { return unit; }
    public void setUnit(String unit)   { this.unit = unit; }

    public String getWeight()          { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getOrigin()          { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getStatus()          { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getPrice()             { return price; }
    public void setPrice(long price)   { this.price = price; }

    public long getOriginalPrice()     { return originalPrice; }
    public void setOriginalPrice(long p) { this.originalPrice = p; }

    /** Trả về URL ảnh đầu tiên trong mảng image (nếu có) */
    public String getImageUrl() {
        if (image != null && !image.isEmpty()) return image.get(0);
        return null;
    }

    public List<String> getImage()     { return image; }
    public void setImage(List<String> image) { this.image = image; }

    public void setImageUrl(String url) {
        if (this.image == null) {
            this.image = new ArrayList<>();
        }
        this.image.clear();
        this.image.add(url);
    }

    public float getRating()           { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getSoldCount()          { return soldCount; }
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }

    public int getLiked()              { return liked; }
    public void setLiked(int liked)    { this.liked = liked; }

    public int getStock()              { return stock; }
    public void setStock(int stock)    { this.stock = stock; }

    public String getCategoryId()      { return categoryId; }
    public void setCategoryId(String id) { this.categoryId = id; }

    public String getSubcategoryId()   { return subcategoryId; }
    public void setSubcategoryId(String id) { this.subcategoryId = id; }

    // ─── Compat getters (giữ tương thích với code cũ dùng ProductMapper) ───

    /** Compat cho ProductMapper.fromDto() */
    public String getDescription()     { return null; }
    public String getCondition()       { return null; }
    public String getFatContent()      { return null; }
    public int getReviewCount()        { return 0; }
    public Boolean getActive() {
        if (active != null) return active;
        return status == null ? null : "Active".equals(status);
    }
    public void setActive(Boolean active) { this.active = active; }
}
