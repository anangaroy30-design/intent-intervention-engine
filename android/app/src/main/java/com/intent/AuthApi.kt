package com.intent.app

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header

interface AuthApi {
    @POST("auth/signup")
    suspend fun signup(
        @Body request: AuthRequest
    ): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: AuthRequest
    ): Response<AuthResponse>

    @GET("auth/me")
    suspend fun me(
        @Header("Authorization") authorization: String
    ): Response<MeResponse>

    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String
    ): Response<Map<String, Boolean>>
}