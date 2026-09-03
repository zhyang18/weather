package com.weather.app.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.pow

/**
 * 比例尺距离档位候选值列表（米）
 */
private val SCALE_DISTANCES_METERS = doubleArrayOf(
    5.0, 10.0, 20.0, 50.0, 100.0, 200.0, 300.0, 500.0,
    1000.0, 2000.0, 3000.0, 5000.0, 10000.0, 20000.0, 30000.0, 50000.0,
    100000.0, 200000.0, 300000.0, 500000.0, 1000000.0, 2000000.0
)

/**
 * 原生高精度自适应地图比例尺控件
 *
 * 根据当前地图中心点纬度与缩放级别（Zoom）或实测分辨率，动态计算屏幕物理像素与大地实际距离的比率，
 * 精准渲染带有两端标尺端线的透明刻度标尺，支持自适应刻度档位匹配。
 *
 * @param latitude 当前视口中心纬度数值（WGS-84 / GCJ-02）
 * @param zoom 当前地图缩放级别（Zoom）
 * @param metersPerPixel 实测每像素大地距离（米/px，由 MapLibre 原生 projection 提供，为 null 时自动通过公式精确换算）
 * @param maxWidthDp 比例尺允许占用的最大宽度（dp）
 * @param modifier 外部布局修饰符
 */
@Composable
fun MapScaleBar(
    latitude: Double,
    zoom: Double,
    metersPerPixel: Double? = null,
    maxWidthDp: Float = 88f,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density

    // 计算大地分辨率与当前最优比例尺刻度信息
    val scaleInfo = remember(latitude, zoom, metersPerPixel, density, maxWidthDp) {
        calculateScaleInfo(latitude, zoom, metersPerPixel, density, maxWidthDp)
    }

    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 刻度数值与单位文本（如 "300 m", "1 km"）
        Text(
            text = scaleInfo.displayText,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.85f),
                    offset = Offset(0f, 2f),
                    blurRadius = 4f
                )
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 底部标尺线（两端带小竖端点）
        Canvas(
            modifier = Modifier
                .width(scaleInfo.widthDp.dp)
                .height(6.dp)
        ) {
            val strokeWidthPx = 1.6.dp.toPx()
            val endTickHeightPx = 5.dp.toPx()
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 阴影底色（增强对比度）
            val shadowColor = Color.Black.copy(alpha = 0.65f)
            val shadowOffset = 1.dp.toPx()

            // 绘制阴影底线
            drawLine(
                color = shadowColor,
                start = Offset(0f, canvasHeight + shadowOffset),
                end = Offset(canvasWidth, canvasHeight + shadowOffset),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Square
            )

            // 绘制主水平白色标尺线
            drawLine(
                color = Color.White,
                start = Offset(0f, canvasHeight),
                end = Offset(canvasWidth, canvasHeight),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Square
            )

            // 绘制左侧垂直端点标线
            drawLine(
                color = Color.White,
                start = Offset(0f, canvasHeight),
                end = Offset(0f, canvasHeight - endTickHeightPx),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Square
            )

            // 绘制右侧垂直端点标线
            drawLine(
                color = Color.White,
                start = Offset(canvasWidth, canvasHeight),
                end = Offset(canvasWidth, canvasHeight - endTickHeightPx),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Square
            )
        }
    }
}

/**
 * 比例尺计算结果数据封装类
 *
 * @property widthDp 标尺在屏幕上的绘制宽度（dp）
 * @property displayText 标尺展示的距离文本
 */
data class ScaleInfo(
    val widthDp: Float,
    val displayText: String
)

/**
 * 计算给定纬度与缩放级别下的标尺宽度与文本
 *
 * @param latitude 纬度数值
 * @param zoom 缩放级别数值
 * @param measuredMetersPerPixel 实测每像素米数（可选）
 * @param density 屏幕像素密度
 * @param maxWidthDp 最大允许标尺宽度
 * @return 包含宽度与文本的 [ScaleInfo] 对象
 */
private fun calculateScaleInfo(
    latitude: Double,
    zoom: Double,
    measuredMetersPerPixel: Double?,
    density: Float,
    maxWidthDp: Float
): ScaleInfo {
    val metersPerPx = if (measuredMetersPerPixel != null && measuredMetersPerPixel > 0) {
        measuredMetersPerPixel
    } else {
        val clampedLat = latitude.coerceIn(-85.0, 85.0)
        val radLat = Math.toRadians(clampedLat)
        // MapLibre 原生引擎基于 512px 墨卡托投影瓦片基准
        (40075016.686 * cos(radLat)) / (512.0 * 2.0.pow(zoom))
    }

    val metersPerDp = (metersPerPx * density).toFloat()
    val maxMeters = maxWidthDp * metersPerDp

    // 寻找最适合当前宽度的距离档位（不超过最大宽度）
    var selectedDistanceMeters = SCALE_DISTANCES_METERS[0]
    for (d in SCALE_DISTANCES_METERS) {
        if (d <= maxMeters) {
            selectedDistanceMeters = d
        } else {
            break
        }
    }

    val actualWidthDp = (selectedDistanceMeters / metersPerDp).toFloat().coerceIn(24f, maxWidthDp)
    val displayText = if (selectedDistanceMeters >= 1000.0) {
        val km = selectedDistanceMeters / 1000.0
        if (km == km.toInt().toDouble()) "${km.toInt()} km" else String.format(java.util.Locale.CHINA, "%.1f km", km)
    } else {
        "${selectedDistanceMeters.toInt()} m"
    }

    return ScaleInfo(
        widthDp = actualWidthDp,
        displayText = displayText
    )
}
