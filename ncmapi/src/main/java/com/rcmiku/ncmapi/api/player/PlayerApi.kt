package com.rcmiku.ncmapi.api.player

import com.rcmiku.ncmapi.api.API_BASE_URL
import com.rcmiku.ncmapi.api.UNBLOCK_BASE_URL
import com.rcmiku.ncmapi.api.apiClient
import com.rcmiku.ncmapi.api.apiGet
import com.rcmiku.ncmapi.model.LyricResponse
import com.rcmiku.ncmapi.model.SongUrl
import com.rcmiku.ncmapi.model.SongUrlResponse
import com.rcmiku.ncmapi.utils.DebugLog
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object PlayerApi {
    /**
     * Third-party sources supported by UnblockNeteaseMusic-utils.
     * The unblock/match service expects one source name per request.
     */
    private val unblockSources = listOf(
        "byfuns",
        "ddyr",
        "gdmusic",
        "msls",
        "oi",
        "qijieya",
        "unm"
    )

    suspend fun songPlayUrlV1(songId: String, songLevel: SongLevel = SongLevel.STANDARD): Result<SongUrlResponse> {
        val parsed = parseSongId(songId)
        val realId = parsed.id
        DebugLog.append("resolve song=$realId restricted=${parsed.shouldUseApiUnblock()}")

        return if (parsed.shouldUseApiUnblock()) {
            val unblockResult = tryUnblockUrl(realId)
            if (unblockResult.hasPlayableUrl()) {
                return unblockResult
            }

            tryApiEnhancedMatchUrl(realId)
        } else {
            val apiResult = apiGet<SongUrlResponse>(
                "/song/url/v1",
                mapOf("id" to realId, "level" to songLevel.value)
            )
            if (apiResult.hasPlayableUrl()) {
                apiResult
            } else {
                tryUnblockUrl(realId)
            }
        }
    }

    private data class ParsedSongId(
        val id: String,
        val fee: Int = 0,
        val pl: Int? = null,
        val dl: Int? = null,
        val fl: Int? = null,
        val st: Int? = null
    ) {
        fun shouldUseApiUnblock(): Boolean =
            fee == 1 || fee == 4 || fee == 8 || pl == 0 || (st != null && st < 0)
    }

    private fun parseSongId(raw: String): ParsedSongId {
        val queryIndex = raw.indexOf('?')
        if (queryIndex == -1) return ParsedSongId(raw)
        val id = raw.substring(0, queryIndex)
        val query = raw.substring(queryIndex + 1)
        val params = query.split("&").associate {
            val eq = it.indexOf('=')
            if (eq > 0) it.substring(0, eq) to it.substring(eq + 1) else it to ""
        }
        val fee = params["fee"]?.toIntOrNull() ?: 0
        val pl = params["pl"]?.toIntOrNull()
        val dl = params["dl"]?.toIntOrNull()
        val fl = params["fl"]?.toIntOrNull()
        val st = params["st"]?.toIntOrNull()
        return ParsedSongId(id = id, fee = fee, pl = pl, dl = dl, fl = fl, st = st)
    }

    private fun Result<SongUrlResponse>.hasPlayableUrl(): Boolean =
        getOrNull()?.data?.any { it.hasFullPlayableUrl() } == true

    private fun SongUrl.hasFullPlayableUrl(): Boolean {
        val playableUrl = url?.takeIf { it.isNotBlank() } ?: return false
        if (isLikelyTrialUrl(playableUrl)) return false
        if (freeTrialInfo != null && freeTrialInfo !is JsonNull) return false
        if (time in 1..60_000) return false
        return true
    }

    private suspend fun tryApiEnhancedMatchUrl(songId: String): Result<SongUrlResponse> =
        tryMatchedSources("${API_BASE_URL.trimEnd('/')}/song/url/match", songId)

    private suspend fun tryUnblockUrl(songId: String): Result<SongUrlResponse> =
        tryMatchedSources("${UNBLOCK_BASE_URL.trimEnd('/')}/match", songId)

    private suspend fun tryMatchedSources(endpoint: String, songId: String): Result<SongUrlResponse> {
        unblockSources.forEach { source ->
            requestMatchedUrl(endpoint, songId, source)?.let { data ->
                return Result.success(
                    SongUrlResponse(
                        data = listOf(
                            SongUrl(
                                id = songId.toLongOrNull() ?: 0L,
                                url = data.url,
                                br = data.br
                            )
                        )
                    )
                )
            }
        }
        return Result.failure(Exception("No full unlocked URL found"))
    }

    private suspend fun requestMatchedUrl(
        endpoint: String,
        songId: String,
        source: String
    ): UnblockData? {
        val startedAt = System.currentTimeMillis()
        DebugLog.append("request endpoint=$endpoint song=$songId source=$source")
        return try {
            val response = apiClient.request(endpoint) {
                method = HttpMethod.Get
                parameter("id", songId)
                parameter("source", source)
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val data = parseUnblockResponse(body)
                DebugLog.append(
                    "response endpoint=$endpoint song=$songId source=$source " +
                        "success=${data != null} br=${data?.br ?: 0} " +
                        "elapsed=${System.currentTimeMillis() - startedAt}ms"
                )
                data
            } else {
                DebugLog.append("response endpoint=$endpoint song=$songId source=$source http=${response.status.value} elapsed=${System.currentTimeMillis() - startedAt}ms")
                null
            }
        } catch (e: Exception) {
            DebugLog.append("error endpoint=$endpoint song=$songId source=$source type=${e.javaClass.simpleName} message=${e.message?.take(160)} elapsed=${System.currentTimeMillis() - startedAt}ms")
            null
        }
    }

    private data class UnblockData(val url: String, val br: Int)

    private fun parseUnblockResponse(body: String): UnblockData? {
        return try {
            val json = kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
            val jsonObj = json.parseToJsonElement(body).jsonObject
            val code = jsonObj["code"]?.jsonPrimitive?.intOrNull
            if (code != null && code != 200) return null

            val data = extractMatchedData(jsonObj["data"])
            // Prefer the validated matched URL. A top-level proxyUrl may point to a
            // trial/proxy stream and must not bypass the freeTrialInfo/time checks.
            val proxyUrl = jsonObj["proxyUrl"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotBlank() && !isLikelyTrialUrl(it) }
            val playableUrl = data?.url ?: proxyUrl
            playableUrl?.let { UnblockData(it, data?.br ?: 320000) }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractMatchedData(dataElement: JsonElement?): UnblockData? {
        val dataObj = dataElement as? JsonObject
        val dataPrimitive = dataElement as? JsonPrimitive
        val dataArray = dataElement as? JsonArray

        dataObj?.let { return extractUrlFromObject(it) }

        dataPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() && !isLikelyTrialUrl(it) }
            ?.let { return UnblockData(it, 320000) }

        dataArray?.forEach { element ->
            when (element) {
                is JsonObject -> extractUrlFromObject(element)?.let { return it }
                is JsonPrimitive -> element.contentOrNull
                    ?.takeIf { it.isNotBlank() && !isLikelyTrialUrl(it) }
                    ?.let { return UnblockData(it, 320000) }
                else -> Unit
            }
        }

        return null
    }

    private fun extractUrlFromObject(dataObj: JsonObject): UnblockData? {
        if (dataObj["freeTrialInfo"] != null && dataObj["freeTrialInfo"] !is JsonNull) return null

        val url = dataObj["url"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() && !isLikelyTrialUrl(it) }
        val time = dataObj["time"]?.jsonPrimitive?.longOrNull
        val br = dataObj["br"]?.jsonPrimitive?.intOrNull
            ?: dataObj["bitrate"]?.jsonPrimitive?.intOrNull
            ?: 320000

        if (time != null && time in 1..60_000) return null
        return url?.let { UnblockData(it, br) }
    }

    private fun isLikelyTrialUrl(url: String): Boolean =
        url.contains("musicrep-ts", ignoreCase = true)

    suspend fun songLyric(musicId: Long): Result<LyricResponse> =
        apiGet("/lyric", mapOf("id" to musicId))
}
