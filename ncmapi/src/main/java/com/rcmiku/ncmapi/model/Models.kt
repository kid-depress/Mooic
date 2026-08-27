package com.rcmiku.ncmapi.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement

// ========== Core models ==========

@Serializable
data class Artist(
    val id: Long = 0,
    val name: String = ""
)

@Serializable
data class SongAlbum(
    val id: Long = 0,
    val name: String = "",
    @SerialName("picUrl") val picUrl: String = "",
    val pic: Long = 0
)

@Serializable
data class Song(
    val id: Long = 0,
    val name: String = "",
    val ar: List<Artist> = emptyList(),
    val al: SongAlbum = SongAlbum(),
    val dt: Long = 0,
    val mv: Long = 0,
    val alia: List<String> = emptyList(),
    val fee: Int = 0,
    val privilege: SongPrivilege? = null
)

@Serializable
data class SongPrivilege(
    val st: Int = 0,
    val pl: Int = 0,
    val dl: Int = 0,
    val fl: Int = 0,
    val maxbr: Int = 0
)

@Serializable
data class Album(
    val id: Long = 0,
    val name: String = "",
    @SerialName("picUrl") val picUrl: String = "",
    val artist: Artist = Artist(),
    val artists: List<Artist> = emptyList(),
    val size: Int = 0,
    @SerialName("publishTime") val publishTime: Long = 0,
    val company: String? = null,
    val description: String = ""
)

@Serializable
data class SubAlbum(
    val id: Long = 0,
    val name: String = "",
    @SerialName("picUrl") val picUrl: String = "",
    val artist: Artist? = null,
    val artists: List<Artist> = emptyList(),
    val size: Int = 0,
    @SerialName("subTime") val subTime: Long = 0
)

@Serializable
data class Playlist(
    val id: Long = 0,
    val name: String = "",
    @SerialName("coverImgUrl") val coverImgUrl: String = "",
    @SerialName("picUrl") val rawPicUrl: String = "",
    @SerialName("playCount") val playCount: Double = 0.0,
    @SerialName("trackCount") val trackCount: Int = 0,
    val creator: PlaylistCreator? = null,
    val description: String = "",
    @SerialName("createTime") val createTime: Long = 0
) {
    val picUrl: String get() = coverImgUrl.ifBlank { rawPicUrl }
}

@Serializable
data class PlaylistCreator(
    val userId: Long = 0,
    val nickname: String = "",
    @SerialName("avatarUrl") val avatarUrl: String? = null
)

@Serializable
data class PlaylistV1(
    val id: Long = 0,
    val name: String = "",
    @SerialName("coverImgUrl") val coverImgUrl: String = "",
    @SerialName("trackCount") val trackCount: Int = 0,
    @SerialName("containTracks") val containsTracks: Boolean = false,
    @SerialName("playCount") val playCount: Double = 0.0,
    val creator: PlaylistCreator? = null,
    val description: String = ""
)

@Serializable
data class VoiceBaseInfo(
    val id: Long = 0,
    val name: String = "",
    @SerialName("coverUrl") val coverUrl: String = "",
    @SerialName("picUrl") val rawPicUrl: String = "",
    @SerialName("programCount") val programCount: Int = 0
) {
    val picUrl: String get() = coverUrl.ifBlank { rawPicUrl }
}

@Serializable
data class Radio(
    val id: Long = 0,
    val name: String = "",
    @SerialName("coverUrl") val coverUrl: String = "",
    @SerialName("mainSong") val mainSong: RadioMainSong = RadioMainSong(),
    val dj: RadioDj? = null,
    @SerialName("programCount") val programCount: Int = 0,
    @SerialName("createTime") val createTime: Long = 0
) {
    val picUrl: String get() = coverUrl
}

@Serializable
data class RadioMainSong(
    val id: Long = 0,
    val name: String = "",
    val artists: List<Artist> = emptyList(),
    val duration: Long = 0
)

@Serializable
data class RadioDj(
    val nickname: String = "",
    @SerialName("avatarUrl") val avatarUrl: String = ""
)

// ========== Cloud Song models ==========

@Serializable
data class CloudSong(
    @SerialName("simpleSong") val simpleSong: SimpleSong = SimpleSong(),
    val artist: String = "",
    val album: String = "",
    @SerialName("songId") val songId: Long = 0,
    @SerialName("fileSize") val fileSize: Long = 0
)

