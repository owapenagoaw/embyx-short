package com.lalakiop.embyx.core.model

data class ResolvedPlaybackStream(
    val streamUrl: String,
    val mediaSourceId: String?,
    val playSessionId: String? = null  // ⚠️ 新增：服务器生成的会话ID
)
