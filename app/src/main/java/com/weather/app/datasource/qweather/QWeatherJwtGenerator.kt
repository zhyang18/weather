package com.weather.app.datasource.qweather

import com.google.crypto.tink.subtle.Ed25519Sign
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference

/**
 * 缓存的 JWT 实体
 *
 * @property token 序列化后的 JWT 字符串
 * @property expiresAtMillis 过期时间戳（毫秒）
 * @property configHash 生成该 Token 时所对应的配置哈希值
 */
private data class CachedToken(
    val token: String,
    val expiresAtMillis: Long,
    val configHash: Int
)

/**
 * 和风天气 JSON Web Token (JWT) 身份认证生成器
 *
 * 严格基于和风天气官方 API 规范，使用 **EdDSA (Ed25519)** 数字签名算法生成带有 Project ID (`sub`) 和 Key ID (`kid`) 的安全访问凭据，
 * 并提供高性能 Token 缓存池，避免高频请求重复进行非对称签名运算。
 */
object QWeatherJwtGenerator {

    /** 内存中缓存的最新 Token 实体 */
    private val cachedTokenRef = AtomicReference<CachedToken?>(null)

    /** Token 默认有效期：900 秒（15 分钟） */
    private const val TOKEN_VALIDITY_SECONDS = 900L

    /** 提前刷新缓冲时间：120 秒（2 分钟） */
    private const val REFRESH_BUFFER_SECONDS = 120L

    /** 时钟容差回拨：30 秒（抵消客户端与服务端微小时钟差异） */
    private const val CLOCK_SKEW_SECONDS = 30L

    /**
     * 根据传入的和风天气配置生成合法的 JWT Bearer Token
     *
     * @param config 和风天气配置信息 [QWeatherConfig]
     * @return 符合和风天气规范的 JWT 字符串（格式：`Header.Payload.Signature`）
     * @throws IllegalArgumentException 当配置项为空或私钥格式非法时抛出异常
     */
    @Synchronized
    fun generateToken(config: QWeatherConfig): String {
        val projectId = config.projectId.trim()
        val keyId = config.keyId.trim()
        val privateKey = config.privateKeyPem.trim()

        if (projectId.isEmpty() || keyId.isEmpty() || privateKey.isEmpty()) {
            throw IllegalArgumentException("和风天气凭据未完整配置，请填写 Project ID、Key ID 及 Private Key")
        }

        val configHash = projectId.hashCode() xor keyId.hashCode() xor privateKey.hashCode()
        val nowMillis = System.currentTimeMillis()

        // 1. 尝试从缓存中获取有效 Token
        val currentCached = cachedTokenRef.get()
        if (currentCached != null &&
            currentCached.configHash == configHash &&
            currentCached.expiresAtMillis - nowMillis > REFRESH_BUFFER_SECONDS * 1000L
        ) {
            return currentCached.token
        }

        // 2. 构造符合和风天气规范的 Header 与 Payload
        val nowSeconds = nowMillis / 1000L
        val iat = nowSeconds - CLOCK_SKEW_SECONDS
        val exp = nowSeconds + TOKEN_VALIDITY_SECONDS

        // 和风天气官方 Header: {"alg":"EdDSA","kid":"<KEY_ID>"}
        val headerJson = """{"alg":"EdDSA","kid":"$keyId"}"""
        // 和风天气官方 Payload: {"sub":"<PROJECT_ID>","iat":<iat>,"exp":<exp>}
        val payloadJson = """{"sub":"$projectId","iat":$iat,"exp":$exp}"""

        val encoder = Base64.getUrlEncoder().withoutPadding()
        val headerEncoded = encoder.encodeToString(headerJson.toByteArray(StandardCharsets.UTF_8))
        val payloadEncoded = encoder.encodeToString(payloadJson.toByteArray(StandardCharsets.UTF_8))
        val signingInput = "$headerEncoded.$payloadEncoded"

        // 3. 解析私钥并使用 Google Tink 进行 Ed25519 签名
        val seedBytes = extractEd25519SeedBytes(privateKey)
        val signer = Ed25519Sign(seedBytes)
        val signatureBytes = signer.sign(signingInput.toByteArray(StandardCharsets.UTF_8))
        val signatureEncoded = encoder.encodeToString(signatureBytes)

        val tokenString = "$signingInput.$signatureEncoded"

        // 4. 更新缓存
        cachedTokenRef.set(
            CachedToken(
                token = tokenString,
                expiresAtMillis = (exp * 1000L),
                configHash = configHash
            )
        )

        return tokenString
    }

    /**
     * 清除内存中缓存的 JWT Token
     */
    fun clearCache() {
        cachedTokenRef.set(null)
    }

    /**
     * 从各种常见格式的私钥文本中提取 32 字节原始 Ed25519 私钥种子 (Seed)
     *
     * 支持包含/不包含 PEM 头部标头的文本、Base64 编码的 PKCS#8 格式字节流、32 字节原始私钥种子及 64 字节密钥组合。
     *
     * @param pemOrBase64 私钥 PEM 文本或 Base64 字符串
     * @return 32 字节私钥种子字节数组
     * @throws IllegalArgumentException 当私钥格式无法识别或长度不合法时抛出异常
     */
    fun extractEd25519SeedBytes(pemOrBase64: String): ByteArray {
        val clean = pemOrBase64
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN ED25519 PRIVATE KEY-----", "")
            .replace("-----END ED25519 PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\"", "")
            .replace("'", "")
            .replace("\\s".toRegex(), "")
            .trim()

        if (clean.isEmpty()) {
            throw IllegalArgumentException("私钥内容为空")
        }

        val decoded = try {
            Base64.getDecoder().decode(clean)
        } catch (_: Exception) {
            try {
                Base64.getMimeDecoder().decode(clean)
            } catch (e2: Exception) {
                try {
                    Base64.getUrlDecoder().decode(clean)
                } catch (e3: Exception) {
                    throw IllegalArgumentException("私钥 Base64 解码失败: ${e3.message}", e3)
                }
            }
        }

        return when (decoded.size) {
            // 32 字节：原始 Ed25519 私钥种子 (Raw Seed)
            32 -> decoded

            // 48 字节：标准 PKCS#8 封装格式 (ASN.1 前缀 16 字节: 302e020100300506032b657004220420 + 32 字节私钥种子)
            48 -> decoded.copyOfRange(16, 48)

            // 64 字节：包含 32 字节私钥种子与 32 字节公钥的组合
            64 -> decoded.copyOfRange(0, 32)

            // 其它 ASN.1 衍生格式：查找 0x04 0x20 (OCTET STRING 32 bytes)
            else -> {
                if (decoded.size > 32) {
                    val candidate = findEd25519SeedInAsn1(decoded)
                    if (candidate != null && candidate.size == 32) {
                        return candidate
                    }
                    // 截取最后 32 字节兜底
                    decoded.copyOfRange(decoded.size - 32, decoded.size)
                } else {
                    throw IllegalArgumentException("无效的 Ed25519 私钥长度: ${decoded.size} 字节（期望为 32、48 或 64 字节）")
                }
            }
        }
    }

    /**
     * 在 ASN.1 字节流中扫描定位 32 字节 Ed25519 私钥种子
     *
     * @param data ASN.1 编码字节流
     * @return 提取出的 32 字节种子，若未找到则返回 null
     */
    private fun findEd25519SeedInAsn1(data: ByteArray): ByteArray? {
        for (i in 0 until data.size - 34) {
            if (data[i] == 0x04.toByte() && data[i + 1] == 0x20.toByte()) {
                return data.copyOfRange(i + 2, i + 34)
            }
        }
        return null
    }
}
