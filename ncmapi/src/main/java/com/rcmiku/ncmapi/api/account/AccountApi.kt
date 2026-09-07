package com.rcmiku.ncmapi.api.account

import com.rcmiku.ncmapi.api.apiGet
import com.rcmiku.ncmapi.api.apiPost
import com.rcmiku.ncmapi.api.player.SongLevel
import com.rcmiku.ncmapi.model.*

object AccountApi {

    suspend fun account(): Result<UserInfoBatch> {
        val result = apiGet<UserInfoBatch>("/user/account")
        return result.map { fixAccountProfile(it) }
    }

    suspend fun accountInfo(): Result<UserInfoBatch> {
        val result = apiGet<UserInfoBatch>("/user/account")
        return result.map { fixAccountProfile(it) }
    }

    private fun fixAccountProfile(batch: UserInfoBatch): UserInfoBatch {
        return batch.copy(account = batch.account.copy(profile = batch.profile))
    }

    suspend fun favoriteSong(uid: Long): Result<FavoriteSongResponse> =
        apiGet("/likelist", mapOf("uid" to uid))

    suspend fun favoriteSongIds(): Result<FavoriteSongResponse> =
        apiGet("/likelist")

    suspend fun favoriteSongLikeChange(): Result<ApiCodeResponse> =
        Result.success(ApiCodeResponse(code = 200))

    suspend fun songLike(like: Boolean, songId: Long, userId: Long): Result<ApiCodeResponse> {
        if (userId <= 0) {
            return Result.failure(IllegalStateException("未登录或用户 ID 无效"))
        }

        return apiPost<ApiCodeResponse>(
            "/song/like",
            mapOf("id" to songId, "uid" to userId, "like" to like)
        ).mapCatching {
            if (it.code == 200) {
                it
            } else {
                val reason = it.message?.takeIf(String::isNotBlank)
                    ?: it.msg?.takeIf(String::isNotBlank)
                    ?: "服务端返回业务码 ${it.code}"
                throw IllegalStateException(reason)
            }
        }
    }

    suspend fun songDislike(songId: Long, userId: Long): Result<ApiCodeResponse> =
        songLike(false, songId, userId)

    suspend fun userPlaylist(
        userId: Long,
        userPlaylistType: UserPlaylistType
    ): Result<UserPlaylistResponse> {
        val result = apiGet<UserPlaylistRawResponse>("/user/playlist", mapOf("uid" to userId))
        return result.map { raw ->
            UserPlaylistResponse(data = UserPlaylistData(playlist = raw.playlist.map { it.toPlaylist() }))
        }
    }

    suspend fun userPlaylistV1(
        userId: Long,
        trackIds: List<Long>
    ): Result<UserPlaylistV1Response> {
        val raw = apiGet<UserPlaylistRawResponse>(
            "/user/playlist",
            mapOf("uid" to userId)
        ).getOrThrow()
        val playlistsV1 = raw.playlist.map { item ->
            PlaylistV1(
                id = item.id,
                name = item.name,
                coverImgUrl = item.coverImgUrl,
                trackCount = item.trackCount,
                containsTracks = item.trackIds.any { it in trackIds },
                playCount = item.playCount,
                creator = item.creator,
                description = item.description
            )
        }
        return Result.success(UserPlaylistV1Response(playlist = playlistsV1))
    }

    suspend fun playlistManipulate(
        playlistId: Long,
        songIds: List<Long>,
        manipulateType: PlayManipulateType = PlayManipulateType.ADD
    ): Result<ApiCodeResponse> {
        val result = if (manipulateType == PlayManipulateType.ADD) {
            apiPost<ApiCodeResponse>("/playlist/track/add", mapOf(
                "op" to "add",
                "pid" to playlistId,
                "tracks" to songIds.joinToString(",")
            ))
        } else {
            apiPost<ApiCodeResponse>("/playlist/track/delete", mapOf(
                "op" to "del",
                "pid" to playlistId,
                "tracks" to songIds.joinToString(",")
            ))
        }
        return result.mapCatching {
            if (it.code == 200) {
                it
            } else {
                val reason = it.message?.takeIf(String::isNotBlank)
                    ?: it.msg?.takeIf(String::isNotBlank)
                    ?: "服务端返回业务码 ${it.code}"
                throw IllegalStateException(reason)
            }
        }
    }

