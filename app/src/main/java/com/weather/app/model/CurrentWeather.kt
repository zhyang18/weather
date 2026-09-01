package com.weather.app.model

import androidx.compose.runtime.Immutable

/**
 * 实时天气数据模型
 *
 * 记录当前时刻的气象实况指标。
 * 显式声明为 [Immutable] 实体，确保 Compose 编译器能够对其智能跳过不必要的父级重组。
 *
 * @property temperature 当前气温，单位为摄氏度 (°C)
 * @property feelsLike 体感温度，单位为摄氏度 (°C)
 * @property weatherText 天气现象描述（如 "晴", "多云", "雷阵雨" 等）
 * @property weatherIconCode 天气图标标识编码
 * @property humidity 相对湿度百分比（如 65 表示 65%）
 * @property windDirection 风向描述（如 "东北风", "南风"）
 * @property windPower 风力等级（如 "3~4级", "微风"）
 * @property windSpeed 风速，单位为 m/s
 * @property pressure 气压值，单位为 hPa
 * @property precipitation 降水量，单位为 mm
 * @property uvIndex 紫外线指数数值（0~11+，可选）
 * @property visibility 能见度距离，单位为公里 (km)（可选）
 * @property publishTime 气象中心数据发布时间（如 "2026-08-21 14:00"）
 */
@Immutable
data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double? = null,
    val weatherText: String,
    val weatherIconCode: String = "",
    val humidity: Double = 0.0,
    val windDirection: String = "",
    val windPower: String = "",
    val windSpeed: Double = 0.0,
    val pressure: Double = 0.0,
    val precipitation: Double = 0.0,
    val uvIndex: Double? = null,
    val visibility: Double? = null,
    val publishTime: String = ""
) {
    /**
     * 获取格式化后的温度文本
     *
     * @return 带有摄氏度符号的温度字符串，如 "28°"
     */
    fun getFormattedTemp(): String {
        return "${temperature.toInt()}°"
    }

    /**
     * 获取格式化后的风向风力描述
     *
     * @return 整合的风力描述，如 "东北风 3~4级"
     */
    fun getFormattedWind(): String {
        return if (windPower.isNotEmpty()) {
            "$windDirection $windPower"
        } else {
            "$windDirection ${windSpeed}m/s"
        }
    }
}

