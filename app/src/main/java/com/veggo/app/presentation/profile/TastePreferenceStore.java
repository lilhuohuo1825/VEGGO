package com.veggo.app.presentation.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.data.remote.api.UserApi;
import com.veggo.app.domain.model.Product;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Response;

public class TastePreferenceStore {
    private static final String PREFS = "taste_preferences";
    private static final String KEY_SEEDED = "seeded";
    private static final String KEY_HIDE_CATALOG = "hide_catalog";
    private static final String KEY_BADGE_CATALOG = "badge_catalog";
    private static final String KEY_SUGGEST_MENU = "suggest_menu";
    private static final String KEY_DIET_MODE = "diet_mode";
    private static final String KEY_TAGS = "tags";

    public static final String ACTION_BLOCK = "Không ăn";
    public static final String ACTION_ALLERGY = "Dị ứng";
    public static final String ACTION_WARN = "Cảnh báo";

    private final Context context;
    private final SharedPreferences prefs;

    public TastePreferenceStore(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        seedIfNeeded();
    }

    public List<TasteTag> tags() {
        List<TasteTag> result = new ArrayList<>();
        Set<String> encoded = prefs.getStringSet(KEY_TAGS, new HashSet<>());
        for (String item : encoded) {
            TasteTag tag = TasteTag.decode(item);
            if (tag != null) result.add(tag);
        }
        return result;
    }

    public List<TasteTag> enabledTags() {
        List<TasteTag> result = new ArrayList<>();
        for (TasteTag tag : tags()) {
            if (tag.enabled) result.add(tag);
        }
        return result;
    }

    public List<TasteTag> enabledBlockingTags() {
        List<TasteTag> result = new ArrayList<>();
        for (TasteTag tag : tags()) {
            if (tag.enabled && !ACTION_WARN.equals(tag.action)) result.add(tag);
        }
        return result;
    }

    public void saveTags(List<TasteTag> tags) {
        saveTagsInternal(tags, true);
    }

    private void saveTagsInternal(List<TasteTag> tags, boolean syncRemote) {
        Set<String> encoded = new HashSet<>();
        for (TasteTag tag : tags) encoded.add(tag.encode());
        prefs.edit().putStringSet(KEY_TAGS, encoded).putBoolean(KEY_SEEDED, true).apply();
        if (syncRemote) saveToMongo();
    }

    public void setTagEnabled(String label, boolean enabled) {
        List<TasteTag> next = new ArrayList<>();
        for (TasteTag tag : tags()) {
            next.add(tag.label.equals(label) ? new TasteTag(tag.label, tag.action, tag.keywords, enabled) : tag);
        }
        saveTags(next);
    }

    public void upsertTag(String originalLabel, String label, String action, String keywords, boolean enabled) {
        String cleanLabel = label == null ? "" : label.trim();
        if (cleanLabel.isEmpty()) return;
        String cleanKeywords = keywords == null || keywords.trim().isEmpty() || keywords.trim().equalsIgnoreCase(cleanLabel)
                ? autoKeywordsFor(cleanLabel)
                : keywords.trim();
        String cleanAction = normalizeAction(action);
        List<TasteTag> next = new ArrayList<>();
        boolean updated = false;
        for (TasteTag tag : tags()) {
            boolean sameOriginal = originalLabel != null && tag.label.equals(originalLabel);
            boolean sameLabel = originalLabel == null && normalize(tag.label).equals(normalize(cleanLabel));
            if (sameOriginal || sameLabel) {
                next.add(new TasteTag(cleanLabel, cleanAction, cleanKeywords, enabled));
                updated = true;
            } else {
                next.add(tag);
            }
        }
        if (!updated) next.add(new TasteTag(cleanLabel, cleanAction, cleanKeywords, enabled));
        saveTags(next);
    }

    public void removeTag(String label) {
        List<TasteTag> next = new ArrayList<>();
        for (TasteTag tag : tags()) {
            if (!tag.label.equals(label)) next.add(tag);
        }
        saveTags(next);
    }

