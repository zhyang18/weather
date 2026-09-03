package com.weather.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.weather.app.model.CityInfo
import com.weather.app.model.WeatherData
import com.weather.app.ui.map.MapLibreComposeView
import com.weather.app.ui.map.MapLibreHelper
import com.weather.app.ui.map.MapScaleBarView
import com.weather.app.util.CoordinateTransform
import kotlinx.coroutines.launch
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

/**
 * 地图底图图层类型枚举
 *
 * @property key 图层在系统中的唯一标识符
 * @property title 图层在 UI 中的展示名称
 */
enum class MapLayerType(val key: String, val title: String) {
    /** 暗夜深色底图 */
    DARK("dark", "暗色夜景"),
    /** 标准中文街景底图 */
    STANDARD("standard", "标准街景"),
    /** 高清遥感卫星影像底图 */
    SATELLITE("satellite", "卫星影像");

    companion object {
        /**
         * 根据图层键名解析枚举，缺省回退为 DARK
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
 * 全屏定位气象大地图页面（基于 MapLibre Native 原生渲染引擎）
 *
 * 采用全屏硬件加速架构，支持双指自由平移旋转缩放、多源底图即时切换、
 * RainViewer 实时降水气象雷达瓦片叠加、一键复位平滑相机动画、高精度自适应比例尺以及底部天气实况卡片。
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

        // 经纬度偏转（GCJ-02）
        val gcjCoords = remember(lat, lng) {
            CoordinateTransform.wgs84ToGcj02(lat, lng)
        }
        val targetLatLng = remember(gcjCoords) {
            LatLng(gcjCoords.first, gcjCoords.second)
        }

        // 默认街道级缩放级别（标准 300m 街区视野 / Zoom 16.0）
        val defaultZoom = 16.0

        var currentLayer by remember(mapLayerType) { mutableStateOf(MapLayerType.fromKey(mapLayerType)) }
        var isRadarEnabled by remember(isMapRadarEnabled) { mutableStateOf(isMapRadarEnabled) }
        var showLayerSelector by remember { mutableStateOf(false) }

        var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
        var currentStyle by remember { mutableStateOf<Style?>(null) }
        var currentMarker by remember { mutableStateOf<Marker?>(null) }
        var currentCameraPosition by remember { mutableStateOf<CameraPosition?>(null) }
        val scope = rememberCoroutineScope()

        // 持有 MapScaleBarView 实例引用，供 onCameraMove 直接调用（零延迟，无 Compose 重组开销）
        val scaleBarViewRef = remember { mutableStateOf<MapScaleBarView?>(null) }

        // 更新定位标注点（仅保留纯图标标记，不添加提示气泡并屏蔽点击事件）
        fun updateMapMarker(map: MapLibreMap) {
            currentMarker?.let { map.removeMarker(it) }
            val marker = map.addMarker(
                MarkerOptions()
                    .position(targetLatLng)
            )
            currentMarker = marker
            // 彻底拦截并消费标注点的点击事件，防止弹出任何默认提示气泡框
            map.setOnMarkerClickListener { true }
        }

        // 同步雷达图层状态
        fun syncRadarLayer(style: Style?, enabled: Boolean) {
            if (style == null) return
            if (enabled) {
                scope.launch {
                    val tileUrl = MapLibreHelper.fetchLatestRadarTileUrl()
                    if (tileUrl != null) {
                        MapLibreHelper.applyRadarLayer(style, tileUrl)
                    }
                }
            } else {
                MapLibreHelper.removeRadarLayer(style)
            }
        }

        // 当城市坐标或天气数据变化时，更新标记点与相机中心（默认加载 300m 街区视野 / Zoom 16.0）
        LaunchedEffect(visible, targetLatLng, cityName, weatherData, mapInstance) {
            mapInstance?.let { map ->
                if (visible) {
                    updateMapMarker(map)
                    val cameraPosition = CameraPosition.Builder()
                        .target(targetLatLng)
                        .zoom(defaultZoom)
                        .build()
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 400)
                }
            }
        }

        // 当雷达开关或 Style 发生变更时，同步雷达图层
        LaunchedEffect(isRadarEnabled, currentStyle) {
            currentStyle?.let { style ->
                syncRadarLayer(style, isRadarEnabled)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121824))
        ) {
            // 1. 底层全屏 MapLibre 原生地图（默认 300m 街区视野 / Zoom 16.0）
            MapLibreComposeView(
                modifier = Modifier.fillMaxSize(),
                lat = lat,
                lng = lng,
                zoom = defaultZoom,
                mapLayerType = currentLayer.key,
                isInteractive = true,
                onMapReady = { map, style ->
                    mapInstance = map
                    currentStyle = style
                    currentCameraPosition = map.cameraPosition
                    updateMapMarker(map)
                    syncRadarLayer(style, isRadarEnabled)
                },
                onCameraMove = { cameraPosition, metersPerPx ->
                    currentCameraPosition = cameraPosition
                    // 直接驱动原生 View 重绘，完全绕开 Compose 重组调度，做到相机每帧零延迟
                    scaleBarViewRef.value?.updateFromSampling(metersPerPx)
                }
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

            // 3. 右侧快捷地图控制操作区 (图层切换 / 气象雷达 / 缩放 / 复位)
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
                        val nextState = !isRadarEnabled
                        isRadarEnabled = nextState
                        onMapRadarToggle(nextState)
                        syncRadarLayer(currentStyle, nextState)
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 放大按钮 (+)
                MapFloatingButton(
                    icon = Icons.Default.Add,
                    contentDescription = "放大地图",
                    onClick = {
                        mapInstance?.animateCamera(CameraUpdateFactory.zoomIn(), 250)
                    }
                )

                // 缩小按钮 (-)
                MapFloatingButton(
                    icon = Icons.Default.Remove,
                    contentDescription = "缩小地图",
                    onClick = {
                        mapInstance?.animateCamera(CameraUpdateFactory.zoomOut(), 250)
                    }
                )

                // 当前城市位置复位（平滑动画飞回 300m 比例尺）
                MapFloatingButton(
                    icon = Icons.Default.MyLocation,
                    contentDescription = "复位定位",
                    onClick = {
                        mapInstance?.let { map ->
                            val cameraPosition = CameraPosition.Builder()
                                .target(targetLatLng)
                                .zoom(defaultZoom)
                                .build()
                            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 500)
                        }
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

            // 5. 左下角原生高帧率比例尺（由 onCameraMove 直接调用 updateFromSampling，零延迟实时刷新）
            AndroidView(
                factory = { ctx ->
                    MapScaleBarView(ctx).also { view ->
                        scaleBarViewRef.value = view
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 20.dp, bottom = 92.dp)
            )

            // 6. 底部城市天气详情信息浮窗
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
                    // 左侧：城市地名与实时天气现象
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

                    // 右侧：湿度、风速与经纬度坐标
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
 * 地图悬浮控制圆形按钮
 *
 * @param icon 图标矢量资产 [ImageVector]
 * @param contentDescription 无障碍辅助描述
 * @param isActive 是否处于高亮激活状态
 * @param onClick 按钮点击回调
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
