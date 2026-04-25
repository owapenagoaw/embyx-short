package com.lalakiop.embyx.domain.usecase

import com.lalakiop.embyx.domain.repository.AuthRepository

/**
 * 登录用例
 * 处理用户登录的业务逻辑
 */
class LoginUseCase(
    // 注入认证仓库
    private val authRepository: AuthRepository
) {
    /**
     * 执行登录操作
     * @param server 服务器地址
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    suspend operator fun invoke(server: String, username: String, password: String): Result<Unit> {
        // 调用仓库层执行登录
        return authRepository.login(server, username, password)
    }
}