    public int enabledTagCount() {
        return enabledTags().size();
    }

    public int enabledBlockingCount() {
        return enabledBlockingTags().size();
    }

    public String dietMode() {
        return prefs.getString(KEY_DIET_MODE, "vegan");
    }

    public void setDietMode(String value) {
        prefs.edit().putString(KEY_DIET_MODE, value == null ? "vegan" : value).apply();
        saveToMongo();
    }

    public boolean hideCatalog() {
        return prefs.getBoolean(KEY_HIDE_CATALOG, true);
    }

    public void setHideCatalog(boolean enabled) {
        SharedPreferences.Editor editor = prefs.edit().putBoolean(KEY_HIDE_CATALOG, enabled);
        if (enabled) {
            editor.putBoolean(KEY_BADGE_CATALOG, false);
        } else if (!badgeCatalog()) {
            editor.putBoolean(KEY_BADGE_CATALOG, true);
        }
        editor.apply();
        saveToMongo();
    }

    public boolean badgeCatalog() {
        return prefs.getBoolean(KEY_BADGE_CATALOG, !hideCatalog());
    }

    public void setBadgeCatalog(boolean enabled) {
        SharedPreferences.Editor editor = prefs.edit().putBoolean(KEY_BADGE_CATALOG, enabled);
        if (enabled) {
            editor.putBoolean(KEY_HIDE_CATALOG, false);
        } else if (!hideCatalog()) {
            editor.putBoolean(KEY_HIDE_CATALOG, true);
        }
        editor.apply();
        saveToMongo();
    }

    public boolean suggestMenu() {
        return prefs.getBoolean(KEY_SUGGEST_MENU, true);
    }

