package com.weather.app.model

/**
 * 每日天气预报数据模型
 *
 * 记录单日的白天与夜间天气预报详情。
 *
 * @property date 预报日期字符串，格式为 "YYYY-MM-DD"
 * @property dayOfWeek 星期几描述（如 "星期一", "今天", "明天"）
 * @property dayWeatherText 白天天气现象（如 "多云"）
 * @property nightWeatherText 夜间天气现象（如 "雷阵雨"）
 * @property dayIconCode 白天天气图标编码
 * @property nightIconCode 夜间天气图标编码
 * @property maxTemperature 当日最高温度 (°C)
 * @property minTemperature 当日最低温度 (°C)
 * @property windDirection 风向描述
 * @property windPower 风力等级
 * @property precipitation 降水概率或降水量
 */
data class DailyForecast(
    val date: String,
    val dayOfWeek: String,
    val dayWeatherText: String,
    val nightWeatherText: String,
    val dayIconCode: String = "",
    val nightIconCode: String = "",
    val maxTemperature: Double,
    val minTemperature: Double,
    val windDirection: String = "",
    val windPower: String = "",
    val precipitation: Double = 0.0
) {
    /**
     * 获取全天综合天气概况描述
     *
     * @return 若白天与夜间相同则返回单一天气，否则返回 "白天转夜间"（如 "多云转晴"）
     */
    fun getSummaryWeather(): String {
        return if (dayWeatherText == nightWeatherText || nightWeatherText.isEmpty()) {
            dayWeatherText
        } else {
            "$dayWeatherText 转 $nightWeatherText"
        }
    }

    /**
     * 获取格式化后的温度区间范围
     *
     * @return 格式化后的温差文本，如 "24° ~ 32°"
     */
    fun getFormattedTempRange(): String {
        return "${minTemperature.toInt()}° ~ ${maxTemperature.toInt()}°"
    }

    /**
     * 获取短日期文本（如 "8月24日"）
     *
     * @return 格式化后的短日期文本，如 "8月24日"；若解析失败则返回原始 date
     */
    fun getShortDateText(): String {
        return try {
            val parts = date.split("-")
            if (parts.size >= 3) {
                val month = parts[1].toIntOrNull() ?: parts[1]
                val day = parts[2].toIntOrNull() ?: parts[2]
                "${month}月${day}日"
            } else {
                date
            }
        } catch (e: Exception) {
            date
        }
    }
}
