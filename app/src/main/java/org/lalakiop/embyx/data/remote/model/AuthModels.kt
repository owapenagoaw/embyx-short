package com.lalakiop.embyx.data.remote.model

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    @SerializedName("Username") val username: String,
    @SerializedName("Pw") val password: String
)

data class AuthResponse(
    @SerializedName("AccessToken") val accessToken: String?,
    @SerializedName("SessionInfo") val sessionInfo: SessionInfo?
)

data class SessionInfo(
    @SerializedName("UserId") val userId: String?
)
