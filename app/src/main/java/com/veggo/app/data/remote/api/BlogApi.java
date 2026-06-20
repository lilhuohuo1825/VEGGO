package com.veggo.app.data.remote.api;

import com.veggo.app.data.local.entity.BlogCommentEntity;
import com.veggo.app.data.local.entity.BlogEntity;
import com.veggo.app.presentation.blog.BlogRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BlogApi {
    @GET("blog")
    Call<List<BlogEntity>> getBlogs(
            @Query("limit") Integer limit,
            @Query("viewerId") String viewerId
    );

    @GET("blog/{blogId}")
    Call<BlogEntity> getBlog(
            @Path("blogId") String blogId,
            @Query("viewerId") String viewerId
    );

    @POST("blog/{blogId}/like")
    Call<BlogEntity> toggleBlogLike(
            @Path("blogId") String blogId,
            @Body BlogRepository.ToggleBlogCommentLikeRequest request
    );

    @GET("blog/{blogId}/comments")
    Call<List<BlogCommentEntity>> getComments(
            @Path("blogId") String blogId,
            @Query("viewerId") String viewerId
    );

    @POST("blog/comments")
    Call<BlogCommentEntity> createComment(@Body BlogRepository.CreateBlogCommentRequest request);

    @POST("blog/comments/{commentId}/like")
    Call<BlogCommentEntity> toggleCommentLike(
            @Path("commentId") String commentId,
            @Body BlogRepository.ToggleBlogCommentLikeRequest request
    );
}