    /** 已关注歌手列表 */
    suspend fun artistSublist(limit: Int = 1000): Result<List<ArtistSublistItem>> =
        apiGet<ArtistSublistResponse>("/artist/sublist", mapOf("limit" to limit))
            .map { it.data.list }

    /** 关注/取关歌手（sub=true 关注） */
    suspend fun artistSub(id: Long, sub: Boolean): Result<ApiCodeResponse> =
        apiPost<ApiCodeResponse>("/artist/sub", mapOf("id" to id, "sub" to if (sub) 1 else 0))
            .mapCatching { it.requireCode200() }

    /** 已收藏专辑列表 */
    suspend fun albumSubList(limit: Int = 1000): Result<List<SubAlbum>> =
        apiGet<AlbumSubscribeListResponse>("/album/sublist", mapOf("limit" to limit))
            .map { it.data.list }

    /** 收藏/取消收藏专辑（sub=true 收藏） */
    suspend fun albumSub(id: Long, sub: Boolean): Result<ApiCodeResponse> =
        apiPost<ApiCodeResponse>("/album/sub", mapOf("id" to id, "sub" to if (sub) 1 else 0))
            .mapCatching { it.requireCode200() }

    private fun ApiCodeResponse.requireCode200(): ApiCodeResponse {
        if (code == 200) return this
        val reason = message?.takeIf(String::isNotBlank)
            ?: msg?.takeIf(String::isNotBlank)
            ?: "服务端返回业务码 $code"
        throw IllegalStateException(reason)
    }

    suspend fun cloudSong(offset: Int, limit: Int): Result<CloudSongResponse> =
        apiGet("/user/cloud", mapOf("offset" to offset, "limit" to limit))

    suspend fun albumSublist(offset: Int, limit: Int): Result<AlbumSublistResponse> =
        apiGet("/album/sublist", mapOf("offset" to offset, "limit" to limit))

    suspend fun songRecord(uid: Long, type: SongRecordType): Result<RecordResponse> =
        apiGet("/user/record", mapOf("uid" to uid, "type" to type.type))

    suspend fun scrobble(
        songId: Long,
        time: Int,
        total: Int? = null,
        sourceId: Long? = null,
        sourceName: String? = null,
        songName: String? = null,
        artistName: String? = null,
        songLevel: SongLevel? = null
    ): Result<ApiCodeResponse> =
        apiGet(
            "/scrobble/v1",
            buildMap {
                put("id", songId)
                put("time", time.coerceAtLeast(1))
                total?.takeIf { it > 0 }?.let { put("total", it) }
                sourceId?.takeIf { it > 0 }?.let { put("sourceid", it) }
                sourceName?.takeIf { it.isNotBlank() }?.let { put("source", it) }
                songName?.takeIf { it.isNotBlank() }?.let { put("name", it) }
                artistName?.takeIf { it.isNotBlank() }?.let { put("artist", it) }
                songLevel?.let { put("level", it.value) }
                put("timestamp", System.currentTimeMillis())
            }
        )

    @kotlinx.serialization.Serializable
    data class UserPlaylistRawResponse(
        val playlist: List<PlaylistRawItem> = emptyList()
    )

    @kotlinx.serialization.Serializable
    data class PlaylistRawItem(
        val id: Long = 0,
        val name: String = "",
        @kotlinx.serialization.SerialName("coverImgUrl") val coverImgUrl: String = "",
        @kotlinx.serialization.SerialName("trackCount") val trackCount: Int = 0,
        @kotlinx.serialization.SerialName("trackIds") val trackIds: List<Long> = emptyList(),
        @kotlinx.serialization.SerialName("playCount") val playCount: Double = 0.0,
        val creator: PlaylistCreator? = null,
        val description: String = ""
    ) {
        fun toPlaylist() = Playlist(
            id = id,
            name = name,
            coverImgUrl = coverImgUrl,
            trackCount = trackCount,
            playCount = playCount,
            creator = creator,
            description = description
        )
    }
}
