package com.weather.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.weather.app.model.CityInfo
import com.weather.app.model.WeatherData

/**
 * 首页定位气象微缩地图卡片组件
 *
 * @param city 当前聚焦的城市实体 [CityInfo]
 * @param weatherData 当前城市聚合气象数据 [WeatherData]
 * @param mapLayerType 持久化记忆的底图图层类型
 * @param onClick 点击卡片跳转全屏大地图的回调
 * @param modifier 外部修饰符
 */
@SuppressLint("SetJavaScriptEnabled")
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
    val displayName = city.district.ifEmpty { city.name }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // 当城市坐标、显示地名或底图类型变更时，立即驱动 WebView 地图更新位置与图层
    LaunchedEffect(lat, lng, displayName, mapLayerType, webViewRef) {
        webViewRef?.let { view ->
            updateCardMapLocation(view, lat, lng, displayName, mapLayerType)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .graphicsLayer {
                shadowElevation = 0f
                shape = RoundedCornerShape(20.dp)
                clip = true
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x331C2433))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // 1. 卡片顶部标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0x3064B5F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "定位地图",
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "定位地图",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "查看大图",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "查看大图",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. 微缩地图视窗容器 (使用禁用手势的 AndroidView WebView)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141923))
        ) {
            AndroidView(
                factory = { ctx ->
                    createCardWebView(ctx, lat, lng, displayName, mapLayerType) { view ->
                        webViewRef = view
                    }
                },
                update = { webView ->
                    webViewRef = webView
                    updateCardMapLocation(webView, lat, lng, displayName, mapLayerType)
                },
                modifier = Modifier.fillMaxSize()
            )

            // 覆盖一层全透明点击层，防止 WebView 内部消费滚动事件影响外部 Column 滑动
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. 底部地理坐标与天气概要
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${city.name} · ${formatCoordinates(lat, lng)}",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp
            )

            if (weatherData != null) {
                Text(
                    text = "${weatherData.current.temperature.toInt()}° · ${weatherData.getDisplayWeatherText()}",
                    color = Color(0xFF90CAF9),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 创建用于小卡片预览的轻量 WebView 实例
 *
 * @param context Android 视图上下文
 * @param lat 纬度数值
 * @param lng 经度数值
 * @param name 城市名称
 * @param mapLayerType 底图图层类型键名
 * @param onCreated 创建完成回调
 * @return 配置完成的 [WebView] 实例
 */
@SuppressLint("SetJavaScriptEnabled")
private fun createCardWebView(
    context: Context,
    lat: Double,
    lng: Double,
    name: String,
    mapLayerType: String = "dark",
    onCreated: (WebView) -> Unit = {}
): WebView {
    return WebView(context).apply {
        layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                android.util.Log.d("WeatherMapCard", "${consoleMessage?.message()} -- line ${consoleMessage?.lineNumber()}")
                return true
            }
        }
        @Suppress("DEPRECATION")
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            loadsImagesAutomatically = true
            blockNetworkImage = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 WeatherApp"
        }
        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(android.graphics.Color.parseColor("#151c28"))
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        setOnTouchListener { _, _ -> false } // 禁用手势触摸，仅做只读展示
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                updateCardMapLocation(view, lat, lng, name, mapLayerType)
            }
        }
        loadUrl("file:///android_asset/map/index.html")
        onCreated(this)
    }
}

/**
 * 更新小卡片地图的中心点坐标、标注与底图图层
 *
 * @param webView 目标 [WebView] 控件
 * @param lat 目标纬度
 * @param lng 目标经度
 * @param name 目标城市地名
 * @param mapLayerType 底图图层类型键名
 */
private fun updateCardMapLocation(
    webView: WebView?,
    lat: Double,
    lng: Double,
    name: String,
    mapLayerType: String = "dark"
) {
    if (webView == null) return
    val js = "javascript:(function(){ if(window.setLocation){ window.setLocation($lat, $lng, 10, '$name', '', false, '$mapLayerType'); window.dispatchEvent(new Event('resize')); } })()"
    webView.evaluateJavascript(js, null)
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
