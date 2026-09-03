package com.weather.app.ui

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.weather.app.model.CityInfo
import com.weather.app.model.WeatherData

/**
 * 地图底图图层类型枚举
 *
 * @property key 图层在 JS 桥接中的唯一标识
 * @property title 图层展示名称
 */
enum class MapLayerType(val key: String, val title: String) {
    /** 暗夜深色矢量底图 */
    DARK("dark", "暗色夜景"),
    /** OpenStreetMap 标准街景底图 */
    STANDARD("standard", "标准街景"),
    /** Esri 高清卫星遥感影像底图 */
    SATELLITE("satellite", "卫星影像");

    companion object {
        /**
         * 根据图层 key 解析枚举，缺省回退为 DARK
         *
         * @param key 图层标识符
         * @return 对应的 [MapLayerType]
         */
        fun fromKey(key: String): MapLayerType {
            return entries.firstOrNull { it.key == key } ?: DARK
        }
    }
}

/**
 * 全屏定位气象大地图交互页面
 *
 * 采用全屏沉浸式架构，支持自由缩放平移、多源底图切换（暗色/街景/卫星）、
 * 实时降雨气象雷达瓦片叠加、一键当前定位复位以及底部天气详情浮窗。
 *
 * @param visible 是否展示该全屏页面
 * @param city 当前聚焦的城市实体 [CityInfo]
 * @param weatherData 当前城市聚合气象数据 [WeatherData]
 * @param mapLayerType 当前持久化保存的底图图层类型键名
 * @param isMapRadarEnabled 当前持久化保存的降水雷达开关状态
 * @param onMapLayerChange 用户切换底图图层类型时的回调
 * @param onMapRadarToggle 用户切换降水雷达开关时的回调
 * @param onBackClick 点击返回按钮或系统返回手势时的回调
 * @param modifier 外部修饰符
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LocationMapScreen(
    visible: Boolean,
    city: CityInfo,
    weatherData: WeatherData?,
    mapLayerType: String = "dark",
    isMapRadarEnabled: Boolean = false,
    onMapLayerChange: (String) -> Unit = {},
    onMapRadarToggle: (Boolean) -> Unit = {},
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
        modifier = modifier.fillMaxSize()
    ) {
        // 优先读取城市自带的经纬度，若为 null 则自动通过全国区划坐标库精确匹配
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
        val cityName = city.name
        val detailText = "${city.province} ${city.district}".trim()

        var currentLayer by remember(mapLayerType) { mutableStateOf(MapLayerType.fromKey(mapLayerType)) }
        var isRadarEnabled by remember(isMapRadarEnabled) { mutableStateOf(isMapRadarEnabled) }
        var showLayerSelector by remember { mutableStateOf(false) }
        var webViewInstance by remember { mutableStateOf<WebView?>(null) }

        // 当大地图可见或当前城市坐标/图层/雷达状态变更时，自动驱动大地图飞至最新坐标并更新标注（默认 300m 比例尺 / Zoom 16）
        LaunchedEffect(visible, lat, lng, cityName, currentLayer, isRadarEnabled, webViewInstance) {
            if (visible && webViewInstance != null) {
                val subtitle = "气温: ${weatherData?.current?.temperature?.toInt() ?: 0}° · ${weatherData?.getDisplayWeatherText() ?: ""}"
                val js = "javascript:(function(){ if(window.setLocation){ window.setLocation($lat, $lng, 16, '$cityName', '$subtitle', true, '${currentLayer.key}', $isRadarEnabled); window.dispatchEvent(new Event('resize')); } })()"
                webViewInstance?.evaluateJavascript(js, null)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121824))
        ) {
            // 1. 底层全屏地图 WebView
            AndroidView(
                factory = { ctx ->
                    createFullMapWebView(
                        context = ctx,
                        lat = lat,
                        lng = lng,
                        title = cityName,
                        subtitle = "气温: ${weatherData?.current?.temperature?.toInt() ?: 0}° · ${weatherData?.getDisplayWeatherText() ?: "未知"}",
                        initialLayer = currentLayer.key,
                        initialRadar = isRadarEnabled
                    ) { view ->
                        webViewInstance = view
                    }
                },
                update = { view ->
                    webViewInstance = view
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. 顶部沉浸式导航栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xCC1A2332),
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xCC1A2332),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = cityName,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (detailText.isNotEmpty()) {
                                Text(
                                    text = detailText,
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 3. 右侧快捷地图控制操作区 (图层切换 / 气象雷达 / 缩放 / 复位，上移至顶部栏下方，彻底杜绝与底部浮窗重叠)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 64.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 图层切换按钮
                MapFloatingButton(
                    icon = Icons.Default.Layers,
                    contentDescription = "切换图层",
                    isActive = showLayerSelector,
                    onClick = { showLayerSelector = !showLayerSelector }
                )

                // 气象雷达图层切换按钮
                MapFloatingButton(
                    icon = Icons.Default.WaterDrop,
                    contentDescription = "降水雷达",
                    isActive = isRadarEnabled,
                    onClick = {
                        isRadarEnabled = !isRadarEnabled
                        onMapRadarToggle(isRadarEnabled)
                        webViewInstance?.evaluateJavascript("javascript:toggleRadarLayer($isRadarEnabled);", null)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 放大按钮 (+)
                MapFloatingButton(
                    icon = Icons.Default.Add,
                    contentDescription = "放大地图",
                    onClick = {
                        webViewInstance?.evaluateJavascript("javascript:zoomIn();", null)
                    }
                )

                // 缩小按钮 (-)
                MapFloatingButton(
                    icon = Icons.Default.Remove,
                    contentDescription = "缩小地图",
                    onClick = {
                        webViewInstance?.evaluateJavascript("javascript:zoomOut();", null)
                    }
                )

                // 当前城市位置复位
                MapFloatingButton(
                    icon = Icons.Default.MyLocation,
                    contentDescription = "复位定位",
                    onClick = {
                        webViewInstance?.evaluateJavascript("javascript:centerCurrent();", null)
                    }
                )
            }

            // 4. 图层选择浮动面板 (点击图层按钮时在右侧弹出)
            if (showLayerSelector) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xF0182230),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 64.dp, end = 70.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MapLayerType.entries.forEach { layer ->
                            val isSelected = currentLayer == layer
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0x3564B5F6) else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        currentLayer = layer
                                        showLayerSelector = false
                                        onMapLayerChange(layer.key)
                                        webViewInstance?.evaluateJavascript("javascript:setLayer('${layer.key}');", null)
                                    }
                            ) {
                                Text(
                                    text = layer.title,
                                    color = if (isSelected) Color(0xFF64B5F6) else Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. 底部城市天气详情信息浮窗 (优化左右权重分配，防止长地名挤压右侧数据)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xEB16202E),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：城市地名与实时天气现象（支持长地名单行省略保护）
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp)
                    ) {
                        Text(
                            text = "${cityName} · 当前实况",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (weatherData != null) "${weatherData.current.temperature.toInt()}°C  ${weatherData.getDisplayWeatherText()}" else "气温数据同步中...",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // 右侧：湿度、风速与经纬度坐标（精简格式化，彻底杜绝浮点数多位尾巴和换行）
                    if (weatherData != null) {
                        val windSpeedFormatted = String.format(java.util.Locale.CHINA, "%.1f", weatherData.current.windSpeed).removeSuffix(".0")
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "湿度: ${weatherData.current.humidity.toInt()}% · 风速: ${windSpeedFormatted}km/h",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = String.format(java.util.Locale.CHINA, "%.2f°%s, %.2f°%s", kotlin.math.abs(lat), if (lat>=0)"N" else "S", kotlin.math.abs(lng), if (lng>=0)"E" else "W"),
                                color = Color(0xFF64B5F6),
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 地图悬浮功能圆钮
 *
 * @param icon 图标矢量资产 [ImageVector]
 * @param contentDescription 无障碍辅助文本
 * @param isActive 是否处于高亮激活状态
 * @param onClick 点击事件回调
 */
