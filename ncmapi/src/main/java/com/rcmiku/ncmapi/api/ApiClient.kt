package com.rcmiku.ncmapi.api

import com.rcmiku.ncmapi.utils.CookieKeys
import com.rcmiku.ncmapi.utils.CookieProvider
import com.rcmiku.ncmapi.utils.NeteaseClientConfig
import com.rcmiku.ncmapi.utils.json as apiJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

var UNBLOCK_BASE_URL = "https://unlock.depresskid.top"

/** Shared HTTP client. Third-party matching requests must not receive the account Cookie. */
val apiClient = HttpClient(OkHttp) {
    defaultRequest {
        header("User-Agent", NeteaseClientConfig.USER_AGENT)
        header("Accept", "application/json")
    }
}

private enum class Encryption { WEAPI, EAPI }
private data class Route(val uri: String, val data: Map<String, Any>, val encryption: Encryption)
private const val WEB_DOMAIN = "https://music.163.com"
private const val API_DOMAIN = "https://interface.music.163.com"
private const val CLIENT_LOG_DOMAIN = "https://clientlog.music.163.com"
private val anonymousDeviceId = UUID.randomUUID().toString().replace("-", "")

private fun route(path: String, p: Map<String, Any>): Route {
    fun required(key: String): Any = requireNotNull(p[key]) { "Missing $key for $path" }
    fun weapi(uri: String, data: Map<String, Any> = emptyMap()) = Route(uri, data, Encryption.WEAPI)
    fun eapi(uri: String, data: Map<String, Any> = emptyMap()) = Route(uri, data, Encryption.EAPI)
    return when (path) {
        "/captcha/sent" -> weapi("/api/sms/captcha/sent", mapOf("cellphone" to required("phone"), "ctcode" to (p["ctcode"] ?: "86")))
        "/login/cellphone" -> weapi("/api/login/cellphone", mapOf("phone" to required("phone"), "captcha" to required("captcha"), "countrycode" to (p["countrycode"] ?: "86"), "rememberLogin" to true))
        "/login/qr/check" -> weapi("/api/login/qrcode/client/login", mapOf("key" to required("key"), "type" to 1))
        "/login/qr/key" -> weapi("/api/login/qrcode/unikey", mapOf("type" to 1))
        "/album" -> weapi("/api/v1/album/${required("id")}")
        "/album/detail/dynamic" -> eapi("/api/album/detail/dynamic", mapOf("id" to required("id")))
        "/album/new" -> weapi("/api/album/new", mapOf("limit" to (p["limit"] ?: 30), "offset" to 0, "total" to true, "area" to "ALL"))
        "/album/sub" -> weapi("/api/album/${if (p["t"].toString() == "1") "sub" else "unsub"}", mapOf("id" to required("id")))
        "/album/sublist" -> weapi("/api/album/sublist", mapOf("limit" to (p["limit"] ?: 25), "offset" to (p["offset"] ?: 0), "total" to true))
        "/artist/album" -> weapi("/api/artist/albums/${required("id")}", mapOf("limit" to (p["limit"] ?: 30), "offset" to (p["offset"] ?: 0), "total" to true))
        "/artist/detail" -> eapi("/api/artist/head/info/get", mapOf("id" to required("id")))
        "/artist/top/song" -> weapi("/api/artist/top/song", mapOf("id" to required("id")))
        "/cloudsearch" -> eapi("/api/cloudsearch/pc", mapOf("s" to required("keywords"), "type" to (p["type"] ?: 1), "limit" to (p["limit"] ?: 30), "offset" to (p["offset"] ?: 0), "total" to true))
        "/dj/detail" -> weapi("/api/djradio/v2/get", mapOf("id" to required("rid")))
        "/dj/program" -> weapi("/api/dj/program/byradio", mapOf("radioId" to required("rid"), "limit" to (p["limit"] ?: 30), "offset" to (p["offset"] ?: 0), "asc" to false))
        "/likelist" -> eapi("/api/song/like/get", p.filterKeys { it == "uid" })
        "/lyric" -> eapi("/api/song/lyric", mapOf("id" to required("id"), "tv" to -1, "lv" to -1, "rv" to -1, "kv" to -1, "_nmclfl" to 1))
        "/personalized" -> weapi("/api/personalized/playlist", mapOf("limit" to (p["limit"] ?: 30), "total" to true, "n" to 1000))
        "/playlist/detail" -> eapi("/api/v6/playlist/detail", mapOf("id" to required("id"), "n" to 100000, "s" to 8))
        "/playlist/detail/dynamic" -> eapi("/api/playlist/detail/dynamic", mapOf("id" to required("id")))
        "/playlist/subscribe" -> eapi("/api/playlist/${if (p["t"].toString() == "1") "subscribe" else "unsubscribe"}", mapOf("id" to required("id")))
        "/playlist/track/add", "/playlist/track/delete" -> {
            val ids = (p["tracks"] ?: p["ids"] ?: "").toString().split(',').filter { it.isNotBlank() }
            val tracks = apiJson.encodeToString(JsonArray.serializer(), JsonArray(ids.map { buildJsonObject { put("type", JsonPrimitive(3)); put("id", JsonPrimitive(it)) } }))
            weapi("/api/playlist/track/${if (path.endsWith("add")) "add" else "delete"}", mapOf("id" to (p["pid"] ?: p["id"] ?: error("Missing playlist ID")), "tracks" to tracks))
        }
        "/recommend/songs" -> weapi("/api/v3/discovery/recommend/songs")
        "/search/suggest" -> weapi("/api/search/suggest/web", mapOf("s" to required("keywords")))
        "/song/like" -> eapi("/api/song/like", mapOf("trackId" to required("id"), "userid" to required("uid"), "like" to required("like")))
        "/song/url/v1" -> eapi("/api/song/enhance/player/url/v1", mapOf("ids" to "[${required("id")}]", "level" to (p["level"] ?: "standard"), "encodeType" to "flac"))
        "/toplist" -> eapi("/api/toplist")
        "/user/account" -> weapi("/api/nuser/account/get")
        "/user/cloud" -> weapi("/api/v1/cloud/get", mapOf("limit" to (p["limit"] ?: 30), "offset" to (p["offset"] ?: 0)))
        "/user/playlist" -> weapi("/api/user/playlist", mapOf("uid" to required("uid"), "limit" to (p["limit"] ?: 30), "offset" to (p["offset"] ?: 0), "includeVideo" to true))
        "/user/record" -> weapi("/api/v1/play/record", mapOf("uid" to required("uid"), "type" to (p["type"] ?: 0)))
        else -> error("Unsupported NetEase route: $path")
    }
}

