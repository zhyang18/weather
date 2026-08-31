package com.weather.app.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 地球实时日光 3D 拟真模拟器全屏交互页面
 *
 * 通过本地离线 WebGL/Three.js 高清场景，真实模拟地球昼夜晨昏圈、太阳赤纬角、实时 UTC 自转角度，
 * 并提供午时线、地轴、黄道与赤道对齐等多维交互控制。
 *
 * @param visible 是否展示该全屏页面
 * @param onBackClick 点击返回或系统返回触发回调
 * @param modifier 外部布局修饰符
 */
@Composable
fun EarthDaylightScreen(
    visible: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = visible) {
        onBackClick()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        EarthDaylightContent(
            onBackClick = onBackClick
        )
    }
}

/**
 * 计算当天指定分钟数对应的毫秒时间戳
 *
 * @param baseTimeMillis 基准时间戳（获取当天年月日）
 * @param minuteOfDay 当天第几分钟（0f ~ 1440f）
 * @return 目标时刻对应的毫秒时间戳
 */
private fun calculateTimestampForMinuteOfDay(baseTimeMillis: Long, minuteOfDay: Float): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = baseTimeMillis
        val totalSecs = (minuteOfDay * 60f).toInt()
        val hour = (totalSecs / 3600) % 24
        val minute = (totalSecs % 3600) / 60
        val second = totalSecs % 60
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, second)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