@Serializable
data class SimpleSong(
    val id: Long = 0,
    val name: String = "",
    val al: SongAlbum? = null,
    val ar: List<Artist> = emptyList(),
    val dt: Long = 0
)

// ========== Search models ==========

@Serializable
data class SearchArtist(
    val id: Long = 0,
    val name: String = "",
    @SerialName("picUrl") val picUrl: String = "",
    @SerialName("albumSize") val albumSize: Int = 0,
    @SerialName("musicSize") val musicSize: Int = 0,
    val briefDesc: String = ""
) {
    val cover: String get() = picUrl
}

@Serializable
data class SearchUser(
    @SerialName("userId") val id: Long = 0,
    val nickname: String = "",
    @SerialName("avatarUrl") val avatarUrl: String = "",
    val signature: String = ""
)

data class SearchResources(
    val song: Song? = null,
    val album: Album? = null,
    val artist: SearchArtist? = null,
    val playlist: Playlist? = null,
    val user: SearchUser? = null,
    val voice: VoiceBaseInfo? = null,
    val baseInfo: VoiceBaseInfo? = null
)

fun SearchResources.toAlbumList(): Album? = album
fun SearchResources.toSearchArtist(): SearchArtist? = artist
fun SearchResources.toPlaylist(): Playlist? = playlist
fun SearchResources.toSearchUser(): SearchUser? = user

data class SearchData(
    val resources: List<SearchResources> = emptyList(),
    val more: Boolean = false
)

data class SearchResultWrapper(
    val data: SearchData = SearchData()
)

// ========== Response wrappers ==========

@Serializable
data class AlbumDetailResponse(
    val songs: List<Song> = emptyList(),
    val album: Album = Album()
)

@Serializable
data class AlbumInfoResponse(
    val album: Album = Album(),
    @SerialName("isSub") val isSub: Boolean = false
)

@Serializable
data class ArtistHeadInfoResponse(
    val data: ArtistHeadData = ArtistHeadData()
)

@Serializable
data class ArtistHeadData(
    val artist: SearchArtist = SearchArtist(),
    val user: ArtistUser? = null,
    val briefDesc: String = ""
)

@Serializable
data class ArtistUser(
    val followed: Boolean = false
)

@Serializable
data class ArtistTopSong(
    val songs: List<Song> = emptyList()
)

@Serializable
data class ArtistAlbumResponse(
    @SerialName("hotAlbums") val hotAlbums: List<Album> = emptyList(),
    val more: Boolean = false
)

@Serializable
data class LyricResponse(
    val lrc: LyricContent? = null,
    val tlyric: LyricContent? = null,
    val klyric: LyricContent? = null,
    val romalrc: LyricContent? = null
)

@Serializable
data class LyricContent(
    val lyric: String = ""
)

@Serializable
data class SongUrlResponse(
    val data: List<SongUrl> = emptyList()
)

@Serializable
data class SongUrl(
    val id: Long = 0,
    val url: String? = null,
    val br: Int = 0,
    val type: String? = null,
    val level: String? = null,
    val size: Long = 0,
    val time: Long = 0,
    val fee: Int = 0,
    val freeTrialInfo: JsonElement? = null
)

@Serializable
data class PlaylistDetailResponse(
    val playlist: PlaylistDetail = PlaylistDetail()
)

@Serializable
data class PlaylistDetail(
    val id: Long = 0,
    val name: String = "",
    @SerialName("coverImgUrl") val coverImgUrl: String = "",
    val tracks: List<Song> = emptyList(),
    val songs: List<Song> = emptyList(),
    val creator: PlaylistCreator? = null,
    val description: String = "",
    @SerialName("trackCount") val trackCount: Int = 0,
    @SerialName("trackUpdateTime") val trackUpdateTime: Long = 0,
    @SerialName("playCount") val playCount: Double = 0.0
) {
    fun getAllTracks(): List<Song> = tracks.ifEmpty { songs }
}

@Serializable
data class PlaylistInfoResponse(
    val playlist: Playlist = Playlist(),
    val subscribed: Boolean = false
)

@Serializable
data class TopListResponse(
    val list: List<Playlist> = emptyList()
)

@Serializable
data class RadioInfoResponse(
    val data: RadioInfoData = RadioInfoData()
)