internal suspend fun sendRawPublic(path: String, params: Map<String, Any>): Pair<String, String> =
    sendRaw(route(path, params))

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is List<*> -> JsonArray(map { it.toJsonElement() })
    is Map<*, *> -> JsonObject(entries.associate { it.key.toString() to it.value.toJsonElement() })
    else -> JsonPrimitive(toString())
}

private fun cookieMap(os: String? = null): Map<String, String> = buildMap {
    putAll(NeteaseClientConfig.cookieOverrides)
    put(CookieKeys.DEVICE_ID, anonymousDeviceId)
    putAll(CookieProvider.getCookieMap())
    if (os != null) put(CookieKeys.OS, os)
}

private suspend fun send(route: Route, os: String? = null, domain: String? = null): String =
    sendRaw(route, os, domain).first

/** Same as [send] but also returns the merged Set-Cookie string, required by the login flows. */
private suspend fun sendRaw(route: Route, os: String? = null, domain: String? = null): Pair<String, String> {
    val cookie = cookieMap(os)
    val csrf = cookie[CookieKeys.CSRF].orEmpty()
    val header = buildMap<String, Any> {
        listOf("osver", "deviceId", "os", "appver", "versioncode", "mobilename", "resolution", "channel").forEach { key ->
            cookie[key]?.let { put(key, it) }
        }
        put("buildver", (System.currentTimeMillis() / 1000).toString())
        put("requestId", "${System.currentTimeMillis()}_${(0..9999).random()}")
        put("__csrf", csrf)
        cookie[CookieKeys.MUSIC_U]?.let { put(CookieKeys.MUSIC_U, it) }
    }
    val data = route.data.toMutableMap()
    if (route.encryption == Encryption.EAPI) data["header"] = header else data["csrf_token"] = csrf
    val json = apiJson.encodeToString(JsonObject.serializer(), data.toJsonElement() as JsonObject)
    val fields = when (route.encryption) {
        Encryption.WEAPI -> NeteaseCrypto.weapi(json)
        Encryption.EAPI -> NeteaseCrypto.eapi(route.uri, json)
    }
    val url = when (route.encryption) {
        Encryption.WEAPI -> (domain ?: WEB_DOMAIN) + "/weapi/" + route.uri.removePrefix("/api/")
        Encryption.EAPI -> (domain ?: API_DOMAIN) + "/eapi/" + route.uri.removePrefix("/api/")
    }
    val response = apiClient.request(url) {
        method = HttpMethod.Post
        contentType(ContentType.Application.FormUrlEncoded)
        if (os == "osx") {
            header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
        }
        header("Cookie", cookie.entries.joinToString("; ") { "${it.key}=${it.value}" })
        if (route.encryption == Encryption.WEAPI) header("Referer", WEB_DOMAIN)
        setBody(fields.entries.joinToString("&") { (key, value) ->
            "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
        })
    }
    val body = response.bodyAsText()
    response.requireSuccess(body)
    val setCookie = response.headers.getAll("Set-Cookie").orEmpty()
        .map { it.substringBefore(';').trim() }
        .filter { it.contains('=') }
        .joinToString("; ")
    return body to setCookie
}