@Composable
private fun MapFloatingButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isActive) Color(0xFF2196F3) else Color(0xCC1A2332),
        modifier = Modifier.size(44.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) Color.White else Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 创建全屏大地图专用的 WebView 控件
 *
 * @param context Android 上下文对象
 * @param lat 纬度数值
 * @param lng 经度数值
 * @param title 标注标题
 * @param subtitle 标注副标题
 * @param initialLayer 初始记忆底图图层标识
 * @param initialRadar 初始记忆降水雷达开关状态
 * @param onCreated 创建完成回调
 * @return 初始化的 [WebView] 实例
 */
@SuppressLint("SetJavaScriptEnabled")
private fun createFullMapWebView(
    context: Context,
    lat: Double,
    lng: Double,
    title: String,
    subtitle: String,
    initialLayer: String = "dark",
    initialRadar: Boolean = false,
    onCreated: (WebView) -> Unit
): WebView {
    return WebView(context).apply {
        layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                android.util.Log.d("WeatherFullMap", "${consoleMessage?.message()} -- line ${consoleMessage?.lineNumber()}")
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
            setSupportZoom(true)
            builtInZoomControls = false
        }
        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(android.graphics.Color.parseColor("#151c28"))
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val js = "javascript:(function(){ if(window.setLocation){ window.setLocation($lat, $lng, 16, '$title', '$subtitle', true, '$initialLayer', $initialRadar); window.dispatchEvent(new Event('resize')); } })()"
                view?.evaluateJavascript(js, null)
            }
        }
        loadUrl("file:///android_asset/map/index.html")
        onCreated(this)
    }
}
