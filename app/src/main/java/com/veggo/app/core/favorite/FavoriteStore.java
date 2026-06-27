package com.veggo.app.core.favorite;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FavoriteStore {
    public static final String TYPE_PRODUCT = "product";
    public static final String TYPE_BLOG = "blog";
    public static final String TYPE_RECIPE = "recipe";

    private static final String PREFS_NAME = "veggo_favorites";
    private static final String KEY_ITEMS = "items";

    private final SharedPreferences preferences;

    public FavoriteStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isFavorite(String type, String id) {
        if (isBlank(type) || isBlank(id)) return false;
        for (FavoriteItem item : getAll()) {
            if (type.equals(item.type) && id.equals(item.id)) {
                return true;
            }
        }
        return false;
    }

    public boolean toggle(FavoriteItem item) {
        if (item == null || isBlank(item.type) || isBlank(item.id)) return false;
        if (isFavorite(item.type, item.id)) {
            remove(item.type, item.id);
            return false;
        }
        add(item);
        return true;
    }

    public void add(FavoriteItem item) {
        if (item == null || isBlank(item.type) || isBlank(item.id)) return;
        List<FavoriteItem> items = getAll();
        for (int i = 0; i < items.size(); i++) {
            FavoriteItem existing = items.get(i);
            if (item.type.equals(existing.type) && item.id.equals(existing.id)) {
                items.set(i, item);
                save(items);
                return;
            }
        }
        items.add(0, item);
        save(items);
    }

    public void remove(String type, String id) {
        List<FavoriteItem> items = getAll();
        for (int i = items.size() - 1; i >= 0; i--) {
            FavoriteItem item = items.get(i);
            if (type.equals(item.type) && id.equals(item.id)) {
                items.remove(i);
            }
        }
        save(items);
    }

    public List<FavoriteItem> getByType(String type) {
        List<FavoriteItem> result = new ArrayList<>();
        for (FavoriteItem item : getAll()) {
            if (type.equals(item.type)) {
                result.add(item);
            }
        }
        return result;
    }

    private List<FavoriteItem> getAll() {
        List<FavoriteItem> items = new ArrayList<>();
        String raw = preferences.getString(KEY_ITEMS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                FavoriteItem item = FavoriteItem.fromJson(object);
                if (item != null) {
                    items.add(item);
                }
            }
        } catch (Exception ignored) {
            preferences.edit().putString(KEY_ITEMS, "[]").apply();
        }
        return items;
    }

    private void save(List<FavoriteItem> items) {
        JSONArray array = new JSONArray();
        for (FavoriteItem item : items) {
            array.put(item.toJson());
        }
        preferences.edit().putString(KEY_ITEMS, array.toString()).apply();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class FavoriteItem {
        public final String type;
        public final String id;
        public final String title;
        public final String subtitle;
        public final String imageUrl;

        public FavoriteItem(String type, String id, String title, String subtitle, String imageUrl) {
            this.type = type;
            this.id = id;
            this.title = title == null ? "" : title;
            this.subtitle = subtitle == null ? "" : subtitle;
            this.imageUrl = imageUrl == null ? "" : imageUrl;
        }

        JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("type", type);
                object.put("id", id);
                object.put("title", title);
                object.put("subtitle", subtitle);
                object.put("imageUrl", imageUrl);
            } catch (Exception ignored) {
            }
            return object;
        }

        static FavoriteItem fromJson(JSONObject object) {
            if (object == null) return null;
            String type = object.optString("type", "");
            String id = object.optString("id", "");
            if (isBlank(type) || isBlank(id)) return null;
            return new FavoriteItem(
                    type,
                    id,
                    object.optString("title", ""),
                    object.optString("subtitle", ""),
                    object.optString("imageUrl", "")
            );
        }
    }
}
