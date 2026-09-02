package com.weather.app.datasource.seniverse

import android.content.Context
import android.content.SharedPreferences

/**
 * 心知天气 (Seniverse) API 凭据（API 私钥与可选公钥）及网络配置数据模型
 *
 * @property apiKey 心知天气 API 私钥 (Key)，用于请求鉴权
 * @property publicKey 心知天气 API 公钥 (User ID)，用于签名鉴权（可选）
 * @property apiHost API 专属主机域名（默认 "api.seniverse.com"）
 */
data class SeniverseConfig(
    val apiKey: String = "",
    val publicKey: String = "",
    val apiHost: String = DEFAULT_API_HOST
) {
    companion object {
        /** 默认心知天气 API 基础域名 */
        const val DEFAULT_API_HOST: String = "api.seniverse.com"

        /** 心知天气公开演示与体验 Key */
        const val DEFAULT_DEMO_KEY: String = "S5r4_O-d8J4aX5-uJ"

        /**
         * 辅助清理凭据文本中的首尾空白及误复制的单双引号
         *
         * @param raw 原始输入字符串
         * @return 清理后的字符串
         */
        fun cleanCredential(raw: String): String {
            return raw.replace("‘", "")
                .replace("’", "")
                .replace("'", "")
                .replace("\"", "")
                .replace("“", "")
                .replace("”", "")
                .replace("`", "")
                .replace(" ", "")
                .replace("\n", "")
                .replace("\r", "")
                .replace("\t", "")
                .trim()
        }
    }

    /**
     * 判断当前 API 凭据是否已配置有效私钥
     *
     * @return true 表示已配置有效私钥，false 表示为空
     */
    fun isConfigured(): Boolean {
        val cleanKey = cleanCredential(apiKey)
        return cleanKey.isNotBlank()
    }

    /**
     * 获取实际生效的 API 私钥
     *
     * @return 用于 API 请求的私钥字符串
     */
    fun getEffectiveApiKey(): String {
        val cleanKey = cleanCredential(apiKey)
        return cleanKey.ifEmpty { DEFAULT_DEMO_KEY }
    }

    /**
     * 判断是否启用了公钥+私钥签名认证模式
     *
     * @return true 表示同时配置了公钥与私钥
     */
    fun isSignatureAuthEnabled(): Boolean {
        return cleanCredential(apiKey).isNotBlank() && cleanCredential(publicKey).isNotBlank()
    }

    /**
     * 获取规范化的 API Base URL（确保以 https:// 开头并以 / 结尾）
     *
     * @return 格式化后的完整 URL 字符串
     */
    fun getFormattedApiBaseUrl(): String {
        val host = apiHost.trim().removePrefix("https://").removePrefix("http://").removeSuffix("/")
        return if (host.isNotEmpty()) "https://$host/" else "https://$DEFAULT_API_HOST/"
    }
}

/**
 * 心知天气本地 API 凭据配置持久化管理器
 *
 * 负责将心知天气的 API Key、Public Key 与网络 Host 保存至 Android SharedPreferences 中。
 *
 * @param context Android 上下文实例 [Context]
 */
class SeniverseConfigManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("seniverse_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "seniverse_api_key"
        private const val KEY_PUBLIC_KEY = "seniverse_public_key"
        private const val KEY_API_HOST = "seniverse_api_host"
    }

    /**
     * 从本地存储中读取当前心知天气 API 凭据配置
     *
     * @return 当前持久化的心知天气配置实体 [SeniverseConfig]
     */
    fun getConfig(): SeniverseConfig {
        val savedApiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        val savedPublicKey = prefs.getString(KEY_PUBLIC_KEY, "") ?: ""
        val savedHost = prefs.getString(KEY_API_HOST, SeniverseConfig.DEFAULT_API_HOST) ?: SeniverseConfig.DEFAULT_API_HOST

        return SeniverseConfig(
            apiKey = SeniverseConfig.cleanCredential(savedApiKey),
            publicKey = SeniverseConfig.cleanCredential(savedPublicKey),
            apiHost = savedHost
        )
    }

    /**
     * 持久化保存心知天气 API 凭据配置
     *
     * @param config 待保存的心知天气配置实体 [SeniverseConfig]
     */
    fun saveConfig(config: SeniverseConfig) {
        prefs.edit()
            .putString(KEY_API_KEY, SeniverseConfig.cleanCredential(config.apiKey))
            .putString(KEY_PUBLIC_KEY, SeniverseConfig.cleanCredential(config.publicKey))
            .putString(KEY_API_HOST, config.apiHost.trim())
            .apply()
    }
}
