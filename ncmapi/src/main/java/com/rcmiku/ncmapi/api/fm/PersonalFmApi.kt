package com.rcmiku.ncmapi.api.fm

import com.rcmiku.ncmapi.api.apiGet
import com.rcmiku.ncmapi.model.PersonalFmResponse
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.model.SongDetailResponse

object PersonalFmApi {

    /** 获取私人 FM 推荐歌曲列表 */
    suspend fun personalFm(): Result<List<Song>> =
        apiGet<PersonalFmResponse>("/personal_fm")
            .map { it.data.map { song -> song.toSong() } }

    /** 批量获取歌曲详情（用于心动模式等场景） */
    suspend fun songDetail(ids: List<Long>): Result<List<Song>> =
        apiGet<SongDetailResponse>(
            "/song/detail",
            mapOf("ids" to ids.joinToString(","))
        ).map { it.songs }
}
