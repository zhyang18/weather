package com.weather.app.model

/**
 * 小时级/历史实况走势数据模型
 *
 * 记录单小时的气象实况或预报指标。
 *
 * @property time 时间字符串（如 "14:00" 或 "2026-08-21 14:00"）
 * @property temperature 当前小时温度 (°C)
 * @property humidity 相对湿度百分比 (%)
 * @property windDirection 风向描述
 * @property windSpeed 风速 (m/s)
 * @property rain 降水量 (mm)
 * @property pressure 气压值 (hPa)
 */
data class HourlyForecast(
    val time: String,
    val temperature: Double,
    val humidity: Double = 0.0,
    val windDirection: String = "",
    val windSpeed: Double = 0.0,
    val rain: Double = 0.0,
    val pressure: Double = 0.0
) {
    /**
     * 获取显示给用户的时间格式（提取 HH:mm）
     *
     * @return 格式化后的简短时间文本，如 "14:00"
     */
    fun getDisplayHour(): String {
        return if (time.contains(" ")) {
            time.substringAfter(" ")
        } else {
            time
        }
    }
}
