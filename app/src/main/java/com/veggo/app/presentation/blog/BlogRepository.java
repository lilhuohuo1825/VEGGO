package com.veggo.app.presentation.blog;

import android.content.Context;

import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.data.local.entity.BlogCommentEntity;
import com.veggo.app.data.local.entity.BlogEntity;
import com.veggo.app.data.remote.api.BlogApi;
import com.veggo.app.presentation.community.CommunityRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class BlogRepository {
    public interface Callback<T> {
        void onResult(T result);
    }

    private final BlogApi api;
    private final AppPreferences preferences;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public BlogRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.api = ApiClient.createService(BlogApi.class);
        this.preferences = new AppPreferences(appContext);
    }

    public void getLatest(int limit, Callback<List<BlogEntity>> callback) {
        request(api.getBlogs(limit, currentCustomerId()), new ArrayList<>(), callback);
    }

    public void getAll(Callback<List<BlogEntity>> callback) {
        request(api.getBlogs(null, currentCustomerId()), new ArrayList<>(), callback);
    }

    public void getById(String blogId, Callback<BlogEntity> callback) {
        request(api.getBlog(blogId, currentCustomerId()), null, callback);
    }

    public void toggleBlogLike(String blogId, Callback<BlogEntity> callback) {
        request(api.toggleBlogLike(blogId, new ToggleBlogCommentLikeRequest(currentCustomerId())), null, callback);
    }

    public void loadComments(String blogId, Callback<List<BlogCommentEntity>> callback) {
        request(api.getComments(blogId, currentCustomerId()), new ArrayList<>(), callback);
    }

    public void createComment(String blogId, String content, Callback<BlogCommentEntity> callback) {
        CreateBlogCommentRequest request = new CreateBlogCommentRequest(blogId, currentCustomerId(), content);
        request.userName = currentCustomerName();
        request.userImageUrl = CommunityRepository.ACCOUNT_AVATAR_URL;
        request(api.createComment(request), null, callback);
    }

    public void toggleCommentLike(String commentId, Callback<BlogCommentEntity> callback) {
        request(api.toggleCommentLike(commentId, new ToggleBlogCommentLikeRequest(currentCustomerId())), null, callback);
    }

    public String currentCustomerId() {
        return firstNonEmpty(preferences.getCustomerId(), CommunityRepository.ACCOUNT_ID);
    }

    public String currentCustomerName() {
        return firstNonEmpty(preferences.getFullName(), CommunityRepository.ACCOUNT_NAME);
    }

    private <T> void request(Call<T> call, T fallback, Callback<T> callback) {
        executor.execute(() -> {
            T result = execute(call);
            callback.onResult(result == null ? fallback : result);
        });
    }

    private <T> T execute(Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private static String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        if (second != null && !second.trim().isEmpty()) {
            return second;
        }
        return "";
    }

    public static class CreateBlogCommentRequest {
        public String blogId;
        public String customerId;
        public String userName;
        public String userImageUrl;
        public String content;

        public CreateBlogCommentRequest(String blogId, String customerId, String content) {
            this.blogId = blogId;
            this.customerId = customerId;
            this.content = content;
        }
    }

    public static class ToggleBlogCommentLikeRequest {
        public String customerId;

        public ToggleBlogCommentLikeRequest(String customerId) {
            this.customerId = customerId;
        }
    }
}
