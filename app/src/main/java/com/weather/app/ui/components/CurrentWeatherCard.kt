package com.weather.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.WeatherData
import com.weather.app.model.WeatherSourceInfo

/**
 * 实时天气核心卡片组件
 *
 * 集中展示当前城市、实时温度、天气现象、温差范围、空气质量标签与数据源切换入口。
 *
 * @param weatherData 聚合天气数据模型 [WeatherData]
 * @param currentSource 当前生效的天气数据源元数据 [WeatherSourceInfo]
 * @param onCityClick 点击城市名称时的回调
 * @param onSourceClick 点击天气源切换入口时的回调
 * @param modifier 外部修饰符
 */
@Composable
fun CurrentWeatherCard(
    weatherData: WeatherData,
    currentSource: WeatherSourceInfo,
    onCityClick: () -> Unit,
    onSourceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val current = weatherData.current
    val city = weatherData.city
    val todayForecast = weatherData.dailyForecasts.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部操作栏：城市名称与数据源标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 城市选择区
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onCityClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (city.isAutoLocated) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "当前定位",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = city.name,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal
                )
                if (city.province.isNotEmpty() && city.province != city.name) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = city.province,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }

            // 天气源切换入口芯片
            Surface(
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.clickable { onSourceClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "源: ${currentSource.name}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "切换天气源",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 天气主视觉：Emoji + 巨幅温度 (加粗) + 现象描述
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = WeatherIcons.getWeatherEmoji(current.weatherText),
                fontSize = 52.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = current.getFormattedTemp(),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            text = current.weatherText,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 温差范围与体感温度
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (todayForecast != null) {
                Text(
                    text = "最高 ${todayForecast.maxTemperature.toInt()}°  最低 ${todayForecast.minTemperature.toInt()}°",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            if (current.feelsLike != null) {
                Text(
                    text = "体感 ${current.feelsLike.toInt()}°",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 空气质量胶囊标签
        weatherData.airQuality?.let { aqi ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(aqi.getAqiColor())
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "AQI ${aqi.aqi} ${aqi.qualityText}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 数据源与发布时间说明
        Text(
            text = "${weatherData.sourceName} ${current.publishTime.substringAfter(" ")} 发布",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}
