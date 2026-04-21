package com.lalakiop.embyx.core.model

data class Session(
    val server: String = "",
    val token: String = "",
    val userId: String = "",
    val username: String = ""
) {
    val isLoggedIn: Boolean
        get() = server.isNotBlank() && token.isNotBlank() && userId.isNotBlank()
}
