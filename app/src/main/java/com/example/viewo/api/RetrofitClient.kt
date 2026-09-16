package com.example.viewo.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = com.example.viewo.Constants.BASE_URL

    val apiService: ViewoApiService by lazy {
        Retrofit.Builder()
            // Use Host IP for physical device testing on the same Wi-Fi
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ViewoApiService::class.java)
    }
}
