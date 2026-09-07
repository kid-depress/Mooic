package com.rcmiku.ncmapi.api.comment

import com.rcmiku.ncmapi.api.API_BASE_URL
import com.rcmiku.ncmapi.api.apiClient
import com.rcmiku.ncmapi.model.Comment
import com.rcmiku.ncmapi.model.CommentResponse
import com.rcmiku.ncmapi.utils.json
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

object CommentApi {

    /**
     * 获取歌曲评论，兼容不同 NeteaseCloudMusicApi 部署版本的返回结构：
     * - 新版：{"code":200,"data":{"comments":[...],"totalCount":N,"hasMore":true}}（评论在 data 内）
     * - 旧版：{"comments":[...],"hotComments":[...],"total":N}（顶层直接返回）
     * 先尝试 /comment/new，失败时回退到 /comment/music。
     */
    suspend fun songComments(
        songId: Long,
        pageSize: Int = 20,
        pageNo: Int = 1,
        sortType: Int = 3
    ): Result<CommentResponse> {
        val newApiResult = runCatching {
            val response = apiClient.request("$API_BASE_URL/comment/new") {
                method = HttpMethod.Get
                parameter("type", 0)
                parameter("id", songId)
                parameter("pageSize", pageSize)
                parameter("pageNo", pageNo)
                parameter("sortType", sortType)
                parameter("timestamp", System.currentTimeMillis())
                parameter("randomCNIP", true)
            }
            parse(response)
        }
        if (newApiResult.isSuccess) {
            return newApiResult
        }

        return runCatching {
            val response = apiClient.request("$API_BASE_URL/comment/music") {
                method = HttpMethod.Get
                parameter("id", songId)
                parameter("timestamp", System.currentTimeMillis())
                parameter("randomCNIP", true)
            }
            parse(response)
        }
    }

    private suspend fun parse(response: HttpResponse): CommentResponse {
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw Exception("HTTP ${response.status.value}: ${body.take(300)}")
        }
        return normalize(body)
    }

    internal fun normalize(body: String): CommentResponse {
        val root = json.parseToJsonElement(body).jsonObject
        val data = root["data"] as? JsonObject

        // 旧版顶层结构
        val topComments = (root["comments"] as? JsonArray)?.mapNotNull { it as? JsonObject }
            ?.map { json.decodeFromJsonElement(Comment.serializer(), it) }
            .orEmpty()
        val topHotComments = (root["hotComments"] as? JsonArray)?.mapNotNull { it as? JsonObject }
            ?.map { json.decodeFromJsonElement(Comment.serializer(), it) }
            .orEmpty()
        val topTotal = (root["total"] as? JsonPrimitive)?.longOrNull ?: 0

        // 新版 data 包裹结构
        val dataComments = (data?.get("comments") as? JsonArray)?.mapNotNull { it as? JsonObject }
            ?.map { json.decodeFromJsonElement(Comment.serializer(), it) }
            .orEmpty()
        val dataHotComments = (data?.get("hotComments") as? JsonArray)?.mapNotNull { it as? JsonObject }
            ?.map { json.decodeFromJsonElement(Comment.serializer(), it) }
            .orEmpty()
        val dataCurrentComment = (data?.get("currentComment") as? JsonObject)
            ?.let { json.decodeFromJsonElement(Comment.serializer(), it) }
        val dataTotal = (data?.get("totalCount") as? JsonPrimitive)?.longOrNull
            ?: (data?.get("total") as? JsonPrimitive)?.longOrNull
            ?: 0
        val dataHasMore = (data?.get("hasMore") as? JsonPrimitive)?.booleanOrNull ?: false

        val comments = dataComments.ifEmpty { topComments }
        val hotComments = buildList {
            addAll(topHotComments)
            addAll(dataHotComments)
            dataCurrentComment?.let { add(it) }
        }
        val total = if (dataTotal > 0) dataTotal else topTotal

        return CommentResponse(
            comments = comments,
            hotComments = hotComments,
            total = total,
            hasMore = dataHasMore
        )
    }
}