private suspend fun scrobble(p: Map<String, Any>): String {
    val id = requireNotNull(p["id"])
    val sourceId = p["sourceid"] ?: id
    val common = mapOf("id" to id, "type" to "song", "mainsite" to "1", "mainsiteWeb" to "1", "content" to "id=$sourceId")
    val start = mapOf("action" to "startplay", "json" to common)
    val play = mapOf("action" to "play", "json" to common + mapOf("download" to 0, "end" to "playend", "sourceId" to sourceId, "time" to requireNotNull(p["time"]), "wifi" to 0, "source" to "list"))
    for (entry in listOf(start, play)) {
        val logs = apiJson.encodeToString(JsonArray.serializer(), listOf(entry).toJsonElement() as JsonArray)
        val result = send(Route("/api/feedback/weblog", mapOf("logs" to logs), Encryption.EAPI), os = "osx", domain = CLIENT_LOG_DOMAIN)
        val code = (apiJson.parseToJsonElement(result) as? JsonObject)?.get("code")?.jsonPrimitive?.intOrNull
        if (code != 200) error("Scrobble rejected: $result")
    }
    return "{\"code\":200}"
}

suspend fun requestNetease(path: String, params: Map<String, Any> = emptyMap()): String {
    if (path == "/scrobble/v1") return scrobble(params)
    if (path == "/login/qr/create") {
        val key = requireNotNull(params["key"]) { "Missing key for /login/qr/create" }
        val url = "https://music.163.com/login?codekey=$key"
        return apiJson.encodeToString(
            JsonObject.serializer(),
            mapOf("code" to 200, "data" to mapOf("qrurl" to url, "qrimg" to url)).toJsonElement() as JsonObject
        )
    }
    val selected = route(path, params)
    if (path != "/playlist/subscribe") return send(selected)

    // Some accounts are rejected by the desktop endpoint without a fresh anti-cheat token.
    // The web endpoint remains useful as a fallback for a signed-in browser session.
    val desktop = runCatching { send(selected) }
    val desktopBody = desktop.getOrNull()
    val code = desktopBody?.let { body -> runCatching {
        (apiJson.parseToJsonElement(body) as? JsonObject)?.get("code")?.jsonPrimitive?.intOrNull
    }.getOrNull() }
    if (code == 200) return desktopBody
    return send(Route(selected.uri, selected.data, Encryption.WEAPI))
}

suspend inline fun <reified T> apiGet(path: String, params: Map<String, Any> = emptyMap()): Result<T> =
    runCatching { apiJson.decodeFromString<T>(requestNetease(path, params)) }

data class ApiResponseWithCookie<T>(val data: T, val cookie: String)

/** Login endpoints need the raw Set-Cookie header, which [apiGet] discards. */
suspend fun requestNeteaseWithCookie(path: String, params: Map<String, Any> = emptyMap()): Pair<String, String> =
    sendRawPublic(path, params)

suspend inline fun <reified T> apiGetWithCookie(
    path: String,
    params: Map<String, Any> = emptyMap()
): Result<ApiResponseWithCookie<T>> = runCatching {
    val (body, cookie) = requestNeteaseWithCookie(path, params)
    val merged = cookie.ifEmpty { extractBodyCookie(body) }
    ApiResponseWithCookie(apiJson.decodeFromString<T>(body), merged)
}

@PublishedApi
internal fun extractBodyCookie(body: String): String = runCatching {
    (apiJson.parseToJsonElement(body) as? JsonObject)
        ?.get("cookie")
        ?.jsonPrimitive
        ?.content
        .orEmpty()
}.getOrDefault("")

suspend inline fun <reified T> apiPost(path: String, body: Map<String, Any> = emptyMap()): Result<T> =
    apiGet(path, body)

@PublishedApi
internal fun HttpResponse.requireSuccess(responseBody: String) {
    if (!status.isSuccess()) throw Exception("HTTP ${status.value} ${status.description}: ${responseBody.take(500)}")
}
