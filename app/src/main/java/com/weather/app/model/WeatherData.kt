package com.weather.app.model

import androidx.compose.runtime.Immutable

/**
 * 完整天气数据集模型
 *
 * 聚合指定城市的实况天气、每日预报、小时趋势、空气质量与数据源属性。
 * 显式声明为 [Immutable] 实体，确保 Compose 编译器能够对其智能跳过不必要的父级重组。
 *
 * @property city 关联的城市信息
 * @property current 实时天气数据
 * @property dailyForecasts 7日预报列表
 * @property hourlyForecasts 24小时历史与走势列表
 * @property airQuality 空气质量信息 (可选)
 * @property alert 官方气象预警数据 (可选)
 * @property lifeIndex 生活气象指数数据 (可选)
 * @property sourceName 提供本条数据的天气源名称
 * @property updateTimestamp 数据获取时间戳 (毫秒)
 */
@Immutable
data class WeatherData(
    val city: CityInfo,
    val current: CurrentWeather,
    val dailyForecasts: List<DailyForecast>,
    val hourlyForecasts: List<HourlyForecast> = emptyList(),
    val airQuality: AirQuality? = null,
    val alert: WeatherAlert? = null,
    val lifeIndex: LifeIndex? = null,
    val sourceName: String = "中央气象台",
    val updateTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * 获取用于界面展示的天气现象描述文本（自动规范化繁体中文）
     *
     * @return 格式化后的天气现象描述文本（如“晴”、“多云”等）
     */
    fun getDisplayWeatherText(): String {
        val raw = when {
            current.weatherText.isNotEmpty() && current.weatherText != "-" && current.weatherText != "无" && current.weatherText != "9999" -> current.weatherText
            dailyForecasts.firstOrNull()?.dayWeatherText?.isNotEmpty() == true && dailyForecasts.firstOrNull()?.dayWeatherText != "-" && dailyForecasts.firstOrNull()?.dayWeatherText != "9999" -> dailyForecasts.first().dayWeatherText
            dailyForecasts.firstOrNull()?.nightWeatherText?.isNotEmpty() == true && dailyForecasts.firstOrNull()?.nightWeatherText != "-" && dailyForecasts.firstOrNull()?.nightWeatherText != "9999" -> dailyForecasts.first().nightWeatherText
            else -> "多云"
        }
        return raw.normalizeWeatherText()
    }
}

/**
 * 天气与气象文本繁简体转换与归一化工具类
 *
 * 专门适配港澳台城市、海外华人地区及各国际气象数据源返回的繁体中文气象描述，
 * 统一映射为标准气象关键词，确保天空背景渲染、动态粒子分类、本地矢量图标与生活指数无缝适配。
 */
object WeatherTextNormalizer {

    /**
     * 常用天气现象与修饰词繁体字至规范简体字对照表
     */
    private val TRAD_TO_SIMP_MAP: Map<Char, Char> = mapOf(
        '陰' to '阴',
        '雲' to '云',
        '陣' to '阵',
        '風' to '风',
        '霧' to '雾',
        '電' to '电',
        '颱' to '台',
        '塵' to '尘',
        '揚' to '扬',
        '凍' to '冻',
        '夾' to '夹',
        '轉' to '转',
        '間' to '间',
        '氣' to '气',
        '溫' to '温',
        '濕' to '湿',
        '強' to '强',
        '優' to '优',
        '驟' to '骤',
        '飄' to '飘',
        '濛' to '蒙',
        '綿' to '绵',
        '極' to '极',
        '輕' to '轻',
        '微' to '微',
        '涼' to '凉'
    )

    /**
     * 将繁体天气相关文本快速转换为规范简体中文
     *
     * 适用于所有港澳台及国际数据源返回的繁体气象描述（如“多雲”、“陰”、“雷陣雨”、“大風”等），
     * 确保视觉层、分类器与图标映射统一准确。
     *
     * @param text 原始输入天气文本
     * @return 转换后的简体中文天气文本
     */
    fun normalize(text: String): String {
        if (text.isEmpty()) return text
        var hasTrad = false
        for (ch in text) {
            if (TRAD_TO_SIMP_MAP.containsKey(ch)) {
                hasTrad = true
                break
            }
        }
        if (!hasTrad) return text

        val sb = StringBuilder(text.length)
        for (ch in text) {
            sb.append(TRAD_TO_SIMP_MAP[ch] ?: ch)
        }
        return sb.toString()
    }
}

/**
 * 字符串天气文本归一化扩展函数
 *
 * @return 归一化后的简体天气文本
 */
fun String.normalizeWeatherText(): String = WeatherTextNormalizer.normalize(this)


