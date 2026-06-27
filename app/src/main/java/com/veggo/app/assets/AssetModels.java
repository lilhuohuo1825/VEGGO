package com.veggo.app.assets;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public final class AssetModels {
    private AssetModels() {
    }

    public static class MongoDate {
        @SerializedName("$date")
        public String date;
    }

    public static class MongoId {
        @SerializedName("$oid")
        public String oid;
    }

    public static class Admin {
        @SerializedName("_id")
        public String objectId;
        public String id;
        public String name;
        public String email;
        public String password;
        @SerializedName("updated_at")
        public String updatedAt;
    }

    public static class Blog {
        @SerializedName("_id")
        public JsonElement objectId;
        public String id;
        public String img;
        public String title;
        public String excerpt;
        public MongoDate pubDate;
        public String author;
        public String categoryTag;
        public String content;
    }

    public static class Category {
        @SerializedName("_id")
        public MongoId objectId;
        @SerializedName("CategoryID")
        public String categoryId;
        @SerializedName("CategoryName")
        public String categoryName;
        @SerializedName("Subcategories")
        public List<Subcategory> subcategories;
    }

    public static class Subcategory {
        @SerializedName("SubcategoryID")
        public String subcategoryId;
        @SerializedName("SubcategoryName")
        public String subcategoryName;
        @SerializedName("img")
        public String img;
    }

    public static class Certificate {
        @SerializedName("_id")
        public MongoId objectId;
        @SerializedName("CertificateID")
        public String certificateId;
        @SerializedName("CertificateName")
        public String certificateName;
        @SerializedName("RequiredCarbonPoint")
        public int requiredCarbonPoint;
        @SerializedName("CertificateDescription")
        public String certificateDescription;
        @SerializedName("RewardDescription")
        public String rewardDescription;
        @SerializedName("Status")
        public boolean status;
    }

    public static class CommunityPost {
        @SerializedName("_id")
        public String objectId;
        @SerializedName("CommunityPostID")
        public String communityPostId;
        @SerializedName("CustomerID")
        public String customerId;
        @SerializedName("Title")
        public String title;
        @SerializedName("Content")
        public String content;
        @SerializedName("Image")
        public String image;
        @SerializedName("Video")
        public String video;
        @SerializedName("PublishDate")
        public String publishDate;
        @SerializedName("UpdateDate")
        public String updateDate;
        @SerializedName("ViewCount")
        public int viewCount;
        @SerializedName("Status")
        public String status;
        @SerializedName("LikesDetail")
        public List<CommunityLike> likesDetail;
        @SerializedName("CommentsDetail")
        public List<CommunityComment> commentsDetail;
        @SerializedName("SavesDetail")
        public List<CommunitySave> savesDetail;
        @SerializedName("LikeCount")
        public int likeCount;
        @SerializedName("CommentCount")
        public int commentCount;
        @SerializedName("SaveCount")
        public int saveCount;
    }

    public static class CommunityLike {
        @SerializedName("CommunityLikeID")
        public String communityLikeId;
        @SerializedName("CustomerID")
        public String customerId;
        @SerializedName("LikeDate")
        public String likeDate;
    }

    public static class CommunityComment {
        @SerializedName("CommunityCommentID")
        public String communityCommentId;
        @SerializedName("CustomerID")
        public String customerId;
        @SerializedName("Content")
        public String content;
        @SerializedName("CommentDate")
        public String commentDate;
    }

    public static class CommunitySave {
        @SerializedName("CommunitySaveID")
        public String communitySaveId;
        @SerializedName("CustomerID")
        public String customerId;
        @SerializedName("SaveDate")
        public String saveDate;
    }

    public static class Consultation {
        @SerializedName("_id")
        public String objectId;
        public String sku;
        public String productName;
        public List<Question> questions;
        public String createdAt;
        public String updatedAt;
        @SerializedName("__v")
        public int version;
    }

    public static class Question {
        @SerializedName("_id")
        public String objectId;
        public String customerId;
        public String customerName;
        public String question;
        public String answer;
        public String status;
        public String answeredBy;
        public String answeredAt;
        public String createdAt;
        public String updatedAt;
    }

    public static class Dish {
        @SerializedName("_id")
        public MongoId objectId;
        @SerializedName("ID")
        public String id;
        @SerializedName("Video")
        public String video;
        @SerializedName("Description")
        public String description;
        @SerializedName("UnitNote")
        public String unitNote;
        @SerializedName("Preparation")
        public String preparation;
        @SerializedName("Cooking")
        public String cooking;
        @SerializedName("Serving")
        public String serving;
        @SerializedName("Ingredients")
        public String ingredients;
        @SerializedName("Tips")
        public String tips;
        @SerializedName("Nutrition")
        public String nutrition;
        @SerializedName("DecorationTip")
        public String decorationTip;
        @SerializedName("Usage")
        public String usage;
        @SerializedName("__v")
        public int version;
    }

    public static class Instruction {
        @SerializedName("_id")
        public MongoId objectId;
        @SerializedName("ID")
        public String id;
        @SerializedName("DishName")
        public String dishName;
        @SerializedName("Image")
        public String image;
        @SerializedName("Ingredient")
        public String ingredient;
        @SerializedName("CookingTime")
        public String cookingTime;
        @SerializedName("Difficulty")
        public String difficulty;
        @SerializedName("Servings")
        public String servings;
        @SerializedName("__v")
        public int version;
    }

    public static class Inventory {
        @SerializedName("_id")
        public MongoId objectId;
        public String sku;
        @SerializedName("total_stock")
        public int totalStock;
        @SerializedName("warehouse_stocks")
        public List<WarehouseStock> warehouseStocks;
    }

    public static class WarehouseStock {
        @SerializedName("ma_kho")
        public String warehouseId;
        public int stock;
    }

    public static class Order {
        @SerializedName("_id")
        public MongoId objectId;
        @SerializedName("OrderID")
        public String orderId;
        @SerializedName("CustomerID")
        public String customerId;
        public String paymentMethod;
        public long subtotal;
        public long shippingFee;
        public long shippingDiscount;
        public double discount;
        public int vatRate;
        public long vatAmount;
        public double totalAmount;
        public String code;
        public boolean wantInvoice;
        public InvoiceInfo invoiceInfo;
        public String consultantCode;
        public String cancelReason;
        public String returnReason;
        public String rejectReason;
        public String status;
        public OrderRoutes routes;
        public MongoDate createdAt;
        public MongoDate updatedAt;
        @SerializedName("__v")
        public int version;
    }

    public static class InvoiceInfo {
        public String companyName;
        public String taxId;
        public String invoiceEmail;
        public String invoiceAddress;
    }

    public static class OrderRoutes {
        public MongoDate pending;
        public MongoDate confirmed;
        public MongoDate shipping;
        public MongoDate delivered;
        public MongoDate completed;
        public MongoDate received;
        public MongoDate cancelled;
        public MongoDate processing_return;
        public MongoDate returning;
        public MongoDate returned;
    }

    public static class OrderDetail {
        @SerializedName("_id")
        public MongoId objectId;
        @SerializedName("OrderID")
        public String orderId;
        public ShippingInfo shippingInfo;
        public List<OrderDetailItem> items;
        @SerializedName("promotion_id")
        public String promotionId;
        @SerializedName("TotalCarbonEmission")
        public double totalCarbonEmission;
        @SerializedName("CarbonPointEarned")
        public int carbonPointEarned;
    }

    public static class OrderDetailItem {
        @SerializedName("_id")
        public MongoId objectId;
        public String sku;
        public String productName;
        public String image;
        public String itemType;
        public String unit;
        public int quantity;
        public long price;
        public long originalPrice;
        @SerializedName("CategoryID")
        public String categoryId;
        @SerializedName("SubcategoryID")
        public String subcategoryId;
        @SerializedName("TotalCarbonEmission")
        public double totalCarbonEmission;
        @SerializedName("CarbonPointEarned")
        public int carbonPointEarned;
    }

    public static class Product {
        @SerializedName("_id")
        public String objectId;
        @SerializedName("product_name")
        public String productName;
        public String brand;
        public String unit;
        public long price;
        public String sku;
        public String origin;
        public String weight;
        public String ingredients;
        public String usage;
        public String storage;
        @SerializedName("manufacture_date")
        public String manufactureDate;
        @SerializedName("expiry_date")
        public String expiryDate;
        public String producer;
        @SerializedName("safety_warning")
        public String safetyWarning;
        public String color;
        @SerializedName("base_price")
        public long basePrice;
        public List<String> image;
        public double rating;
        @SerializedName("purchase_count")
        public int purchaseCount;
        public String status;
        @SerializedName("post_date")
        public MongoDate postDate;
        public int liked;
        public int stock;
        @SerializedName("__v")
        public int version;
        public List<String> groups;
        @SerializedName("CategoryID")
        public String categoryId;
        @SerializedName("SubcategoryID")
        public String subcategoryId;
        @SerializedName("responsible_org")
        public String responsibleOrg;
        @SerializedName("EmissionFactor")
        public double emissionFactor;
        @SerializedName("AllowCustomWeight")
        public Boolean allowCustomWeight;
        @SerializedName("WeightOptions")
        public List<String> weightOptions;
        @SerializedName("CarbonSavingPoint")
        public double carbonSavingPoint;
    }

    public static class Promotion {
        @SerializedName("_id")
        public JsonElement objectId;
        @SerializedName("promotion_id")
        public String promotionId;
        public String code;
        public String name;
        public String description;
        public String type;
        public String scope;
        @SerializedName("promotion_kind")
        public String promotionKind;
        @SerializedName("display_section")
        public String displaySection;
        @SerializedName("discount_type")
        public String discountType;
        @SerializedName("discount_value")
        public int discountValue;
        @SerializedName("max_discount_value")
        public int maxDiscountValue;
        @SerializedName("min_order_value")
        public int minOrderValue;
        @SerializedName("usage_limit")
        public int usageLimit;
        @SerializedName("user_limit")
        public int userLimit;
        @SerializedName("is_first_order_only")
        public boolean firstOrderOnly;
        @SerializedName("start_date")
        public String startDate;
        @SerializedName("end_date")
        public String endDate;
        public String status;
        @SerializedName("created_by")
        public String createdBy;
        @SerializedName("created_at")
        public String createdAt;
        @SerializedName("updated_at")
        public String updatedAt;
    }

    public static class PromotionTarget {
        @SerializedName("_id")
        public JsonElement objectId;
        @SerializedName("promotion_id")
        public String promotionId;
        @SerializedName("target_type")
        public String targetType;
        @SerializedName("target_ref")
        public List<String> targetRef;
        @SerializedName("updated_at")
        public MongoDate updatedAt;
    }

    public static class PromotionUsage {
        @SerializedName("_id")
        public String objectId;
        @SerializedName("promotion_id")
        public String promotionId;
        @SerializedName("user_id")
        public List<String> userIds;
        @SerializedName("order_id")
        public List<String> orderIds;
    }

    public static class Reminder {
        @SerializedName("_id")
        public MongoId objectId;
        @SerializedName("CustomerID")
        public String customerId;
        @SerializedName("SubscriptionID")
        public String subscriptionId;
        public String config_status;
        public String start_date;
        public String end_date;
        public String frequency;
        public String reminder_time;
        public String next_delivery_date;
        public ShippingInfo shippingInfo;
        public List<ReminderItem> items;
        public String paymentMethod;
        public boolean reminder_sound;
        public List<OrderCreationLog> order_creation_log;
    }

    public static class ReminderItem {
        public String sku;
        public String productName;
        @SerializedName("CategoryID")
        public String categoryId;
        public int quantity;
        public long price;
    }

    public static class OrderCreationLog {
        public String generated_OrderID;
        public String date;
        public String time;
        public boolean user_confirmed;
    }

    public static class User {
        @SerializedName("_id")
        public String objectId;
        @SerializedName("CustomerID")
        public String customerId;
        @SerializedName("Phone")
        public String phone;
        @SerializedName("Password")
        public String password;
        @SerializedName("FullName")
        public String fullName;
        @SerializedName("Email")
        public String email;
        @SerializedName("Address")
        public String address;
        @SerializedName("RegisterDate")
        public MongoDate registerDate;
        @SerializedName("CustomerType")
        public String customerType;
        @SerializedName("TotalSpent")
        public double totalSpent;
        @SerializedName("CarbonPoint")
        public int carbonPoint;
        @SerializedName("CertificateID")
        public String certificateId;
        @SerializedName("PasswordVersion")
        public int passwordVersion;
        @SerializedName("LastPasswordReset")
        public String lastPasswordReset;
        @SerializedName("updated_at")
        public String updatedAtSnakeCase;
        @SerializedName("updatedAt")
        public String updatedAt;
        @SerializedName("BirthDay")
        public String birthDay;
        @SerializedName("Gender")
        public String gender;
        @SerializedName("Avatar")
        public String avatar;
        @SerializedName("__v")
        public int version;
    }

    public static class Warehouse {
        @SerializedName("_id")
        public MongoId objectId;
        @SerializedName("ma_kho")
        public String warehouseId;
        @SerializedName("ten_kho")
        public String warehouseName;
        @SerializedName("loai_hinh")
        public String type;
        @SerializedName("dia_chi")
        public WarehouseAddress address;
        @SerializedName("toa_do")
        public Coordinates coordinates;
        @SerializedName("thong_tin_lien_he")
        public ContactInfo contactInfo;
        @SerializedName("thoi_gian_hoat_dong")
        public WorkingHours workingHours;
        @SerializedName("trang_thai")
        public String status;
        public boolean isActive;
    }

    public static class WarehouseAddress {
        @SerializedName("so_nha")
        public String streetNumber;
        @SerializedName("duong")
        public String street;
        @SerializedName("phuong_xa")
        public String ward;
        @SerializedName("quan_huyen")
        public String district;
        @SerializedName("tinh_thanh")
        public String city;
        @SerializedName("ma_quan")
        public String districtCode;
        @SerializedName("ma_tinh")
        public String cityCode;
        @SerializedName("dia_chi_day_du")
        public String fullAddress;
    }

    public static class Coordinates {
        public double lat;
        public double lng;
    }

    public static class ContactInfo {
        public String hotline;
        @SerializedName("so_dien_thoai")
        public List<String> phoneNumbers;
        public String email;
    }

    public static class WorkingHours {
        @SerializedName("thu_2_7")
        public WorkingDay mondayToSaturday;
    }

    public static class WorkingDay {
        @SerializedName("mo_cua")
        public String open;
        @SerializedName("dong_cua")
        public String close;
    }

    public static class ShippingInfo {
        public String fullName;
        public String phone;
        public String email;
        public ShippingAddress address;
        public String deliveryMethod;
        public String notes;
        @SerializedName("warehouse_id")
        public String warehouseId;
    }

    public static class ShippingAddress {
        public String detail;
        public String ward;
        public String district;
        public String city;
    }

    public static class LocationNode {
        public String code;
        public String name;
        public String type;
        public String slug;
        @SerializedName("name_with_type")
        public String nameWithType;
        @SerializedName("path")
        public String path;
        @SerializedName("path_with_type")
        public String pathWithType;
        @SerializedName("parent_code")
        public String parentCode;
        @SerializedName("quan-huyen")
        public Map<String, LocationNode> districts;
        @SerializedName("xa-phuong")
        public Map<String, LocationNode> wards;
    }
}