/**
 * 地球实时日光核心内容与控制器组合视图
 *
 * @param onBackClick 返回事件回调
 * @param modifier 布局修饰符
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EarthDaylightContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isTerminatorActive by remember { mutableStateOf(false) }
    var isSubsolarActive by remember { mutableStateOf(false) }
    var isGraticuleActive by remember { mutableStateOf(false) }
    var isMeridianActive by remember { mutableStateOf(false) }
    var isAxisActive by remember { mutableStateOf(false) }

    // 24小时时间模拟与动态演变状态
    var isTimeCardVisible by remember { mutableStateOf(false) }
    var isCustomTimeActive by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var playSpeed by remember { mutableFloatStateOf(1f) }

    val initialCal = remember { Calendar.getInstance() }
    var selectedMinuteOfDay by remember {
        mutableFloatStateOf(initialCal.get(Calendar.HOUR_OF_DAY) * 60f + initialCal.get(Calendar.MINUTE) + initialCal.get(Calendar.SECOND) / 60f)
    }

    // 动态演变播放协程 (循环向前演进 24 小时)
    LaunchedEffect(isPlaying, playSpeed) {
        if (isPlaying) {
            while (true) {
                delay(40L)
                selectedMinuteOfDay = (selectedMinuteOfDay + 1.8f * playSpeed) % 1440f
                val targetTimeMillis = calculateTimestampForMinuteOfDay(System.currentTimeMillis(), selectedMinuteOfDay)
                executeSimulatorJs(webViewRef, "window.earthSimulator.setTime($targetTimeMillis)")
            }
        }
    }

    // 实时系统时钟（每秒自动校准刷新）
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // 顶栏时钟展示（支持实时与模拟时间联动）
    val displayMillis = if (isCustomTimeActive) {
        calculateTimestampForMinuteOfDay(currentTimeMillis, selectedMinuteOfDay)
    } else {
        currentTimeMillis
    }

    val (dateStr, timeStr, timeZoneStr) = remember(displayMillis / 1000L, isCustomTimeActive) {
        val date = Date(displayMillis)
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.CHINA)
        val tzFmt = SimpleDateFormat("Z", Locale.CHINA)
        val tz = "GMT" + tzFmt.format(date)
        Triple(dateFmt.format(date), timeFmt.format(date), tz)
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030710))
    ) {
        // 1. 硬件加速 WebGL 离线视图容器
        AndroidView(
            factory = { context ->
                createEarthWebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }
                    }
                    loadUrl("file:///android_asset/earth_daylight/index.html")
                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. 初始加载指示器
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF60A5FA),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "正在载入 3D 拟真地球模型...",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 3. 顶部沉浸式导航栏（绝对居中排版）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(38.dp)
                    .align(Alignment.CenterStart)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 中间居中主标题与实时时钟胶囊（始终绝对居中）
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = "全球昼夜光照",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCustomTimeActive) Color(0x991E293B) else Color(0x660F172A))
                        .border(
                            0.6.dp,
                            if (isCustomTimeActive) Color(0xFF38BDF8).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$dateStr ",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = timeStr,
                        color = if (isCustomTimeActive) Color(0xFF38BDF8) else Color(0xFF60A5FA),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isCustomTimeActive) " (动态演变)" else " · $timeZoneStr",
                        color = if (isCustomTimeActive) Color(0xFFFDE047) else Color.White.copy(alpha = 0.65f),
                        fontSize = 10.sp
                    )
                }
            }

            // 右侧视角复位快捷按钮（绝对靠右）
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(38.dp)
                    .align(Alignment.CenterEnd)
            ) {
                IconButton(
                    onClick = {
                        executeSimulatorJs(webViewRef, "window.earthSimulator.resetCamera()")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "视角复位",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 4. 底部 24 小时动态演变面板与控制坞（水平全宽全透明贴底，上移 10dp）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 24小时手动时间调节与动态图播放控制面板（支持点击底部按钮展开/收起）
            AnimatedVisibility(
                visible = isTimeCardVisible,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 8.dp)
            ) {
                Surface(
                    color = Color(0x800B132B), // 50% 透明度毛玻璃背景
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        // 顶栏：播放控制、时间指示与恢复实时
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 动态播放/暂停圆形按钮
                                Surface(
                                    shape = CircleShape,
                                    color = if (isPlaying) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.20f),
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clickable {
                                            isCustomTimeActive = true
                                            isPlaying = !isPlaying
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "暂停" else "播放",
                                            tint = if (isPlaying) Color(0xFF0F172A) else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // 24小时当前时刻
                                val displayHour = (selectedMinuteOfDay.toInt() / 60) % 24
                                val displayMin = selectedMinuteOfDay.toInt() % 60
                                val formattedTime = String.format(Locale.CHINA, "%02d:%02d", displayHour, displayMin)

                                Text(
                                    text = formattedTime,
                                    color = if (isCustomTimeActive) Color(0xFF38BDF8) else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = if (isPlaying) "24小时动态演变中" else if (isCustomTimeActive) "手动选择时间" else "实时光照",
                                    color = Color.White.copy(alpha = 0.60f),
                                    fontSize = 11.sp
                                )
                            }

                            // 右侧倍速与恢复实时按钮
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 倍速切换 (1x -> 2x -> 4x)
                                if (isPlaying) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier
                                            .clickable {
                                                playSpeed = when (playSpeed) {
                                                    1f -> 2f
                                                    2f -> 4f
                                                    else -> 1f
                                                }
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${playSpeed.toInt()}x",
                                            color = Color(0xFFFDE047),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // 恢复实时按钮
                                if (isCustomTimeActive) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0x3338BDF8),
                                        border = BorderStroke(0.6.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .clickable {
                                                isPlaying = false
                                                isCustomTimeActive = false
                                                val now = Calendar.getInstance()
                                                selectedMinuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60f + now.get(Calendar.MINUTE) + now.get(Calendar.SECOND) / 60f
                                                executeSimulatorJs(webViewRef, "window.earthSimulator.resetToLiveTime()")
                                            }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "恢复实时",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // 24小时平滑滑动轴
                        Slider(
                            value = selectedMinuteOfDay,
                            onValueChange = { min ->
                                isCustomTimeActive = true
                                selectedMinuteOfDay = min
                                val targetMillis = calculateTimestampForMinuteOfDay(System.currentTimeMillis(), min)
                                executeSimulatorJs(webViewRef, "window.earthSimulator.setTime($targetMillis)")
                            },
                            valueRange = 0f..1440f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF38BDF8),
                                inactiveTrackColor = Color.White.copy(alpha = 0.20f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                        )

                        // 24小时刻度标尺
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEach { label ->
                                Text(
                                    text = label,
                                    color = Color.White.copy(alpha = 0.40f),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            // 操作说明提示文本
            Text(
                text = "单指拖动旋转视角 · 双指捏合自由缩放 · 实时模拟全球昼夜晨昏圈",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // 底部控制坞（水平全屏占满、全透明现代悬浮容器）
            Surface(
                color = Color.Transparent,
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. 晨昏线切换（昼夜交界线）
                    EarthDockButton(
                        icon = Icons.Default.WbSunny,
                        label = "晨昏线",
                        isActive = isTerminatorActive,
                        onClick = {
                            isTerminatorActive = !isTerminatorActive
                            executeSimulatorJs(webViewRef, "window.earthSimulator.toggleTerminator()")
                        }
                    )

                    // 2. 太阳直射点切换
                    EarthDockButton(
                        icon = Icons.Default.WbSunny,
                        label = "直射点",
                        isActive = isSubsolarActive,
                        onClick = {
                            isSubsolarActive = !isSubsolarActive
                            executeSimulatorJs(webViewRef, "window.earthSimulator.toggleSubsolar()")
                        }
                    )

                    // 3. 午时线切换（图标使用指南针 Explore）
                    EarthDockButton(
                        icon = Icons.Default.Explore,
                        label = "午时线",
                        isActive = isMeridianActive,
                        onClick = {
                            isMeridianActive = !isMeridianActive
                            executeSimulatorJs(webViewRef, "window.earthSimulator.toggleMeridian()")
                        }
                    )

                    // 4. 地轴切换
                    EarthDockButton(
                        icon = Icons.Default.VpnLock,
                        label = "地轴",
                        isActive = isAxisActive,
                        onClick = {
                            isAxisActive = !isAxisActive
                            executeSimulatorJs(webViewRef, "window.earthSimulator.toggleAxis()")
                        }
                    )

                    // 5. 赤道对齐
                    EarthDockButton(
                        icon = Icons.Default.Language,
                        label = "赤道对齐",
                        isActive = false,
                        onClick = {
                            executeSimulatorJs(webViewRef, "window.earthSimulator.alignEquator()")
                        }
                    )

                    // 6. 黄道对齐
                    EarthDockButton(
                        icon = Icons.Default.WbSunny,
                        label = "黄道对齐",
                        isActive = false,
                        onClick = {
                            executeSimulatorJs(webViewRef, "window.earthSimulator.alignEcliptic()")
                        }
                    )

                    // 7. 经纬线网格切换（已移动到最后）
                    EarthDockButton(
                        icon = Icons.Default.Language,
                        label = "经纬线",
                        isActive = isGraticuleActive,
                        onClick = {
                            isGraticuleActive = !isGraticuleActive
                            executeSimulatorJs(webViewRef, "window.earthSimulator.toggleGraticule()")
                        }
                    )

                    // 8. 24小时时间轴面板开关（已移动到最后）
                    EarthDockButton(
                        icon = Icons.Default.Timeline,
                        label = "时间轴",
                        isActive = isTimeCardVisible,
                        onClick = {
                            isTimeCardVisible = !isTimeCardVisible
                        }
                    )
                }
            }
        }
    }
}

/**
 * 底部控制栏单个交互胶囊按钮（统一固定高度、逐字竖排文字与无边框扁平设计）
 *
 * @param icon 按钮矢量图标 [ImageVector]
 * @param label 按钮文字描述
 * @param isActive 是否处于激活高亮状态
 * @param onClick 点击回调函数
 */