@Serializable
data class RadioInfoData(
    val id: Long = 0,
    val name: String = "",
    @SerialName("coverUrl") val coverUrl: String = "",
    val dj: RadioDj? = null,
    @SerialName("programCount") val programCount: Int = 0,
    @SerialName("subCount") val subCount: Long = 0,
    @SerialName("playCount") val playCount: Double = 0.0,
    val desc: String? = null
) {
    val picUrl: String get() = coverUrl
}

@Serializable
data class ProgramRadioResponse(
    val data: ProgramRadioData = ProgramRadioData()
)

@Serializable
data class ProgramRadioData(
    val programs: List<Radio> = emptyList(),
    val more: Boolean = false
)

@Serializable
data class DailySongsResponse(
    val data: DailySongsData = DailySongsData()
)

@Serializable
data class DailySongsData(
    @SerialName("dailySongs") val dailySongs: List<Song> = emptyList()
)

@Serializable
data class PersonalizedPlaylistResponse(
    val result: List<Playlist> = emptyList()
)

@Serializable
data class RecommendPlaylistResponse(
    val result: List<Playlist> = emptyList(),
    val recommend: List<Playlist> = emptyList()
)

@Serializable
data class NewAlbumResponse(
    val albums: List<Album> = emptyList(),
    val weekData: List<Album> = emptyList(),
    val monthData: List<Album> = emptyList()
)

@Serializable
data class SearchSuggestKeywordResponse(
    val data: SearchSuggestData? = null
)

@Serializable
data class SearchSuggestData(
    val suggests: List<SearchSuggestItem> = emptyList()
)

@Serializable
data class SearchSuggestItem(
    val keyword: String = ""
)

@Serializable
data class CloudSongResponse(
    val data: List<CloudSong> = emptyList(),
    @SerialName("hasMore") val hasMore: Boolean = false
)

@Serializable
data class AlbumSublistResponse(
    val data: List<SubAlbum> = emptyList(),
    @SerialName("hasMore") val hasMore: Boolean = false
)

@Serializable
data class SongRecordResponse(
    @SerialName("weekData") val weekData: List<RecordSong> = emptyList(),
    @SerialName("allData") val allData: List<RecordSong> = emptyList()
)

@Serializable
data class RecordSong(
    @SerialName("playCount") val playCount: Int = 0,
    val song: Song = Song()
)

typealias RecordResponse = SongRecordResponse

@Serializable
data class UserInfoBatch(
    val account: UserAccount = UserAccount(),
    val profile: UserProfile = UserProfile(),
    val level: UserLevel = UserLevel()
)

@Serializable
data class UserAccount(
    val id: Long = 0,
    val profile: UserProfile = UserProfile()
)

@Serializable
data class UserProfile(
    val userId: Long = 0,
    val nickname: String = "",
    @SerialName("avatarUrl") val avatarUrl: String = "",
    val signature: String = "",
    val vipType: Int = 0
)

@Serializable
data class UserLevel(
    val data: UserLevelData = UserLevelData()
)

@Serializable
data class UserLevelData(
    val level: Int = 0
)

@Serializable
data class FavoriteSongResponse(
    val data: FavoriteSongData? = null,
    val ids: List<Long> = emptyList()
)

@Serializable
data class FavoriteSongData(
    val id: Long = 0
)

@Serializable
data class UserPlaylistResponse(
    val data: UserPlaylistData = UserPlaylistData()
)

@Serializable
data class UserPlaylistData(
    val playlist: List<Playlist> = emptyList()
)

@Serializable
data class UserPlaylistV1Response(
    val playlist: List<PlaylistV1> = emptyList()
)

@Serializable
data class ApiCodeResponse(
    val code: Int = 0,
    val message: String? = null,
    val msg: String? = null
)

// ========== Login models ==========

@Serializable
data class QrKeyResponse(
    val data: QrKeyData = QrKeyData()
)

@Serializable
data class QrKeyData(
    val unikey: String = ""
)

@Serializable
data class QrCreateResponse(
    val data: QrCreateData = QrCreateData()
)

@Serializable
data class QrCreateData(
    val qrurl: String = "",
    val qrimg: String = ""
)

@Serializable
data class QrCheckResponse(
    val code: Int = 0,
    val message: String = "",
    val cookie: String? = null
)
