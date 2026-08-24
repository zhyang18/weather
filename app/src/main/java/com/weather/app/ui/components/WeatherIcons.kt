package com.weather.app.ui.components

/**
 * 天气图标辅助工具类
 */
object WeatherIcons {

    /**
     * 根据天气文本返回直观的天气 Emoji 符号
     *
     * @param weatherText 天气现象描述（如 "晴", "多云", "雷阵雨", "大雪"）
     * @return 对应的天气 Emoji 符号
     */
    fun getWeatherEmoji(weatherText: String): String {
        return when {
            weatherText.contains("晴") -> "☀️"
            weatherText.contains("雷") -> "⛈️"
            weatherText.contains("大雨") || weatherText.contains("暴雨") -> "🌧️"
            weatherText.contains("中雨") || weatherText.contains("小雨") || weatherText.contains("雨") -> "🌦️"
            weatherText.contains("雪") -> "❄️"
            weatherText.contains("阴") -> "☁️"
            weatherText.contains("多云") -> "⛅"
            weatherText.contains("雾") || weatherText.contains("霾") -> "🌫️"
            weatherText.contains("风") || weatherText.contains("沙") -> "🌪️"
            else -> "🌤️"
        }
    }
}
