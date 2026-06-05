package com.asylum.app.api;

import com.asylum.app.models.AuthResponse;
import com.asylum.app.models.Chat;
import com.asylum.app.models.CreatePostRequest;
import com.asylum.app.models.LoginRequest;
import com.asylum.app.models.Message;
import com.asylum.app.models.Post;
import com.asylum.app.models.RegisterRequest;
import com.asylum.app.models.UpdateSettingsRequest;
import com.asylum.app.models.UserProfile;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ===== AUTH =====
    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    // ===== USERS =====
    @GET("users/me")
    Call<UserProfile> getMyProfile(@Header("Authorization") String bearerToken);

    @GET("users/{id}")
    Call<UserProfile> getUserProfile(@Header("Authorization") String bearerToken, @Path("id") int userId);

    @GET("users/search")
    Call<List<UserProfile>> searchUsers(@Header("Authorization") String bearerToken, @Query("q") String query);

    @POST("users/{id}/follow")
    Call<Void> followUser(@Header("Authorization") String bearerToken, @Path("id") int userId);

    @PATCH("users/settings")
    Call<UserProfile> updateSettings(@Header("Authorization") String bearerToken, @Body UpdateSettingsRequest request);

    @GET("users/me/following")
    Call<List<UserProfile>> getFollowing(@Header("Authorization") String bearerToken);

    @GET("users/me/followers")
    Call<List<UserProfile>> getFollowers(@Header("Authorization") String bearerToken);

    // ===== POSTS =====
    @GET("posts/feed")
    Call<List<Post>> getFeed(@Header("Authorization") String bearerToken, @Query("tag") String tag);

    @GET("posts/feed")
    Call<List<Post>> getFeed(@Header("Authorization") String bearerToken);

    @POST("posts")
    Call<Post> createPost(@Header("Authorization") String bearerToken, @Body CreatePostRequest request);

    @POST("posts/{id}/like")
    Call<Void> toggleLike(@Header("Authorization") String bearerToken, @Path("id") int postId);

    @DELETE("posts/{id}")
    Call<Void> deletePost(@Header("Authorization") String bearerToken, @Path("id") int postId);

    // ===== CHATS =====
    @GET("chats")
    Call<List<Chat>> getChats(@Header("Authorization") String bearerToken);

    @GET("chats/{userId}")
    Call<List<Message>> getChatHistory(@Header("Authorization") String bearerToken, @Path("userId") int otherUserId);

    // ===== MEDIA =====
    /**
     * Важно: для List<MultipartBody.Part> не указывается имя в @Part(), 
     * так как имя уже задано внутри каждого Part.
     */
    @Multipart
    @POST("media/upload")
    Call<MediaUploadResponse> uploadImages(
            @Header("Authorization") String bearerToken,
            @Part List<MultipartBody.Part> parts
    );
}
