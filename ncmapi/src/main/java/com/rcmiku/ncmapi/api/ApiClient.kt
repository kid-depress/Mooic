package com.rcmiku.ncmapi.api

import android.util.Log
import com.rcmiku.ncmapi.model.ApiCodeResponse
import com.rcmiku.ncmapi.utils.CookieKeys
import com.rcmiku.ncmapi.utils.CookieProvider
import com.rcmiku.ncmapi.utils.NeteaseClientConfig
import com.rcmiku.ncmapi.utils.json as apiJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder

var API_BASE_URL = "https://ncm-api.prod.gbclstudio.cn"
var UNBLOCK_BASE_URL = "https://unlock.depresskid.top"

val apiClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(apiJson)
    }
    defaultRequest {
        header("User-Agent", NeteaseClientConfig.USER_AGENT)
        header("Accept", "application/json")
        header("Cache-Control", "no-cache, no-store, max-age=0")
        header("Pragma", "no-cache")
        val cookie = CookieProvider.cookie
        if (cookie.isNotEmpty()) {
            header("Cookie", cookie)
        }
    }
}

suspend inline fun <reified T> apiGet(path: String, params: Map<String, Any> = emptyMap()): Result<T> {
    return runCatching {
        val response = apiClient.request("$API_BASE_URL$path") {
            method = HttpMethod.Get
            val finalParams = params.toMutableMap().apply {
                put("timestamp", System.currentTimeMillis())
                putIfAbsent("randomCNIP", true)
            }
            finalParams.forEach { (key, value) ->
                parameter(key, value)
            }
        }
        val responseBody = response.bodyAsText()
        response.requireSuccess(responseBody)
        apiJson.decodeFromString<T>(responseBody)
    }
}

data class ApiResponseWithCookie<T>(
    val data: T,
    val cookie: String
)

suspend inline fun <reified T> apiGetWithCookie(path: String, params: Map<String, Any> = emptyMap()): Result<ApiResponseWithCookie<T>> {
    return try {
        val response = apiClient.request("$API_BASE_URL$path") {
            method = HttpMethod.Get
            params.forEach { (key, value) ->
                parameter(key, value)
            }
        }
        if (response.status.isSuccess()) {
            val headerCookies = response.headers.getAll("Set-Cookie").orEmpty()
                .map { it.substringBefore(';').trim() }
                .filter { it.contains('=') }
            val body = response.bodyAsText()
            val allCookies = if (headerCookies.isNotEmpty()) {
                headerCookies.joinToString("; ")
            } else {
                extractBodyCookie(body)
            }
            try {
                val result = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                }.decodeFromString<T>(body)
                Result.success(ApiResponseWithCookie(data = result, cookie = allCookies))
            } catch (e: Exception) {
                Result.success(ApiResponseWithCookie(data = ApiCodeResponse(code = 200) as T, cookie = allCookies))
            }
        } else {
            Result.failure(Exception("API error: ${response.status}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

@PublishedApi
internal fun extractBodyCookie(body: String): String {
    return try {
        val element = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }.parseToJsonElement(body)
        val cookie = (element as? JsonObject)?.get("cookie")
        if (cookie is JsonPrimitive && cookie.content.isNotBlank()) {
            cookie.jsonPrimitive.content
        } else {
            ""
        }
    } catch (e: Exception) {
        ""
    }
}

@PublishedApi
internal fun encodeForm(value: Any): String =
    URLEncoder.encode(value.toString(), "UTF-8")

suspend inline fun <reified T> apiPost(path: String, body: Map<String, Any> = emptyMap()): Result<T> {
    return runCatching {
        val response = apiClient.request("$API_BASE_URL$path") {
            method = HttpMethod.Post
            contentType(ContentType.Application.FormUrlEncoded)
            parameter("timestamp", System.currentTimeMillis())
            parameter("_", System.nanoTime())
            parameter("randomCNIP", true)
            val finalBody = body.toMutableMap().apply {
                CookieProvider.getCookieMap()[CookieKeys.CSRF]
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { put("csrf_token", it) }
            }
            setBody(
                finalBody.entries.joinToString("&") { (key, value) ->
                    "${key.encodeURLParameter()}=${value.toString().encodeURLParameter()}"
                }
            )
        }
        val responseBody = response.bodyAsText()
        response.requireSuccess(responseBody)
        apiJson.decodeFromString<T>(responseBody)
    }
}

@PublishedApi
internal fun HttpResponse.requireSuccess(responseBody: String) {
    if (!status.isSuccess()) {
        val description = status.description
            .takeUnless { it.isBlank() || it.equals("unknown", ignoreCase = true) }
            ?.let { " $it" }
            .orEmpty()
        throw Exception("HTTP ${status.value}$description: ${responseBody.take(500)}")
    }
}