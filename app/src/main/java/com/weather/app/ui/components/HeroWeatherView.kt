package com.weather.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.WeatherData

/**
 * 主界面核心温度与现象居中展示视图
 *
 * 集中展示顶部巨幅实时温度（加粗突出）、温差范围与空气质量/天气状况说明（常规字重）。
 *
 * @param weatherData 聚合天气数据模型 [WeatherData]
 * @param modifier 外部修饰符
 */
@Composable
fun HeroWeatherView(
    weatherData: WeatherData,
    modifier: Modifier = Modifier
) {
    val current = weatherData.current
    val todayForecast = weatherData.dailyForecasts.firstOrNull()
    val aqi = weatherData.airQuality

    val maxT = todayForecast?.maxTemperature?.toInt() ?: current.temperature.toInt()
    val minT = todayForecast?.minTemperature?.toInt() ?: (current.temperature.toInt() - 7)
    val aqiText = if (aqi != null && aqi.qualityText.isNotEmpty() && aqi.qualityText != "-") "空气${aqi.qualityText}" else "空气优"

    // 当实时实况 weatherText 缺失或为 "-" 时，自动取当天预测详情的天气现象
    val displayWeatherText = when {
        current.weatherText.isNotEmpty() && current.weatherText != "-" && current.weatherText != "无" && current.weatherText != "9999" -> current.weatherText
        todayForecast?.dayWeatherText?.isNotEmpty() == true && todayForecast.dayWeatherText != "-" && todayForecast.dayWeatherText != "9999" -> todayForecast.dayWeatherText
        todayForecast?.nightWeatherText?.isNotEmpty() == true && todayForecast.nightWeatherText != "-" && todayForecast.nightWeatherText != "9999" -> todayForecast.nightWeatherText
        else -> "多云"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 巨幅主温度展示（常规字重不加粗，字号小一号至 92sp，带清晰立体文字阴影）
        Text(
            text = "${current.temperature.toInt()}°",
            style = TextStyle(
                fontSize = 92.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.38f),
                    offset = Offset(0f, 3f),
                    blurRadius = 8f
                )
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 最高温与最低温 (常规字重，增强阴影)
        Text(
            text = "最高 $maxT° 最低 $minT°",
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.95f),
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.38f),
                    offset = Offset(0f, 2f),
                    blurRadius = 5f
                )
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 空气质量与天气描述 (常规字重，增强阴影)
        Text(
            text = "$aqiText  $displayWeatherText",
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.95f),
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.38f),
                    offset = Offset(0f, 2f),
                    blurRadius = 5f
                )
            )
        )
    }
}

