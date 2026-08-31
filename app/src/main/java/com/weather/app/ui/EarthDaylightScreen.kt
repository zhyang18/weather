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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    var isTerminatorActive by remember { mutableStateOf(true) }
    var isMeridianActive by remember { mutableStateOf(false) }
    var isAxisActive by remember { mutableStateOf(false) }

    // 实时系统时钟（每秒自动校准刷新）
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val (dateStr, timeStr, timeZoneStr) = remember(currentTimeMillis / 1000L) {
        val date = Date(currentTimeMillis)
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
                    text = "地球实时日光",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x660F172A))
                        .border(0.6.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
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
                        color = Color(0xFF60A5FA),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " · $timeZoneStr",
                        color = Color.White.copy(alpha = 0.65f),
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

        // 4. 底部现代化毛玻璃交互控制坞与操作说明提示
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp, start = 14.dp, end = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 操作说明提示文本（始终居中显示在控制坞上方，永不被遮挡）
            Text(
                text = "单指拖动旋转视角 · 双指捏合自由缩放 · 实时模拟全球昼夜晨昏圈",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                color = Color(0x990F172A),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 晨昏线切换（昼夜交界线）
                    EarthDockButton(
                        icon = Icons.Default.WbSunny,
                        label = "晨昏线",
                        isActive = isTerminatorActive,
                        onClick = {
                            isTerminatorActive = !isTerminatorActive
                            executeSimulatorJs(webViewRef, "window.earthSimulator.toggleTerminator()")
                        }
                    )

                    // 午时线切换
                    EarthDockButton(
                        icon = Icons.Default.Timeline,
                        label = "午时线",
                        isActive = isMeridianActive,
                        onClick = {
                            isMeridianActive = !isMeridianActive
                            executeSimulatorJs(webViewRef, "window.earthSimulator.toggleMeridian()")
                        }
                    )

                    // 地轴切换
                    EarthDockButton(
                        icon = Icons.Default.VpnLock,
                        label = "地轴",
                        isActive = isAxisActive,
                        onClick = {
                            isAxisActive = !isAxisActive
                            executeSimulatorJs(webViewRef, "window.earthSimulator.toggleAxis()")
                        }
                    )

                    // 赤道对齐
                    EarthDockButton(
                        icon = Icons.Default.Language,
                        label = "赤道对齐",
                        isActive = false,
                        onClick = {
                            executeSimulatorJs(webViewRef, "window.earthSimulator.alignEquator()")
                        }
                    )

                    // 黄道对齐
                    EarthDockButton(
                        icon = Icons.Default.WbSunny,
                        label = "黄道对齐",
                        isActive = false,
                        onClick = {
                            executeSimulatorJs(webViewRef, "window.earthSimulator.alignEcliptic()")
                        }
                    )

                    // 视角复位
                    EarthDockButton(
                        icon = Icons.Default.CenterFocusStrong,
                        label = "复位",
                        isActive = false,
                        onClick = {
                            executeSimulatorJs(webViewRef, "window.earthSimulator.resetCamera()")
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
            listOf(Color(0xFF2563EB), Color(0xFF3B82F6))
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
