package com.weather.app.datasource.caiyun

import android.content.Context
import android.content.SharedPreferences

/**
 * 彩云天气 API 凭证（AppKey & AppSecret / Token）与网络配置数据模型
 *
 * @property appKey 开放平台应用唯一标识（AppKey）
 * @property appSecret 开放平台应用密钥（AppSecret）
 * @property token 兼容历史版本开发者令牌（Token）
 * @property apiHost API 专属主机域名（默认 "api.caiyunapp.com"）
 */
data class CaiyunConfig(
    val appKey: String = "",
    val appSecret: String = "",
    val token: String = DEFAULT_TOKEN,
    val apiHost: String = DEFAULT_API_HOST
) {
    companion object {
        /** 默认彩云天气 API 基础域名 */
        const val DEFAULT_API_HOST: String = "api.caiyunapp.com"

        /** 内置演示与兜底 Token（彩云开放平台公开测试凭据） */
        const val DEFAULT_TOKEN: String = "TAkhjf8d1nv5svNu"

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
     * 判断当前 API 凭据是否已配置
     *
     * @return true 表示已配置有效凭据，false 表示为空
     */
    fun isConfigured(): Boolean {
        val cleanKey = cleanCredential(appKey)
        val cleanToken = cleanCredential(token)
        return cleanKey.isNotBlank() || cleanToken.isNotBlank()
    }

    /**
     * 获取实际生效的 Key / Token
     *
     * @return 用于 API 请求的 Key 字符串
     */
    fun getEffectiveAuthKey(): String {
        val cleanKey = cleanCredential(appKey)
        val cleanToken = cleanCredential(token)
        return cleanKey.ifEmpty { cleanToken.ifEmpty { DEFAULT_TOKEN } }
    }

    /**
     * 判断是否启用了 AppKey & AppSecret 官方签名认证
     *
     * @return true 表示同时配置了 AppKey 和 AppSecret
     */
    fun isSignatureAuthEnabled(): Boolean {
        return cleanCredential(appKey).isNotBlank() && cleanCredential(appSecret).isNotBlank()
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
 * 彩云天气本地 API 凭据配置持久化管理器
 *
 * 负责将彩云天气的 AppKey、AppSecret、Token 与网络 Host 保存至 Android SharedPreferences 中。
 *
 * @param context Android 上下文实例 [Context]
 */
class CaiyunConfigManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("caiyun_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_APP_KEY = "caiyun_app_key"
        private const val KEY_APP_SECRET = "caiyun_app_secret"
        private const val KEY_TOKEN = "caiyun_token"
        private const val KEY_API_HOST = "caiyun_api_host"
    }

    /**
     * 从本地存储中读取当前彩云天气 API 凭据配置
     *
     * @return 当前持久化的彩云天气配置实体 [CaiyunConfig]
     */
    fun getConfig(): CaiyunConfig {
        val savedAppKey = prefs.getString(KEY_APP_KEY, "") ?: ""
        val savedAppSecret = prefs.getString(KEY_APP_SECRET, "") ?: ""
        val savedToken = prefs.getString(KEY_TOKEN, "") ?: ""
        val savedHost = prefs.getString(KEY_API_HOST, CaiyunConfig.DEFAULT_API_HOST) ?: CaiyunConfig.DEFAULT_API_HOST

        val effectiveToken = savedToken.ifEmpty { savedAppKey }

        return CaiyunConfig(
            appKey = CaiyunConfig.cleanCredential(savedAppKey),
            appSecret = CaiyunConfig.cleanCredential(savedAppSecret),
            token = CaiyunConfig.cleanCredential(effectiveToken),
            apiHost = savedHost
        )
    }

    /**
     * 持久化保存彩云天气 API 凭据配置
     *
     * @param config 待保存的彩云天气配置实体 [CaiyunConfig]
     */
    fun saveConfig(config: CaiyunConfig) {
        prefs.edit()
            .putString(KEY_APP_KEY, CaiyunConfig.cleanCredential(config.appKey))
            .putString(KEY_APP_SECRET, CaiyunConfig.cleanCredential(config.appSecret))
            .putString(KEY_TOKEN, CaiyunConfig.cleanCredential(config.token))
            .putString(KEY_API_HOST, config.apiHost.trim())
            .apply()
    }
}
