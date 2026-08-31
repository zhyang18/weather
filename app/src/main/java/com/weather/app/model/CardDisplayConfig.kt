package com.weather.app.model

import androidx.compose.runtime.Immutable

/**
 * 界面天气卡片显示与隐藏配置实体
 *
 * 用于支持用户在设置中自定义首页中各项天气卡片（预警、分钟降水、逐时预报、近日预报以及各项详细气象指标）的开启与隐藏展示。
 * 显式声明为 [Immutable] 实体，确保 Compose 编译器能够对其智能跳过不必要的父级重组。
 *
 * @property showWeatherAlert 是否展示官方气象灾害预警卡片（默认 true）
 * @property showMinutelyPrecipitation 是否展示 2 小时分钟级短时降水预测走势卡片（默认 true）
 * @property showHourlyForecast 是否展示 24 小时逐时预报卡片（默认 true）
 * @property showDailyForecast 是否展示近日天气预报卡片（默认 true）
 * @property showAirQuality 是否展示空气质量卡片（默认 true）
 * @property showSunriseSunset 是否展示日出日落卡片（默认 true）
 * @property showMoonPhase 是否展示 3D 月相卡片（默认 true）
 * @property showFeelsLike 是否展示体感温度卡片（默认 true）
 * @property showWind 是否展示风向风速卡片（默认 true）
 * @property showHumidity 是否展示相对湿度卡片（默认 true）
 * @property showPressure 是否展示大气压强卡片（默认 true）
 * @property showPrecipitation 是否展示实时降水量卡片（默认 true）
 * @property showLocationMap 是否展示定位小地图卡片（默认 true）
 */
@Immutable
data class CardDisplayConfig(
    val showWeatherAlert: Boolean = true,
    val showMinutelyPrecipitation: Boolean = true,
    val showHourlyForecast: Boolean = true,
    val showDailyForecast: Boolean = true,
    val showAirQuality: Boolean = true,
    val showSunriseSunset: Boolean = true,
    val showMoonPhase: Boolean = true,
    val showFeelsLike: Boolean = true,
    val showWind: Boolean = true,
    val showHumidity: Boolean = true,
    val showPressure: Boolean = true,
    val showPrecipitation: Boolean = true,
    val showLocationMap: Boolean = true
) {

    companion object {
        /**
         * 创建全部开启的默认配置
         *
         * @return 全部卡片均开启的配置对象 [CardDisplayConfig]
         */
        fun allEnabled(): CardDisplayConfig = CardDisplayConfig()

        /**
         * 创建全部隐藏的配置
         *
         * @return 全部卡片均关闭的配置对象 [CardDisplayConfig]
         */
        fun allDisabled(): CardDisplayConfig = CardDisplayConfig(
            showWeatherAlert = false,
            showMinutelyPrecipitation = false,
            showHourlyForecast = false,
            showDailyForecast = false,
            showAirQuality = false,
            showSunriseSunset = false,
            showMoonPhase = false,
            showFeelsLike = false,
            showWind = false,
            showHumidity = false,
            showPressure = false,
            showPrecipitation = false,
            showLocationMap = false
        )
    }

    /**
     * 拷贝并切换指定卡片的显示状态
     *
     * @param cardId 卡片唯一标识 Key
     * @param enabled 是否显示该卡片
     * @return 更新后的新配置对象 [CardDisplayConfig]
     */
    fun withCardToggled(cardId: String, enabled: Boolean): CardDisplayConfig {
        return when (cardId) {
            KEY_WEATHER_ALERT -> copy(showWeatherAlert = enabled)
            KEY_MINUTELY_PRECIPITATION -> copy(showMinutelyPrecipitation = enabled)
            KEY_HOURLY_FORECAST -> copy(showHourlyForecast = enabled)
            KEY_DAILY_FORECAST -> copy(showDailyForecast = enabled)
            KEY_AIR_QUALITY -> copy(showAirQuality = enabled)
            KEY_SUNRISE_SUNSET -> copy(showSunriseSunset = enabled)
            KEY_MOON_PHASE -> copy(showMoonPhase = enabled)
            KEY_FEELS_LIKE -> copy(showFeelsLike = enabled)
            KEY_WIND -> copy(showWind = enabled)
            KEY_HUMIDITY -> copy(showHumidity = enabled)
            KEY_PRESSURE -> copy(showPressure = enabled)
            KEY_PRECIPITATION -> copy(showPrecipitation = enabled)
            KEY_LOCATION_MAP -> copy(showLocationMap = enabled)
            else -> this
        }
    }
}

/** 官方气象预警卡片键名 */
const val KEY_WEATHER_ALERT = "weather_alert"
/** 2小时分钟级短时降水预测卡片键名 */
const val KEY_MINUTELY_PRECIPITATION = "minutely_precipitation"
/** 24小时逐时预报卡片键名 */
const val KEY_HOURLY_FORECAST = "hourly_forecast"
/** 近日天气预报卡片键名 */
const val KEY_DAILY_FORECAST = "daily_forecast"
/** 空气质量卡片键名 */
const val KEY_AIR_QUALITY = "air_quality"
/** 日出日落卡片键名 */
const val KEY_SUNRISE_SUNSET = "sunrise_sunset"
/** 3D 月相卡片键名 */
const val KEY_MOON_PHASE = "moon_phase"
/** 体感温度卡片键名 */
const val KEY_FEELS_LIKE = "feels_like"
/** 风向风速卡片键名 */
const val KEY_WIND = "wind"
/** 相对湿度卡片键名 */
const val KEY_HUMIDITY = "humidity"
/** 大气压强卡片键名 */
const val KEY_PRESSURE = "pressure"
/** 实时降水量卡片键名 */
const val KEY_PRECIPITATION = "precipitation"
/** 定位地图卡片键名 */
const val KEY_LOCATION_MAP = "location_map"
