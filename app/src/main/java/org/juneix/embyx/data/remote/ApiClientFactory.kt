package com.lalakiop.embyx.data.remote

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClientFactory {

    fun createAuthApi(server: String): EmbyAuthApi {
        val retrofit = createRetrofit(server = server, token = null)
        return retrofit.create(EmbyAuthApi::class.java)
    }

    fun createMediaApi(server: String, token: String): EmbyMediaApi {
        val retrofit = createRetrofit(server = server, token = token)
        return retrofit.create(EmbyMediaApi::class.java)
    }

    private fun createRetrofit(server: String, token: String?): Retrofit {
        val baseUrl = if (server.endsWith('/')) server else "$server/"

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(EmbyAuthorizationInterceptor)

        if (!token.isNullOrBlank()) {
            clientBuilder.addInterceptor(ApiKeyQueryInterceptor(token))
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private object EmbyAuthorizationInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
                .header(
                    "X-Emby-Authorization",
                    "Emby Client=\"EmbyXNative\", Device=\"Android\", DeviceId=\"EmbyXNative-Device\", Version=\"0.1.0\""
                )
                .build()
            return chain.proceed(request)
        }
    }

    private class ApiKeyQueryInterceptor(
        private val token: String
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val originalUrl: HttpUrl = original.url
            val hasApiKey = originalUrl.queryParameterNames.contains("api_key")

            val newUrl = if (hasApiKey) {
                originalUrl
            } else {
                originalUrl.newBuilder()
                    .addQueryParameter("api_key", token)
                    .build()
            }

            val request = original.newBuilder()
                .url(newUrl)
                .build()
            return chain.proceed(request)
        }
    }
}
