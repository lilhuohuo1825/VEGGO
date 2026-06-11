package com.veggo.app.presentation.blog;

import android.content.Context;

import com.veggo.app.assets.AssetFiles;
import com.veggo.app.assets.AssetJsonLoader;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.database.VeggoDatabase;
import com.veggo.app.data.local.entity.BlogEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlogRepository {
    public interface Callback<T> {
        void onResult(T result);
    }

    private final Context context;
    private final VeggoDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public BlogRepository(Context context) {
        this.context = context.getApplicationContext();
        this.database = VeggoDatabase.getInstance(this.context);
    }

    public void getLatest(int limit, Callback<List<BlogEntity>> callback) {
        executor.execute(() -> {
            ensureSeeded();
            callback.onResult(database.blogDao().getLatest(limit));
        });
    }

    public void getAll(Callback<List<BlogEntity>> callback) {
        executor.execute(() -> {
            ensureSeeded();
            callback.onResult(database.blogDao().getAll());
        });
    }

    public void getById(String blogId, Callback<BlogEntity> callback) {
        executor.execute(() -> {
            ensureSeeded();
            callback.onResult(database.blogDao().getById(blogId));
        });
    }

    private void ensureSeeded() {
        if (database.blogDao().count() > 0) {
            return;
        }
        try {
            AssetJsonLoader loader = new AssetJsonLoader(context);
            List<AssetModels.Blog> assetBlogs = loader.readList(AssetFiles.BLOGS, AssetModels.Blog.class);
            List<BlogEntity> blogs = new ArrayList<>();
            Set<String> usedImages = new HashSet<>();
            int fallbackIndex = 0;
            for (AssetModels.Blog assetBlog : assetBlogs) {
                String id = firstNonEmpty(BlogText.objectIdValue(assetBlog.objectId), assetBlog.id);
                if (id == null) {
                    continue;
                }
                String publishedAt = assetBlog.pubDate == null ? null : assetBlog.pubDate.date;
                String imageUrl = normalizedImage(assetBlog.img, assetBlog.categoryTag, usedImages, fallbackIndex);
                if (!imageUrl.equals(assetBlog.img)) {
                    fallbackIndex++;
                }
                usedImages.add(imageUrl);
                blogs.add(new BlogEntity(
                        id,
                        imageUrl,
                        assetBlog.title,
                        assetBlog.excerpt,
                        publishedAt,
                        parseMillis(publishedAt),
                        assetBlog.author,
                        assetBlog.categoryTag,
                        assetBlog.content
                ));
            }
            database.blogDao().insertAll(blogs);
        } catch (Exception ignored) {
        }
    }

    private static String normalizedImage(String imageUrl, String category, Set<String> usedImages, int fallbackIndex) {
        if (imageUrl != null && !imageUrl.trim().isEmpty() && !usedImages.contains(imageUrl)) {
            return imageUrl;
        }
        String[] pool = fallbackImages(category);
        for (int offset = 0; offset < pool.length; offset++) {
            String candidate = pool[Math.abs(fallbackIndex + offset) % pool.length];
            if (!usedImages.contains(candidate)) {
                return candidate;
            }
        }
        return pool[Math.abs(fallbackIndex) % pool.length];
    }

    private static String[] fallbackImages(String category) {
        String cleanCategory = category == null ? "" : category.toLowerCase();
        if (cleanCategory.contains("nấm") || cleanCategory.contains("náº¥m")) {
            return MUSHROOM_IMAGES;
        }
        if (cleanCategory.contains("trái") || cleanCategory.contains("trÃ¡i")) {
            return FRUIT_IMAGES;
        }
        if (cleanCategory.contains("rau")) {
            return VEGETABLE_IMAGES;
        }
        if (cleanCategory.contains("nông") || cleanCategory.contains("nÃ´ng")) {
            return FARM_IMAGES;
        }
        return NUTRITION_IMAGES;
    }

    private static final String[] VEGETABLE_IMAGES = {
            "https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1566385101042-1a0aa0c1268c?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1598170845058-32b996a69f76?auto=format&fit=crop&w=1200&q=80"
    };

    private static final String[] FRUIT_IMAGES = {
            "https://images.unsplash.com/photo-1610832958506-aa56368176cf?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1579613832125-5d34a13e691b?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1550258987-190a2d41a8ba?auto=format&fit=crop&w=1200&q=80"
    };

    private static final String[] MUSHROOM_IMAGES = {
            "https://images.unsplash.com/photo-1518977676601-b53f82aba655?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1607877742574-a2f1f62f57f6?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1512595765784-5ebad80772a6?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1603048719539-9ecb4aa395e3?auto=format&fit=crop&w=1200&q=80"
    };

    private static final String[] FARM_IMAGES = {
            "https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1464226184884-fa280b87c399?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1492496913980-501348b61469?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1523741543316-beb7fc7023d8?auto=format&fit=crop&w=1200&q=80"
    };

    private static final String[] NUTRITION_IMAGES = {
            "https://images.unsplash.com/photo-1498837167922-ddd27525d352?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1490645935967-10de6ba17061?auto=format&fit=crop&w=1200&q=80",
            "https://images.unsplash.com/photo-1543362906-acfc16c67564?auto=format&fit=crop&w=1200&q=80"
    };

    private static long parseMillis(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (Exception exception) {
            return 0L;
        }
    }

    private static String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        if (second != null && !second.trim().isEmpty()) {
            return second;
        }
        return null;
    }
}
