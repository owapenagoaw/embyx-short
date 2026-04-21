package com.lalakiop.embyx.data.remote

import com.lalakiop.embyx.data.remote.model.AuthRequest
import com.lalakiop.embyx.data.remote.model.AuthResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface EmbyAuthApi {
    @POST("emby/Users/AuthenticateByName")
    suspend fun authenticateByName(
        @Header("X-Emby-Authorization") authorization: String,
        @Body body: AuthRequest
    ): Response<AuthResponse>
}
