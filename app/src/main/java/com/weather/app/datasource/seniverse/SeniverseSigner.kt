package com.weather.app.datasource.seniverse

import okhttp3.HttpUrl
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 心知天气 (Seniverse) 官方请求签名与鉴权拦截辅助工具
 *
 * 遵循心知天气官方规范：
 * 1. 当仅配置 API 私钥 (Key) 时，直接在 Query 参数中附加 `key={apiKey}`；
 * 2. 当同时配置 API 私钥与公钥 (Public Key / UID) 时，生成 `ts`、`ttl`、`uid` 与 `sig` 进行 HMAC-SHA1 签名鉴权。
 */
object SeniverseSigner {

    private const val HMAC_SHA1_ALGORITHM = "HmacSHA1"

    /**
     * 为 OkHttp 请求动态添加心知天气鉴权参数（私钥或公钥签名）
     *
     * @param originalRequest 原始网络请求实体 [Request]
     * @param config 心知天气凭据配置实体 [SeniverseConfig]
     * @return 注入鉴权参数后的新请求实体 [Request]
     */
    fun signRequest(originalRequest: Request, config: SeniverseConfig): Request {
        val apiKey = SeniverseConfig.cleanCredential(config.apiKey)
        val publicKey = SeniverseConfig.cleanCredential(config.publicKey)

        val urlBuilder = originalRequest.url.newBuilder()

        // 1. 若启用了公钥+私钥签名鉴权
        if (apiKey.isNotEmpty() && publicKey.isNotEmpty()) {
            val ts = Instant.now().epochSecond.toString()
            val ttl = "1800" // 30 分钟有效期
            val stringToSign = "ts=$ts&ttl=$ttl&uid=$publicKey"
            val signature = calculateHmacSha1(stringToSign, apiKey)

            urlBuilder.addQueryParameter("ts", ts)
            urlBuilder.addQueryParameter("ttl", ttl)
            urlBuilder.addQueryParameter("uid", publicKey)
            urlBuilder.addQueryParameter("sig", signature)
        } else {
            // 2. 默认私钥鉴权模式
            val effectiveKey = config.getEffectiveApiKey()
            if (originalRequest.url.queryParameter("key") == null && effectiveKey.isNotEmpty()) {
                urlBuilder.addQueryParameter("key", effectiveKey)
            }
        }

        return originalRequest.newBuilder()
            .url(urlBuilder.build())
            .header("User-Agent", "WeatherApp/1.0 (Android; Seniverse-Client)")
            .build()
    }

    /**
     * 计算 HMAC-SHA1 签名（Base64 编码）
     *
     * @param data 待签名的原文
     * @param secret 用于签名的私钥
     * @return Base64 编码的签名字符串
     */
    fun calculateHmacSha1(data: String, secret: String): String {
        val signingKey = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), HMAC_SHA1_ALGORITHM)
        val mac = Mac.getInstance(HMAC_SHA1_ALGORITHM)
        mac.init(signingKey)
        val rawHmac = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(rawHmac).trim()
    }
}
