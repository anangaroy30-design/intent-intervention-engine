package com.intent.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val Context.authDataStore by preferencesDataStore(
    name = "intent_auth"
)

class AuthRepository(context: Context) {
    private val appContext = context.applicationContext

    private val tokenKey = stringPreferencesKey("jwt_token")

    private val api: AuthApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AuthApi::class.java)

    suspend fun token(): String? =
        appContext.authDataStore.data
            .map { it[tokenKey] }
            .first()

    suspend fun login(
        email: String,
        password: String
    ): AuthUser {
        val response = api.login(
            AuthRequest(email.trim(), password)
        )

        if (!response.isSuccessful) {
            throw Exception(
                response.errorBody()?.string()
                    ?: "Login failed (${response.code()})"
            )
        }

        val body = response.body()
            ?: throw Exception("Empty login response")

        appContext.authDataStore.edit {
            it[tokenKey] = body.token
        }

        return body.user
    }

    suspend fun signup(
        email: String,
        password: String
    ): AuthUser {
        val response = api.signup(
            AuthRequest(email.trim(), password)
        )

        if (!response.isSuccessful) {
            throw Exception(
                response.errorBody()?.string()
                    ?: "Registration failed (${response.code()})"
            )
        }

        val body = response.body()
            ?: throw Exception("Empty registration response")

        appContext.authDataStore.edit {
            it[tokenKey] = body.token
        }

        return body.user
    }

    suspend fun currentUser(): AuthUser? {
        val savedToken = token() ?: return null

        return try {
            val response = api.me("Bearer $savedToken")

            if (response.isSuccessful) {
                response.body()?.user
            } else {
                clearToken()
                null
            }
        } catch (e: Exception) {
            // Preserve the token during temporary network failures.
            throw e
        }
    }

    suspend fun logout() {
        val savedToken = token()

        try {
            if (savedToken != null) {
                api.logout("Bearer $savedToken")
            }
        } finally {
            clearToken()
        }
    }

    suspend fun clearToken() {
        appContext.authDataStore.edit {
            it.remove(tokenKey)
        }
    }
}
