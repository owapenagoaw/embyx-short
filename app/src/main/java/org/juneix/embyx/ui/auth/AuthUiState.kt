package com.lalakiop.embyx.ui.auth

import com.lalakiop.embyx.core.model.Session

data class AuthUiState(
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val isRestoring: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val session: Session = Session()
)
