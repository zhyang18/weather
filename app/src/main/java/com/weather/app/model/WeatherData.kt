package com.weather.app.model

/**
 * 完整天气数据集模型
 *
 * 聚合指定城市的实况天气、每日预报、小时趋势、空气质量与数据源属性。
 *
 * @property city 关联的城市信息
 * @property current 实时天气数据
 * @property dailyForecasts 7日预报列表
 * @property hourlyForecasts 24小时历史与走势列表
 * @property airQuality 空气质量信息 (可选)
 * @property sourceName 提供本条数据的天气源名称
 * @property updateTimestamp 数据获取时间戳 (毫秒)
 */
data class WeatherData(
    val city: CityInfo,
    val current: CurrentWeather,
    val dailyForecasts: List<DailyForecast>,
    val hourlyForecasts: List<HourlyForecast> = emptyList(),
    val airQuality: AirQuality? = null,
    val alert: WeatherAlert? = null,
    val sourceName: String = "中央气象台",
    val updateTimestamp: Long = System.currentTimeMillis()
)
