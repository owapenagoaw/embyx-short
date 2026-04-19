package com.lalakiop.embyx.domain.usecase

import com.lalakiop.embyx.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(server: String, username: String, password: String): Result<Unit> {
        return authRepository.login(server, username, password)
    }
}
