package com.weather.app.datasource.caiyun

import android.content.Context
import android.content.SharedPreferences

/**
 * 彩云天气凭据与网络配置数据模型
 *
 * @property token 彩云天气开放平台开发者令牌（Token）
 * @property apiHost API 专属主机域名（例如 "api.caiyunapp.com"）
 */
data class CaiyunConfig(
    val token: String = DEFAULT_TOKEN,
    val apiHost: String = DEFAULT_API_HOST
) {
    companion object {
        /** 默认彩云天气 API 基础域名 */
        const val DEFAULT_API_HOST: String = "api.caiyunapp.com"

        /** 内置演示与兜底 Token（彩云开放平台公开测试 Token） */
        const val DEFAULT_TOKEN: String = "TAkhjf8d1nv5svNu"
    }

    /**
     * 判断当前凭据是否已配置
     *
     * @return true 表示已配置有效 Token，false 表示为空
     */
    fun isConfigured(): Boolean {
        return token.isNotBlank()
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
 * 彩云天气本地配置持久化管理器
 *
 * 负责将彩云天气的 Token 与网络 Host 保存至 Android SharedPreferences 中。
 *
 * @param context Android 上下文实例 [Context]
 */
class CaiyunConfigManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("caiyun_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "caiyun_token"
        private const val KEY_API_HOST = "caiyun_api_host"
    }

    /**
     * 从本地存储中读取当前彩云天气配置
     *
     * @return 当前持久化的彩云天气配置实体 [CaiyunConfig]
     */
    fun getConfig(): CaiyunConfig {
        val savedToken = prefs.getString(KEY_TOKEN, null)
        val savedHost = prefs.getString(KEY_API_HOST, CaiyunConfig.DEFAULT_API_HOST) ?: CaiyunConfig.DEFAULT_API_HOST
        return CaiyunConfig(
            token = savedToken ?: CaiyunConfig.DEFAULT_TOKEN,
            apiHost = savedHost
        )
    }

    /**
     * 持久化保存彩云天气配置
     *
     * @param config 待保存的彩云天气配置实体 [CaiyunConfig]
     */
    fun saveConfig(config: CaiyunConfig) {
        prefs.edit()
            .putString(KEY_TOKEN, config.token.trim())
            .putString(KEY_API_HOST, config.apiHost.trim())
            .apply()
    }
}
