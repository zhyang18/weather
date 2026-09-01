package com.weather.app.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.weather.app.model.CityInfo
import com.weather.app.util.SolarAstroCalculator
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * 昼夜晨昏线主页单列气象指标小卡片组件
 *
 * 1. 统一 152.dp 标准高度与深灰蓝毛玻璃质感，与气象指标宫格其他单列卡片完美等高并排；
 * 2. 顶部栏展示微型地球天体图标与“昼夜晨昏线”标题及跳转箭头；
 * 3. 左侧大字展示当地实时处于“处于白昼”或“处于黑夜”（4字精简，20.sp，永不截断）；
 * 4. 左侧下方展示太阳直射点纬度（如“直射 北纬 8.9°”）与地轴倾角（23.4°）；
 * 5. 右侧靠右放置精致的 50dp 3D 拟真地球昼夜晨昏微缩球体模型，绝不遮挡左侧文字；
 * 6. 点击后跳转至 3D 拟真地球实时日光全屏模拟器（[com.weather.app.ui.EarthDaylightScreen]）。
 *
 * @param city 当前城市实体 [CityInfo]
 * @param modifier 外部修饰符 [Modifier]
 * @param onClick 点击卡片跳转昼夜晨昏线详情页回调
 */
@Composable
fun EarthDaylightRealCard(
    city: CityInfo,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    var currentSystemTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentSystemTimeMillis = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60 * 1000L)
            currentSystemTimeMillis = System.currentTimeMillis()
        }
    }

    val calendar = remember(currentSystemTimeMillis / 60000L) {
        Calendar.getInstance().apply { timeInMillis = currentSystemTimeMillis }
    }

    val solarDetail = remember(city.getCacheKey(), currentSystemTimeMillis / 60000L) {
        SolarAstroCalculator.calculateSolarDayDetail(city, calendar)
    }

    val isSunUp = solarDetail.isSunAboveHorizon
    val statusTitle = if (isSunUp) "处于白昼" else "处于黑夜"

    val declination = solarDetail.declinationDeg
    val declinationValStr = if (declination >= 0) {
        "直射 北纬 ${String.format(Locale.CHINA, "%.1f", declination)}°"
    } else {
        "直射 南纬 ${String.format(Locale.CHINA, "%.1f", abs(declination))}°"
    }

    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .height(152.dp)
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(20.dp)
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .clickable(onClick = onClick)
            .padding(14.dp)
    } else {
        modifier
            .fillMaxWidth()
            .height(152.dp)
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(20.dp)
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .padding(14.dp)
    }

    Column(
        modifier = cardModifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. 顶部标题栏（微型地球图标 + 标题 + 跳转小箭头）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = "昼夜晨昏线",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "昼夜晨昏线",
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "查看全球昼夜详情",
                    tint = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // 2. 中间主要区域：居中大幅 3D 拟真地球晨昏圈模型 (80dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            EarthTerminatorMiniCanvas(
                declinationDeg = declination,
                isDaytime = isSunUp,
                modifier = Modifier.size(80.dp)
            )
        }

        // 3. 底部信息行：左右两端对齐展示【昼夜状态】与【太阳直射点纬度】（统一 11.5.sp 风格）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = statusTitle,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )

            Text(
                text = declinationValStr.replace(" ", ""),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

/**
 * 绘制微缩拟真地球昼夜晨昏圈图形
 *
 * @param declinationDeg 太阳赤纬角（决定晨昏线倾斜与弯曲）
 * @param isDaytime 当前城市是否处于昼半球
 * @param modifier 外部修饰符 [Modifier]
 */
@Composable
private fun EarthTerminatorMiniCanvas(
    declinationDeg: Double,
    isDaytime: Boolean,
    modifier: Modifier = Modifier
) {
    val terminatorPath = remember { Path() }

    Canvas(modifier = modifier) {
        val r = size.width / 2f * 0.88f
        val c = Offset(size.width / 2f, size.height / 2f)

        // 1. 地球夜半球底色（深邃星空墨蓝底球）
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1E293B),
                    Color(0xFF0F172A)
                ),
                center = c,
                radius = r
            ),
            radius = r,
            center = c
        )

        // 2. 地球大气边缘微光晕 (Atmosphere Rim Glow)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x3360A5FA),
                    Color(0x6638BDF8)
                ),
                center = c,
                radius = r * 1.08f
            ),
            radius = r * 1.08f,
            center = c
        )

        // 3. 绘制昼半球阳光照射区域（依托晨昏半椭圆与赤纬倾角构建弧形）
        val tiltDeg = (declinationDeg.toFloat() * 0.9f).coerceIn(-23.4f, 23.4f)

        rotate(degrees = -tiltDeg, pivot = c) {
            val outerRect = Rect(c.x - r, c.y - r, c.x + r, c.y + r)
            val k = (declinationDeg.toFloat() / 23.44f).coerceIn(-0.35f, 0.35f)
            val termRx = (abs(k) * r).coerceAtLeast(0.01f)
            val termRect = Rect(c.x - termRx, c.y - r, c.x + termRx, c.y + r)

            terminatorPath.reset()
            if (isDaytime) {
                // 昼半球在右侧
                terminatorPath.arcTo(outerRect, -90f, 180f, false)
                if (k >= 0) {
                    terminatorPath.arcTo(termRect, 90f, -180f, false)
                } else {
                    terminatorPath.arcTo(termRect, 90f, 180f, false)
                }
            } else {
                // 昼半球在左侧（背光视角）
                terminatorPath.arcTo(outerRect, 90f, 180f, false)
                if (k >= 0) {
                    terminatorPath.arcTo(termRect, 270f, -180f, false)
                } else {
                    terminatorPath.arcTo(termRect, 270f, 180f, false)
                }
            }
            terminatorPath.close()

            // 昼半球蔚蓝明亮渐变填充
            drawPath(
                path = terminatorPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0284C7),
                        Color(0xFF38BDF8),
                        Color(0xFFBAE6FD)
                    ),
                    startX = c.x - r,
                    endX = c.x + r
                )
            )

            // 晨昏分界过渡带柔和光晕线 (Terminator Line Glow)
            val termLinePath = Path().apply {
                if (k >= 0) {
                    arcTo(termRect, 90f, -180f, false)
                } else {
                    arcTo(termRect, 90f, 180f, false)
                }
            }
            drawPath(
                path = termLinePath,
                color = Color(0xFFFFD54F).copy(alpha = 0.65f),
                style = Stroke(width = 1.4f, cap = StrokeCap.Round)
            )
        }

        // 4. 地轴极点指示线与地平赤道线
        rotate(degrees = 23.4f, pivot = c) {
            // 地轴虚线
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(c.x, c.y - r * 1.15f),
                end = Offset(c.x, c.y + r * 1.15f),
                strokeWidth = 1.0f,
                cap = StrokeCap.Round
            )
            // 北极点与南极点小圆标记
            drawCircle(
                color = Color.White.copy(alpha = 0.80f),
                radius = 1.5.dp.toPx(),
                center = Offset(c.x, c.y - r * 1.15f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.80f),
                radius = 1.5.dp.toPx(),
                center = Offset(c.x, c.y + r * 1.15f)
            )
        }

        // 5. 外圈球体发光纤细描边
        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = r,
            center = c,
            style = Stroke(width = 1.0f)
        )
    }
}
