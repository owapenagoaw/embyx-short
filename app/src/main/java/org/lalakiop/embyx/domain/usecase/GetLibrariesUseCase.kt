package com.lalakiop.embyx.domain.usecase

import com.lalakiop.embyx.core.model.MediaLibrary
import com.lalakiop.embyx.data.repository.VideoRepository

/**
 * 获取媒体库列表 用例
 */
class GetLibrariesUseCase(
    // 注入视频仓库
    private val repository: VideoRepository
) {
    /**
     * 执行获取媒体库操作
     * @return 媒体库列表结果
     */
    suspend operator fun invoke(): Result<List<MediaLibrary>> {
        // 调用仓库层获取媒体库数据
        return repository.getLibraries()
    }
}