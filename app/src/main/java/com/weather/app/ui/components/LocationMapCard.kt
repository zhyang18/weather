package com.weather.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.CityInfo
import com.weather.app.model.WeatherData
import com.weather.app.ui.map.MapLibreComposeView

/**
 * 首页定位气象微缩地图卡片组件
 *
 * 采用 MapLibre Native 原生地图硬件加速渲染：
 * 1. 统一 152.dp 标准高度与深灰蓝质感，与其他卡片保持严格一致的视觉层级；
 * 2. 嵌入轻量只读 MapLibre 原生地图视窗，自动跟随当前聚焦城市地理坐标与图层偏好；
 * 3. 中心叠加动态微光呼吸定位点；
 * 4. 点击卡片触发 [onClick] 回调跳转至全屏定位大地图页面。
 *
 * @param city 当前聚焦的城市实体 [CityInfo]
 * @param weatherData 当前城市聚合气象数据 [WeatherData]
 * @param mapLayerType 持久化记忆的底图图层类型
 * @param onClick 点击卡片跳转全屏大地图的回调
 * @param modifier 外部修饰符
 */
@Composable
fun LocationMapCard(
    city: CityInfo,
    weatherData: WeatherData?,
    mapLayerType: String = "dark",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 优先读取城市自身的经纬度，若为 null 则自动通过全国区划坐标库精确匹配经纬度
    val coords = remember(city) {
        if (city.latitude != null && city.longitude != null) {
            Pair(city.latitude, city.longitude)
        } else {
            com.weather.app.datasource.openmeteo.ChinaCityCoordinates.findCoordinates(city)
                ?: Pair(39.9042, 116.4074)
        }
    }
    val lat = coords.first
    val lng = coords.second

    Box(
        modifier = modifier
            .height(152.dp)
            .graphicsLayer {
                shadowElevation = 0f
                shape = RoundedCornerShape(20.dp)
                clip = true
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 顶部标题栏：微型定位图标 + 标题 + 提示小箭头
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "定位地图",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "定位地图",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "查看大图",
                    tint = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(13.dp)
                )
            }

            // 2. 中间主要内容：微缩原生地图视窗（只读展示，屏蔽内部手势）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141923))
            ) {
                MapLibreComposeView(
                    modifier = Modifier.fillMaxSize(),
                    lat = lat,
                    lng = lng,
                    zoom = 14.5,
                    mapLayerType = mapLayerType,
                    isInteractive = false
                )

                // 正中心叠加脉冲呼吸定位标记
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LocationPulseDot()
                }

                // 覆盖一层全透明层，避免任何手势穿透影响列表滑动
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                )
            }

            // 3. 底部地理坐标说明
            Text(
                text = formatCoordinates(lat, lng),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

/**
 * 动态呼吸脉冲定位圆点微组件
 *
 * 用于在小地图卡片中心呈现定位状态。
 */
@Composable
private fun LocationPulseDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scaleState = infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val alphaState = infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(24.dp)
    ) {
        // 外层扩散光圈 (在 graphicsLayer lambda 中延迟读取 State，彻底避免 Composable 函数每帧重组)
        Box(
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = scaleState.value
                    scaleY = scaleState.value
                    this.alpha = alphaState.value
                }
                .clip(CircleShape)
                .background(Color(0xFF2196F3))
        )
        // 核心实心圆点
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF2196F3))
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

/**
 * 格式化经纬度坐标为简洁的人类可读字符串
 *
 * @param lat 纬度数值
 * @param lng 经度数值
 * @return 格式化后的坐标描述（如 "39.90°N, 116.41°E"）
 */
private fun formatCoordinates(lat: Double, lng: Double): String {
    val latDir = if (lat >= 0) "N" else "S"
    val lngDir = if (lng >= 0) "E" else "W"
    val latVal = kotlin.math.abs(lat)
    val lngVal = kotlin.math.abs(lng)
    return String.format(java.util.Locale.CHINA, "%.2f°%s, %.2f°%s", latVal, latDir, lngVal, lngDir)
}
