package com.lalakiop.embyx.domain.usecase

import com.lalakiop.embyx.data.repository.VideoRepository

/**
 * 设置视频收藏状态 用例
 */
class SetFavoriteUseCase(
    // 注入视频仓库
    private val repository: VideoRepository
) {
    /**
     * 执行收藏/取消收藏操作
     * @param itemId 视频项ID
     * @param favorite true=收藏, false=取消收藏
     * @return 操作结果
     */
    suspend operator fun invoke(itemId: String, favorite: Boolean): Result<Unit> {
        // 调用仓库层修改收藏状态
        return repository.setFavorite(itemId = itemId, favorite = favorite)
    }
}