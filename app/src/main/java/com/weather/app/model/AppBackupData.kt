package com.weather.app.model

import com.weather.app.datasource.caiyun.CaiyunConfig
import com.weather.app.datasource.qweather.QWeatherConfig
import com.weather.app.datasource.seniverse.SeniverseConfig

/**
 * 完整应用配置与城市数据备份模型
 *
 * 聚合了应用的所有可持久化状态，包括保存的城市列表、卡片自定义布局、
 * 当前激活的天气数据源、和风/彩云/心知天气开发者凭据、自动更新间隔与各类显示模式。
 *
 * @property version 备份协议版本号（初始为 1）
 * @property timestamp 备份生成的毫秒级时间戳
 * @property appVersion 生成备份时的应用版本名（如 "1.4.0"）
 * @property savedCities 用户已保存的全部城市列表
 * @property cardDisplayConfig 首页卡片组件显隐与排序配置实体
 * @property activeSourceId 当前选中的天气数据源唯一标识（如 "cma", "qweather", "caiyun", "seniverse"）
 * @property qWeatherConfig 和风天气凭据与网络配置实体
 * @property caiyunConfig 彩云天气凭据与网络配置实体
 * @property seniverseConfig 心知天气凭据与网络配置实体
 * @property autoUpdateIntervalMinutes 后台自动刷新间隔分钟数
 * @property locationDisplayMode 定位名称展示模式（地标/街道 或 区县）
 * @property isDailyChartMode 近日天气展示模式（折线图表 或 列表）
 */
data class AppBackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "1.4.0",
    val savedCities: List<CityInfo> = emptyList(),
    val cardDisplayConfig: CardDisplayConfig = CardDisplayConfig(),
    val activeSourceId: String = "cma",
    val qWeatherConfig: QWeatherConfig = QWeatherConfig(),
    val caiyunConfig: CaiyunConfig = CaiyunConfig(),
    val seniverseConfig: SeniverseConfig = SeniverseConfig(),
    val autoUpdateIntervalMinutes: Int = 60,
    val locationDisplayMode: LocationDisplayMode = LocationDisplayMode.DISTRICT,
    val isDailyChartMode: Boolean = true
) {
    /**
     * 获取格式化的备份生成时间描述文本
     *
     * @return 格式化后的时间字符串（如 "2026-08-28 16:30:00"）
     */
    fun getFormattedDate(): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        } catch (e: Exception) {
            timestamp.toString()
        }
    }

    /**
     * 校验当前备份数据的有效性与完整性
     *
     * @return true 表示数据结构合法，false 表示存在异常或空结构
     */
    fun isValid(): Boolean {
        return version >= 1 && (savedCities.isNotEmpty() || activeSourceId.isNotBlank())
    }
}
