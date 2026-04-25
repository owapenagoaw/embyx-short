package com.lalakiop.embyx.domain.usecase

import com.lalakiop.embyx.core.model.VideoItem
import com.lalakiop.embyx.data.repository.VideoRepository

/**
 * 获取首页视频流/推荐内容 用例
 * 专门处理视频列表获取的业务逻辑
 */
class GetFeedUseCase(
    // 注入视频仓库，用于获取数据
    private val repository: VideoRepository
) {
    /**
     * 执行获取视频流操作
     * @param parentId 父级分类ID
     * @param random 是否随机获取
     * @param favoritesOnly 是否仅获取收藏视频
     * @param startIndex 分页起始位置
     * @param limit 每页获取数量
     * @param searchTerm 搜索关键词
     * @return 视频列表结果
     */
    suspend operator fun invoke(
        parentId: String? = null,
        random: Boolean = false,
        favoritesOnly: Boolean = false,
        startIndex: Int = 0,
        limit: Int = 99,
        searchTerm: String? = null
    ): Result<List<VideoItem>> {
        // 调用仓库层方法获取视频流数据
        return repository.getFeed(
            parentId = parentId,
            random = random,
            favoritesOnly = favoritesOnly,
            startIndex = startIndex,
            limit = limit,
            searchTerm = searchTerm
        )
    }
}