    public void syncFromMongo(Runnable callback) {
        String customerId = new AppPreferences(context).getCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) {
            runOnMain(callback);
            return;
        }
        new Thread(() -> {
            try {
                UserApi api = ApiClient.createService(UserApi.class);
                Response<RemoteTastePreferences> response = api.getTastePreferences(customerId).execute();
                if (response.isSuccessful() && response.body() != null) applyRemote(response.body());
            } catch (Exception ignored) {
            }
            runOnMain(callback);
        }).start();
    }

    private void saveToMongo() {
        String customerId = new AppPreferences(context).getCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) return;
        RemoteTastePreferences remote = toRemote();
        new Thread(() -> {
            try {
                ApiClient.createService(UserApi.class).saveTastePreferences(customerId, remote).execute();
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void applyRemote(RemoteTastePreferences remote) {
        List<TasteTag> next = new ArrayList<>();
        if (remote.tags != null) {
            for (RemoteTasteTag tag : remote.tags) {
                if (tag == null || tag.label == null || tag.action == null) continue;
                next.add(new TasteTag(tag.label, normalizeAction(tag.action), joinKeywords(tag.keywords), tag.enabled));
            }
        }
        if (!next.isEmpty()) saveTagsInternal(next, false);
        boolean badge = "badge".equals(remote.catalogBehavior);
        prefs.edit()
                .putString(KEY_DIET_MODE, remote.dietMode == null ? "vegan" : remote.dietMode)
                .putBoolean(KEY_HIDE_CATALOG, !badge)
                .putBoolean(KEY_BADGE_CATALOG, badge)
                .putBoolean(KEY_SUGGEST_MENU, remote.suggestMenu)
                .putBoolean(KEY_SEEDED, true)
                .apply();
    }

    private RemoteTastePreferences toRemote() {
        RemoteTastePreferences remote = new RemoteTastePreferences();
        remote.dietMode = dietMode();
        remote.catalogBehavior = badgeCatalog() ? "badge" : "hide";
        remote.suggestMenu = suggestMenu();
        remote.tags = new ArrayList<>();
        for (TasteTag tag : tags()) {
            RemoteTasteTag remoteTag = new RemoteTasteTag();
            remoteTag.label = tag.label;
            remoteTag.action = tag.action;
            remoteTag.keywords = splitKeywords(tag.keywords);
            remoteTag.enabled = tag.enabled;
            remote.tags.add(remoteTag);
        }
        return remote;
    }

    public void saveGeneratedTasteData(List<TasteTag> alertTags, List<String> replacementSuggestions) {
        String customerId = new AppPreferences(context).getCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) return;
        RemoteTastePreferences remote = toRemote();
        remote.alertIngredients = new ArrayList<>();
        if (alertTags != null) {
            for (TasteTag tag : alertTags) {
                RemoteTasteAlertItem item = new RemoteTasteAlertItem();
                item.label = tag.label;
                item.action = tag.action;
                item.status = ACTION_WARN.equals(tag.action) ? "Cần kiểm tra" : "Không phù hợp";
                item.description = ACTION_WARN.equals(tag.action)
                        ? "Sản phẩm liên quan sẽ được gắn cảnh báo trước khi đặt món."
                        : "Sản phẩm liên quan sẽ được chặn khỏi catalog hoặc đánh dấu theo lựa chọn của bạn.";
                remote.alertIngredients.add(item);
            }
        }
        remote.replacementSuggestions = replacementSuggestions == null ? new ArrayList<>() : new ArrayList<>(replacementSuggestions);
        new Thread(() -> {
            try {
                ApiClient.createService(UserApi.class).saveTastePreferences(customerId, remote).execute();
            } catch (Exception ignored) {
            }
        }).start();
    }

    public boolean shouldHide(Product product) {
        return hideCatalog() && matchingBlockingTag(product) != null;
    }

    public String productWarning(Product product) {
        if (!badgeCatalog()) return "";
        TasteTag tag = matchingAnyTag(product);
        return tag == null ? "" : tag.action + ": " + tag.label;
    }

    public TasteTag matchingBlockingTag(Product product) {
        if (product == null) return null;
        String haystack = normalize(product.getName() + " " + product.getDescription() + " " + product.getCategoryId() + " " + product.getSubcategoryId());
        return matchingBlockingTag(haystack);
    }

    public TasteTag matchingAnyTag(Product product) {
        if (product == null) return null;
        String haystack = normalize(product.getName() + " " + product.getDescription() + " " + product.getCategoryId() + " " + product.getSubcategoryId());
        return matchingAnyTag(haystack);
    }

    public TasteTag matchingBlockingTag(AssetModels.Product product) {
        if (product == null) return null;
        return matchingBlockingTag(productHaystack(product));
    }

    public TasteTag matchingAnyTag(AssetModels.Product product) {
        if (product == null) return null;
        return matchingAnyTag(productHaystack(product));
    }

    public TasteTag matchingBlockingTag(AssetModels.Instruction instruction) {
        if (instruction == null) return null;
        return matchingBlockingTag(normalize(instruction.dishName + " " + instruction.ingredient));
    }

    public boolean instructionBlocked(AssetModels.Instruction instruction) {
        return matchingBlockingTag(instruction) != null;
    }

    public List<AssetModels.Product> blockedProducts(List<AssetModels.Product> products) {
        List<AssetModels.Product> result = new ArrayList<>();
        if (products == null) return result;
        Set<String> seenTags = new HashSet<>();
        for (AssetModels.Product product : products) {
            TasteTag tag = matchingBlockingTag(product);
            if (tag != null && seenTags.add(tag.label)) result.add(product);
        }
        return result;
    }

    public List<AssetModels.Product> alternativesFor(AssetModels.Product blocked, List<AssetModels.Product> products) {
        List<AssetModels.Product> result = new ArrayList<>();
        if (blocked == null || products == null) return result;
        for (AssetModels.Product product : products) {
            if (sameProduct(product, blocked) || matchingBlockingTag(product) != null) continue;
            boolean sameCategory = safe(product.categoryId).equals(safe(blocked.categoryId));
            boolean differentSub = !safe(product.subcategoryId).equals(safe(blocked.subcategoryId));
            if (sameCategory && differentSub) result.add(product);
            if (result.size() >= 4) break;
        }
        if (result.isEmpty()) {
            for (AssetModels.Product product : products) {
                if (!sameProduct(product, blocked) && matchingBlockingTag(product) == null) result.add(product);
                if (result.size() >= 4) break;
            }
        }
        return result;
    }

    public List<AssetModels.Product> alternativesForUserTags(List<AssetModels.Product> products, int limit) {
        List<AssetModels.Product> result = new ArrayList<>();
        Set<String> seenSku = new HashSet<>();
        for (AssetModels.Product blocked : blockedProducts(products)) {
            for (AssetModels.Product alt : alternativesFor(blocked, products)) {
                String key = safe(alt.sku) + safe(alt.productName);
                if (seenSku.add(key)) result.add(alt);
                if (result.size() >= limit) return result;
            }
        }
        return result;
    }

    public List<AssetModels.Instruction> safeInstructions(List<AssetModels.Instruction> instructions, int limit) {
        List<AssetModels.Instruction> result = new ArrayList<>();
        if (instructions == null) return result;
        for (AssetModels.Instruction instruction : instructions) {
            if (hasText(instruction.dishName) && !instructionBlocked(instruction)) result.add(instruction);
            if (result.size() >= limit) break;
        }
        return result;
    }

    public List<String> replacementNotes(List<AssetModels.Product> products, List<AssetModels.Instruction> instructions, int limit) {
        List<String> notes = new ArrayList<>();
        for (AssetModels.Product product : alternativesForUserTags(products, limit)) {
            notes.add("Dùng " + safe(product.productName) + " thay cho nguyên liệu cần tránh");
            if (notes.size() >= limit) return notes;
        }
        for (AssetModels.Instruction instruction : safeInstructions(instructions, limit)) {
            notes.add(instruction.dishName);
            if (notes.size() >= limit) return notes;
        }
        return notes;
    }

    private TasteTag matchingBlockingTag(String normalizedHaystack) {
        for (TasteTag tag : tags()) {
            if (tag.enabled && !ACTION_WARN.equals(tag.action) && tag.matches(normalizedHaystack)) return tag;
        }
        return null;
    }

    private TasteTag matchingAnyTag(String normalizedHaystack) {
        for (TasteTag tag : tags()) {
            if (tag.enabled && tag.matches(normalizedHaystack)) return tag;
        }
        return null;
    }

    private void seedIfNeeded() {
        if (prefs.getBoolean(KEY_SEEDED, false)) return;
        saveTagsInternal(Arrays.asList(
                new TasteTag("ca cao", ACTION_BLOCK, "ca cao,cacao,socola,chocolate", false),
                new TasteTag("trái cây nhiệt đới", ACTION_BLOCK, "xoài,dưa hấu,đu đủ,sầu riêng,thanh long,chuối,dứa,thơm", true),
                new TasteTag("đào", ACTION_ALLERGY, "đào,peach", true),
                new TasteTag("đậu phộng", ACTION_ALLERGY, "đậu phộng,peanut,lạc", true),
                new TasteTag("cà chua", ACTION_WARN, "cà chua,tomato", true)
        ), false);
        prefs.edit()
                .putBoolean(KEY_HIDE_CATALOG, true)
                .putBoolean(KEY_BADGE_CATALOG, false)
                .putBoolean(KEY_SUGGEST_MENU, true)
                .putString(KEY_DIET_MODE, "vegan")
                .apply();
    }

    static String normalize(String value) {
        String raw = value == null ? "" : value.toLowerCase(Locale.ROOT);
        String stripped = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return stripped.replace('đ', 'd');
    }

    private static String productHaystack(AssetModels.Product product) {
        return normalize(product.productName + " " + product.ingredients + " " + product.categoryId + " " + product.subcategoryId + " " + product.groups);
    }

    private static boolean sameProduct(AssetModels.Product first, AssetModels.Product second) {
        return safe(first.sku).equals(safe(second.sku)) && safe(first.productName).equals(safe(second.productName));
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static List<String> splitKeywords(String keywords) {
        List<String> result = new ArrayList<>();
        if (keywords == null) return result;
        Set<String> seen = new HashSet<>();
        for (String keyword : keywords.split(",")) {
            String trimmed = keyword.trim();
            if (!trimmed.isEmpty() && seen.add(trimmed)) result.add(trimmed);
        }
        return result;
    }

    private static String autoKeywordsFor(String label) {
        List<String> result = new ArrayList<>();
        String cleanLabel = label == null ? "" : label.trim();
        String normalized = normalize(cleanLabel);
        addKeyword(result, cleanLabel);
        if (normalized.contains("dau phong")) {
            addKeyword(result, "lạc");
        } else if (normalized.contains("hai san")) {
            addKeyword(result, "tôm");
            addKeyword(result, "cua");
            addKeyword(result, "mực");
        } else if (normalized.contains("sua")) {
            addKeyword(result, "phô mai");
            addKeyword(result, "bơ sữa");
        } else if (normalized.contains("ca chua")) {
            addKeyword(result, "cà chua bi");
        } else if (normalized.contains("ca cao")) {
            addKeyword(result, "cacao");
            addKeyword(result, "sô cô la");
            addKeyword(result, "socola");
        } else if (normalized.contains("dao")) {
            addKeyword(result, "đào");
        } else if (normalized.contains("trai cay nhiet doi")) {
            addKeyword(result, "xoài");
            addKeyword(result, "dưa hấu");
            addKeyword(result, "đu đủ");
            addKeyword(result, "sầu riêng");
            addKeyword(result, "thanh long");
            addKeyword(result, "chuối");
            addKeyword(result, "dứa");
            addKeyword(result, "thơm");
        }
        return joinKeywords(result);
    }

    private static void addKeyword(List<String> result, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return;
        String clean = keyword.trim();
        for (String item : result) {
            if (normalize(item).equals(normalize(clean))) return;
        }
        result.add(clean);
    }

    private static String joinKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (String keyword : keywords) {
            if (keyword == null || keyword.trim().isEmpty()) continue;
            if (builder.length() > 0) builder.append(',');
            builder.append(keyword.trim());
        }
        return builder.toString();
    }

    private static String normalizeAction(String action) {
        String value = normalize(action);
        if (value.contains("di ung")) return ACTION_ALLERGY;
        if (value.contains("canh bao")) return ACTION_WARN;
        return ACTION_BLOCK;
    }

    private static void runOnMain(Runnable callback) {
        if (callback != null) new Handler(Looper.getMainLooper()).post(callback);
    }

    public static class TasteTag {
        public final String label;
        public final String action;
        public final String keywords;
        public final boolean enabled;

        public TasteTag(String label, String action, String keywords, boolean enabled) {
            this.label = label;
            this.action = action;
            this.keywords = keywords;
            this.enabled = enabled;
        }

        public boolean matches(String normalizedHaystack) {
            for (String keyword : keywords.split(",")) {
                String value = normalize(keyword).trim();
                if (!value.isEmpty() && normalizedHaystack.contains(value)) return true;
            }
            return false;
        }

        String encode() {
            return label + "|" + action + "|" + keywords + "|" + enabled;
        }

        static TasteTag decode(String value) {
            String[] parts = value == null ? new String[0] : value.split("\\|", -1);
            if (parts.length < 4) return null;
            return new TasteTag(parts[0], normalizeAction(parts[1]), parts[2], Boolean.parseBoolean(parts[3]));
        }
    }

    public static class RemoteTastePreferences {
        public String dietMode;
        public String catalogBehavior;
        public boolean suggestMenu;
        public List<RemoteTasteTag> tags;
        public List<RemoteTasteAlertItem> alertIngredients;
        public List<String> replacementSuggestions;
    }

    public static class RemoteTasteTag {
        public String label;
        public String action;
        public List<String> keywords;
        public boolean enabled;
    }

    public static class RemoteTasteAlertItem {
        public String label;
        public String action;
        public String status;
        public String description;
    }
}
