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
     * 获取用于界面展示的天气现象描述文本
     *
     * @return 格式化后的天气现象描述文本（如“晴”、“多云”等）
     */
    fun getDisplayWeatherText(): String {
        return when {
            current.weatherText.isNotEmpty() && current.weatherText != "-" && current.weatherText != "无" && current.weatherText != "9999" -> current.weatherText
            dailyForecasts.firstOrNull()?.dayWeatherText?.isNotEmpty() == true && dailyForecasts.firstOrNull()?.dayWeatherText != "-" && dailyForecasts.firstOrNull()?.dayWeatherText != "9999" -> dailyForecasts.first().dayWeatherText
            dailyForecasts.firstOrNull()?.nightWeatherText?.isNotEmpty() == true && dailyForecasts.firstOrNull()?.nightWeatherText != "-" && dailyForecasts.firstOrNull()?.nightWeatherText != "9999" -> dailyForecasts.first().nightWeatherText
            else -> "多云"
        }
    }
}

