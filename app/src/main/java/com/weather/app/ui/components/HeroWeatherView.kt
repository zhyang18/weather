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
    val aqiText = if (aqi != null) "空气${aqi.qualityText}" else "空气优"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 巨幅温度数字 (仅当前温度保留加粗，增强阴影保护)
        Text(
            text = "${current.temperature.toInt()}°",
            style = TextStyle(
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
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
            text = "$aqiText  ${current.weatherText}",
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

