package com.lalakiop.embyx.data.repository

import kotlinx.coroutines.flow.Flow
import com.lalakiop.embyx.core.model.Session
import com.lalakiop.embyx.data.local.SessionStore
import com.lalakiop.embyx.data.remote.ApiClientFactory
import com.lalakiop.embyx.data.remote.model.AuthRequest
import com.lalakiop.embyx.domain.repository.AuthRepository
import java.net.ConnectException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class AuthRepositoryImpl(
    private val sessionStore: SessionStore,
    private val apiClientFactory: ApiClientFactory
) : AuthRepository {

    override val sessionFlow: Flow<Session> = sessionStore.sessionFlow

    override suspend fun login(server: String, username: String, password: String): Result<Unit> {
        val normalizedServer = normalizeServer(server)
        if (normalizedServer.isBlank() || username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("请完整填写服务器、用户名和密码"))
        }

        return runCatching {
            val api = apiClientFactory.createAuthApi(normalizedServer)
            val response = api.authenticateByName(
                // ⚠️ 关键修复：伪装成Emby Web客户端
                authorization = "Emby Client=\"Emby Web\", Device=\"Edge Windows\", DeviceId=\"027b1582-1688-4224-b16b-cc7b6ed96ce5\", Version=\"4.9.1.80\"",
                body = AuthRequest(username = username.trim(), password = password)
            )

            if (!response.isSuccessful) {
                val msg = when (response.code()) {
                    401 -> "用户名或密码错误"
                    404 -> "连接失败，请检查服务器地址或端口"
                    else -> "登录失败: HTTP ${response.code()}"
                }
                throw IllegalStateException(msg)
            }

            val body = response.body()
            val token = body?.accessToken.orEmpty()
            val userId = body?.sessionInfo?.userId.orEmpty()
            if (token.isBlank() || userId.isBlank()) {
                throw IllegalStateException("登录失败: 响应缺少 token 或 userId")
            }

            sessionStore.saveSession(
                Session(
                    server = normalizedServer,
                    token = token,
                    userId = userId,
                    username = username.trim()
                )
            )
        }.recoverCatching { throwable ->
            when (throwable) {
                is UnknownHostException -> throw IllegalStateException("网络错误：无法解析服务器地址")
                is ConnectException -> throw IllegalStateException("网络错误：连接被拒绝，请检查端口和服务是否启动")
                is SocketTimeoutException -> throw IllegalStateException("网络错误：连接超时，请检查网络或服务器响应")
                is SSLHandshakeException -> throw IllegalStateException("TLS/证书错误：HTTPS 证书不受信任，可改用 HTTP 或配置受信任证书")
                is IOException -> {
                    val msg = throwable.message.orEmpty()
                    if (msg.contains("CLEARTEXT communication", ignoreCase = true)) {
                        throw IllegalStateException("网络策略限制：请使用 HTTPS，或确认已允许 HTTP 明文访问")
                    }
                    throw IllegalStateException("网络错误：无法连接服务器")
                }
                else -> throw throwable
            }
        }
    }

    private fun normalizeServer(input: String): String {
        val raw = input.trim().trimEnd('/')
        if (raw.isBlank()) return ""
        return if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
            raw
        } else {
            "http://$raw"
        }
    }

    override suspend fun logout() {
        sessionStore.clear()
    }
}
