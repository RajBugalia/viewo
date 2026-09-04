package com.example.viewo.di

import com.example.viewo.api.ViewoApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        // Use Host IP for physical device testing on the same Wi-Fi
        return Retrofit.Builder()
            .baseUrl("http://10.58.187.64:8081/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideViewoApiService(retrofit: Retrofit): ViewoApiService {
        return retrofit.create(ViewoApiService::class.java)
    }
}
