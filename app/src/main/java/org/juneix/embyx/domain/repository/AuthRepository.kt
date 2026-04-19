package com.lalakiop.embyx.domain.repository

import kotlinx.coroutines.flow.Flow
import com.lalakiop.embyx.core.model.Session

interface AuthRepository {
    val sessionFlow: Flow<Session>

    suspend fun login(server: String, username: String, password: String): Result<Unit>

    suspend fun logout()
}