@Composable
private fun EarthDockButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val backgroundBrush = if (isActive) {
        Brush.verticalGradient(
            listOf(Color(0x802563EB), Color(0x803B82F6))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF))
        )
    }

    val verticalLabel = remember(label) {
        label.map { it.toString() }.joinToString("\n")
    }

    Column(
        modifier = Modifier
            .height(88.dp)
            .width(36.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundBrush)
            .border(
                0.6.dp,
                if (isActive) Color(0xFF60A5FA).copy(alpha = 0.6f) else Color.Transparent,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) Color.White else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = verticalLabel,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            lineHeight = 13.5.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 构建并配置用于渲染 WebGL 3D 场景的 WebView 实例
 *
 * @param context Android 上下文对象 [Context]
 * @return 已完成硬件加速与安全配置的 [WebView] 实例
 */
@SuppressLint("SetJavaScriptEnabled")
private fun createEarthWebView(context: Context): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.BLACK)
        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                android.util.Log.d("EarthSim", "${consoleMessage?.message()} -- line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                return true
            }
        }

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            loadWithOverviewMode = true
            useWideViewPort = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                mediaPlaybackRequiresUserGesture = false
            }
        }
    }
}

/**
 * 安全向指定 WebView 注入执行 JavaScript 代码
 *
 * @param webView 目标 WebView 引用
 * @param jsScript 待执行的 JavaScript 脚本字符串
 */
private fun executeSimulatorJs(webView: WebView?, jsScript: String) {
    webView?.evaluateJavascript(jsScript, null)
}
