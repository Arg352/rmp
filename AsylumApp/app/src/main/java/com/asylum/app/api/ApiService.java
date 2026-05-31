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
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit интерфейс для Asylum API (http://10.0.2.2:3000 для эмулятора).
 *
 * Все защищённые эндпоинты принимают Bearer-токен через @Header("Authorization").
 */
public interface ApiService {

    // ===== AUTH =====

    /** POST /auth/register */
    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    /** POST /auth/login */
    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    // ===== USERS =====

    /** GET /users/me — свой профиль */
    @GET("users/me")
    Call<UserProfile> getMyProfile(@Header("Authorization") String bearerToken);

    /** GET /users/:id — чужой профиль */
    @GET("users/{id}")
    Call<UserProfile> getUserProfile(
            @Header("Authorization") String bearerToken,
            @Path("id") int userId
    );

    /** GET /users/search?q= — поиск пользователей */
    @GET("users/search")
    Call<List<UserProfile>> searchUsers(
            @Header("Authorization") String bearerToken,
            @Query("q") String query
    );

    /** POST /users/:id/follow — подписаться / отписаться */
    @POST("users/{id}/follow")
    Call<Void> followUser(
            @Header("Authorization") String bearerToken,
            @Path("id") int userId
    );

    /** PATCH /users/settings — обновить настройки профиля */
    @PATCH("users/settings")
    Call<UserProfile> updateSettings(
            @Header("Authorization") String bearerToken,
            @Body UpdateSettingsRequest request
    );

    /** GET /users/me/following — список тех, на кого подписан */
    @GET("users/me/following")
    Call<List<UserProfile>> getFollowing(@Header("Authorization") String bearerToken);

    /** GET /users/me/followers — список подписчиков */
    @GET("users/me/followers")
    Call<List<UserProfile>> getFollowers(@Header("Authorization") String bearerToken);

    // ===== POSTS =====

    /** GET /posts/feed — глобальная лента постов */
    @GET("posts/feed")
    Call<List<Post>> getFeed(
            @Header("Authorization") String bearerToken,
            @Query("tag") String tag
    );

    /** GET /posts/feed без тега */
    @GET("posts/feed")
    Call<List<Post>> getFeed(@Header("Authorization") String bearerToken);

    /** POST /posts — создать пост */
    @POST("posts")
    Call<Post> createPost(
            @Header("Authorization") String bearerToken,
            @Body CreatePostRequest request
    );

    /** POST /posts/:id/like — поставить / снять лайк */
    @POST("posts/{id}/like")
    Call<Void> toggleLike(
            @Header("Authorization") String bearerToken,
            @Path("id") int postId
    );

    // ===== CHATS =====

    /** GET /chats — список всех чатов */
    @GET("chats")
    Call<List<Chat>> getChats(@Header("Authorization") String bearerToken);

    /** GET /chats/:userId — история сообщений с пользователем */
    @GET("chats/{userId}")
    Call<List<Message>> getChatHistory(
            @Header("Authorization") String bearerToken,
            @Path("userId") int otherUserId
    );

    // ===== MEDIA =====

    /**
     * POST /media/upload — загрузка изображений на Cloudinary.
     * Принимает multipart/form-data с полем "images" (до 5 файлов).
     * Возвращает { urls: ["https://..."] }
     */
    @Multipart
    @POST("media/upload")
    Call<MediaUploadResponse> uploadImages(
            @Header("Authorization") String bearerToken,
            @Part List<MultipartBody.Part> images
    );
}
