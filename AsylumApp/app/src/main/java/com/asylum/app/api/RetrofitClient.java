package com.asylum.app.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Singleton Retrofit клиент.
 *
 * ДЛЯ ЭМУЛЯТОРА: BASE_URL = "http://10.0.2.2:3000/"
 * ДЛЯ РЕАЛЬНОГО УСТРОЙСТВА: замените на IP вашего компьютера, например "http://192.168.1.100:3000/"
 */
public class RetrofitClient {

    // 10.0.2.2 — это localhost хост-машины в эмуляторе Android
    private static final String BASE_URL = "http://10.0.2.2:3000/";

    private static RetrofitClient instance;
    private final ApiService apiService;

    private RetrofitClient() {
        // Логирование HTTP-запросов (видно в Logcat)
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }
}
