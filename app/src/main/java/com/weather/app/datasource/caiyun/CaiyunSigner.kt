package com.weather.app.datasource.caiyun

import okhttp3.HttpUrl
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 彩云科技开放平台官方 v3 签名鉴权计算器
 *
 * 严格按照彩云官方文档规范 (https://docs.caiyunapp.com/weather-api/v3/auth.html) 实现：
 * 1. 将 URL Query 参数按字母升序排序并进行 URL 编码；
 * 2. 构造签名原文：{method}:{path}:{query}:{app_key}:{nonce}:{timestamp}；
 * 3. 使用 AppSecret 对签名原文执行 HMAC-SHA256 运算；
 * 4. 使用 URL Safe Base64 对哈希摘要进行编码生成签名；
 * 5. 将 x-cy-app-key、x-cy-nonce、x-cy-timestamp 及 x-cy-signature 注入请求头部。
 */
object CaiyunSigner {

    private const val HMAC_SHA256_ALGORITHM = "HmacSHA256"

    /**
     * 为 OkHttp 请求动态添加符合彩云官方 v3 规范的签名鉴权头部
     *
     * @param originalRequest 原始请求 [Request]
     * @param config 彩云天气配置实体 [CaiyunConfig]
     * @return 注入签名鉴权 Header 后的新请求 [Request]
     */
    fun signRequest(originalRequest: Request, config: CaiyunConfig): Request {
        val appKey = CaiyunConfig.cleanCredential(config.appKey)
        val appSecret = CaiyunConfig.cleanCredential(config.appSecret)

        val builder = originalRequest.newBuilder()
            .header("User-Agent", "WeatherApp/1.0 (Android; Caiyun-Client)")

        // 若未启用 AppKey & AppSecret 签名认证，则直接返回
        if (appKey.isEmpty() || appSecret.isEmpty()) {
            return builder.build()
        }

        val url = originalRequest.url
        val method = originalRequest.method.uppercase()
        val path = url.encodedPath
        val queryStr = buildSortedQueryString(url)

        val nonce = UUID.randomUUID().toString()
        val timestamp = Instant.now().epochSecond.toString()

        // 官方拼接格式：{method}:{path}:{query}:{app_key}:{nonce}:{timestamp}
        val stringToSign = "$method:$path:$queryStr:$appKey:$nonce:$timestamp"

        val signature = calculateHmacSha256(stringToSign, appSecret)

        builder.header("x-cy-app-key", appKey)
        builder.header("x-cy-nonce", nonce)
        builder.header("x-cy-timestamp", timestamp)
        builder.header("x-cy-signature", signature)

        return builder.build()
    }

    /**
     * 对 URL Query 参数按字母升序排序并构造符合官方规范的标准 Query String
     *
     * @param url 请求的 [HttpUrl]
     * @return 排序并 URL 编码后的 Query 字符串（如 "alert=true&dailysteps=15"），若无参数则返回空字符串
     */
    fun buildSortedQueryString(url: HttpUrl): String {
        val paramNames = url.queryParameterNames.toList().sorted()
        if (paramNames.isEmpty()) return ""

        val list = mutableListOf<String>()
        for (name in paramNames) {
            val values = url.queryParameterValues(name)
            val encodedKey = URLEncoder.encode(name, StandardCharsets.UTF_8.name())
            for (value in values) {
                if (value != null) {
                    val encodedVal = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    list.add("$encodedKey=$encodedVal")
                } else {
                    list.add(encodedKey)
                }
            }
        }
        return list.joinToString("&")
    }

    /**
     * 使用 AppSecret 对待签名原文计算 HMAC-SHA256 签名（URL Safe Base64）
     *
     * @param data 待签名字符串
     * @param secret 开放平台 AppSecret
     * @return URL Safe Base64 编码的签名字符串
     */
    fun calculateHmacSha256(data: String, secret: String): String {
        val secretKeySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM)
        val mac = Mac.getInstance(HMAC_SHA256_ALGORITHM)
        mac.init(secretKeySpec)
        val rawHmac = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().encodeToString(rawHmac).trim()
    }
}
