package com.weather.app.ui.map

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.weather.app.util.CoordinateTransform
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * 适用于 Jetpack Compose 的 MapLibre 原生地图组件包装层
 *
 * 自动管理 Android 原生 [MapView] 的完整生命周期（Start/Resume/Pause/Stop/Destroy），
 * 支持手势交互开关、坐标系精准偏转（GCJ-02）、多源底图样式动态渲染及相机平滑变焦。
 *
 * @param modifier 外部布局修饰符
 * @param lat 目标中心点纬度数值（WGS-84 标准地球坐标）
 * @param lng 目标中心点经度数值（WGS-84 标准地球坐标）
 * @param zoom 初始或目标缩放级别
 * @param mapLayerType 底图类型标识（dark: 暗色夜景, standard: 标准街景, satellite: 卫星影像）
 * @param isInteractive 是否允许用户通过手势拖拽、缩放和旋转地图（卡片模式下可设为 false）
 * @param onMapReady 地图及其样式加载完成后的回调函数，提供 [MapLibreMap] 与当前 [Style] 句柄
 */
@Composable
fun MapLibreComposeView(
    modifier: Modifier = Modifier,
    lat: Double,
    lng: Double,
    zoom: Double = 15.0,
    mapLayerType: String = "dark",
    isInteractive: Boolean = true,
    onMapReady: (MapLibreMap, Style) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 将 WGS-84 坐标转换为高德/国内地图所需的 GCJ-02 火星坐标
    val gcjCoords = remember(lat, lng) {
        CoordinateTransform.wgs84ToGcj02(lat, lng)
    }
    val targetLatLng = remember(gcjCoords) {
        LatLng(gcjCoords.first, gcjCoords.second)
    }

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var currentStyleInstance by remember { mutableStateOf<Style?>(null) }

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    // 绑定 Compose 与 MapView 的生命周期
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // 当底图图层类型发生变化时，重新构建并加载原生 Style
    LaunchedEffect(mapLayerType, mapInstance) {
        mapInstance?.let { map ->
            val styleJson = MapLibreHelper.buildStyleJson(mapLayerType)
            map.setStyle(Style.Builder().fromJson(styleJson)) { newStyle ->
                currentStyleInstance = newStyle
                onMapReady(map, newStyle)
            }
        }
    }

    // 当坐标或缩放级别变化时，平滑更新地图相机位置
    LaunchedEffect(targetLatLng, zoom, mapInstance) {
        mapInstance?.let { map ->
            val cameraPosition = CameraPosition.Builder()
                .target(targetLatLng)
                .zoom(zoom)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 300)
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    mapInstance = map

                    // 配置手势交互与内置 UI 控件状态
                    map.uiSettings.apply {
                        setAllGesturesEnabled(isInteractive)
                        isLogoEnabled = false
                        isAttributionEnabled = false
                        isCompassEnabled = false
                        isRotateGesturesEnabled = isInteractive
                        isTiltGesturesEnabled = isInteractive
                        isZoomGesturesEnabled = isInteractive
                        isScrollGesturesEnabled = isInteractive
                    }

                    // 设置初始相机位置
                    map.cameraPosition = CameraPosition.Builder()
                        .target(targetLatLng)
                        .zoom(zoom)
                        .build()

                    // 加载初始底图样式
                    val initialStyleJson = MapLibreHelper.buildStyleJson(mapLayerType)
                    map.setStyle(Style.Builder().fromJson(initialStyleJson)) { style ->
                        currentStyleInstance = style
                        onMapReady(map, style)
                    }
                }
            }
        },
        modifier = modifier
    )
}
