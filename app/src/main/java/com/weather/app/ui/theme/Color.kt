package com.weather.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.weather.app.model.normalizeWeatherText

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// 天气主题常用色
val WeatherBluePrimary = Color(0xFF1E88E5)
val WeatherBlueDark = Color(0xFF1565C0)
val WeatherBlueLight = Color(0xFF64B5F6)
val WeatherCardBg = Color(0x33FFFFFF)
val WeatherCardBorder = Color(0x4DFFFFFF)

/**
 * 根据天气现象获取主背景渐变色画刷
 *
 * @param weatherText 天气现象描述文本（如 "晴", "多云", "雨", "雪"）
 * @param isNight 是否为夜间
 * @return 渐变背景画刷 [Brush]
 */
fun getWeatherBackgroundBrush(weatherText: String, isNight: Boolean = false): Brush {
    val norm = weatherText.normalizeWeatherText()
    return if (isNight) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0F2027),
                Color(0xFF203A43),
                Color(0xFF2C5364)
            )
        )
    } else when {
        norm.contains("晴") -> Brush.verticalGradient(
            listOf(
                Color(0xFF2193b0),
                Color(0xFF6dd5ed)
            )
        )
        norm.contains("云") || norm.contains("阴") -> Brush.verticalGradient(
            listOf(
                Color(0xFF4A6572),
                Color(0xFF344955),
                Color(0xFF232F34)
            )
        )
        norm.contains("雨") || norm.contains("雷") -> Brush.verticalGradient(
            listOf(
                Color(0xFF373B44),
                Color(0xFF4286f4)
            )
        )
        norm.contains("雪") -> Brush.verticalGradient(
            listOf(
                Color(0xFF83a4d4),
                Color(0xFFb6fbff)
            )
        )
        else -> Brush.verticalGradient(
            listOf(
                Color(0xFF1E88E5),
                Color(0xFF42A5F5),
                Color(0xFF90CAF9)
            )
        )
    }
}
