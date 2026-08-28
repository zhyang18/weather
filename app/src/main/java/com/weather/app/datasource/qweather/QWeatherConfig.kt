package com.weather.app.datasource.qweather

import android.content.Context
import android.content.SharedPreferences

/**
 * 和风天气凭据与网络配置数据模型
 *
 * @property projectId 项目 ID（对应 JWT 的 sub 字段）
 * @property keyId 凭据 ID（对应 JWT Header 的 kid 字段）
 * @property privateKeyPem Ed25519 私钥（支持 PKCS#8 PEM 格式或 Base64 编码私钥种子）
 * @property apiHost API 专属主机域名（例如 "devapi.qweather.com" 或 "api.qweather.com"）
 * @property geoHost 地理检索专属主机域名（例如 "geoapi.qweather.com"）
 */
data class QWeatherConfig(
    val projectId: String = "",
    val keyId: String = "",
    val privateKeyPem: String = "",
    val apiHost: String = DEFAULT_API_HOST,
    val geoHost: String = DEFAULT_GEO_HOST
) {
    companion object {
        /** 默认和风天气 API 基础域名（开发版） */
        const val DEFAULT_API_HOST: String = "devapi.qweather.com"

        /** 默认和风天气地理编码检索基础域名 */
        const val DEFAULT_GEO_HOST: String = "geoapi.qweather.com"
    }

    /**
     * 判断当前凭据是否已完整配置
     *
     * @return true 表示已配置关键凭据（Project ID、Key ID 和 Private Key），false 表示未配置
     */
    fun isConfigured(): Boolean {
        return projectId.isNotBlank() && keyId.isNotBlank() && privateKeyPem.isNotBlank()
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

    /**
     * 获取规范化的 GeoAPI Base URL（确保以 https:// 开头并以 / 结尾）
     *
     * 优先使用用户配置的专属 API Host，确保 JWT 身份认证在地理编码接口统一生效。
     *
     * @return 格式化后的地理检索 URL 字符串
     */
    fun getFormattedGeoBaseUrl(): String {
        val host = apiHost.trim().removePrefix("https://").removePrefix("http://").removeSuffix("/")
        if (host.isNotEmpty() && host != DEFAULT_API_HOST) {
            return "https://$host/"
        }
        val customGeo = geoHost.trim().removePrefix("https://").removePrefix("http://").removeSuffix("/")
        return if (customGeo.isNotEmpty()) "https://$customGeo/" else "https://$DEFAULT_GEO_HOST/"
    }
}

/**
 * 和风天气本地配置持久化管理器
 *
 * 负责将和风天气的 JWT 凭据与网络 Host 保存至 Android SharedPreferences 中。
 *
 * @param context Android 上下文实例 [Context]
 */
class QWeatherConfigManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("qweather_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROJECT_ID = "qweather_project_id"
        private const val KEY_KEY_ID = "qweather_key_id"
        private const val KEY_PRIVATE_KEY_PEM = "qweather_private_key_pem"
        private const val KEY_API_HOST = "qweather_api_host"
        private const val KEY_GEO_HOST = "qweather_geo_host"
    }

    /**
     * 从本地存储中读取当前和风天气配置
     *
     * @return 当前持久化的和风天气配置实体 [QWeatherConfig]
     */
    fun getConfig(): QWeatherConfig {
        return QWeatherConfig(
            projectId = prefs.getString(KEY_PROJECT_ID, "") ?: "",
            keyId = prefs.getString(KEY_KEY_ID, "") ?: "",
            privateKeyPem = prefs.getString(KEY_PRIVATE_KEY_PEM, "") ?: "",
            apiHost = prefs.getString(KEY_API_HOST, QWeatherConfig.DEFAULT_API_HOST) ?: QWeatherConfig.DEFAULT_API_HOST,
            geoHost = prefs.getString(KEY_GEO_HOST, QWeatherConfig.DEFAULT_GEO_HOST) ?: QWeatherConfig.DEFAULT_GEO_HOST
        )
    }

    /**
     * 持久化保存和风天气配置
     *
     * @param config 待保存的和风天气配置实体 [QWeatherConfig]
     */
    fun saveConfig(config: QWeatherConfig) {
        prefs.edit()
            .putString(KEY_PROJECT_ID, config.projectId.trim())
            .putString(KEY_KEY_ID, config.keyId.trim())
            .putString(KEY_PRIVATE_KEY_PEM, config.privateKeyPem.trim())
            .putString(KEY_API_HOST, config.apiHost.trim())
            .putString(KEY_GEO_HOST, config.geoHost.trim())
            .apply()
    }
}
