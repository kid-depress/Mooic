package com.rcmiku.ncmapi.api.account

import com.rcmiku.ncmapi.api.apiGet
import com.rcmiku.ncmapi.api.apiGetWithCookie
import com.rcmiku.ncmapi.api.ApiResponseWithCookie
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
        val timestamp = System.currentTimeMillis()
        return if (manipulateType == PlayManipulateType.ADD) {
            apiGet("/playlist/tracks", mapOf(
                "op" to "add",
                "pid" to playlistId,
                "tracks" to songIds.joinToString(","),
                "timestamp" to timestamp
            ))
        } else {
            apiGet("/playlist/tracks", mapOf(
                "op" to "del",
                "pid" to playlistId,
                "tracks" to songIds.joinToString(","),
                "timestamp" to timestamp
            ))
        }
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

    suspend fun qrKey(): Result<QrKeyResponse> =
        apiGet("/login/qr/key", mapOf("timestamp" to System.currentTimeMillis()))

    suspend fun qrCreate(key: String): Result<QrCreateResponse> =
        apiGet("/login/qr/create", mapOf("key" to key, "qrimg" to true, "timestamp" to System.currentTimeMillis()))

    suspend fun qrCheck(key: String): Result<QrCheckResponse> =
        apiGet("/login/qr/check", mapOf("key" to key, "timestamp" to System.currentTimeMillis()))

    suspend fun sentCaptcha(phone: String, ctcode: String): Result<ApiCodeResponse> =
        apiGet("/captcha/sent", mapOf("phone" to phone, "ctcode" to ctcode))

    suspend fun loginCellphoneWithCookie(phone: String, captcha: String, ctcode: String): Result<ApiResponseWithCookie<ApiCodeResponse>> =
        apiGetWithCookie("/login/cellphone", mapOf("phone" to phone, "captcha" to captcha, "countrycode" to ctcode))

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
